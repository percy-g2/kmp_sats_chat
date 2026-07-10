import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// Config-cache-friendly env reads. `-Penv` (default regtest) is the single source of truth.
val isCi = providers.environmentVariable("CI").isPresent
val buildEnv = providers.gradleProperty("env").orNull ?: "regtest"

android {
    namespace = "com.androdevlinux.satschat"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.androdevlinux.satschat"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // Overridden per env flavor below; defaults keep non-flavored tooling happy.
        manifestPlaceholders["appLabel"] = "SatsChat"
        manifestPlaceholders["deeplinkScheme"] = "satschat"
    }

    // One env flavor per network. Suffixed applicationIds + distinct deeplink schemes let all
    // three installs coexist on one device without colliding. Pass -Penv=<flavor> so :core:config
    // bakes the matching BuildKonfig; verifyEnvMatrix asserts the two agree.
    flavorDimensions += "env"
    productFlavors {
        create("regtest") {
            dimension = "env"
            applicationIdSuffix = ".rt"
            versionNameSuffix = "-rt"
            manifestPlaceholders["appLabel"] = "SatsChat RT"
            manifestPlaceholders["deeplinkScheme"] = "satschat-rt"
        }
        create("signet") {
            dimension = "env"
            applicationIdSuffix = ".signet"
            versionNameSuffix = "-signet"
            manifestPlaceholders["appLabel"] = "SatsChat Signet"
            manifestPlaceholders["deeplinkScheme"] = "satschat-signet"
        }
        create("mainnet") {
            dimension = "env"
            // no applicationId suffix — this is the production id.
            manifestPlaceholders["appLabel"] = "SatsChat"
            manifestPlaceholders["deeplinkScheme"] = "satschat"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            // R8 + shrinkResources + signing are turned on in Phase 6 (release hardening); a real
            // keystore is required and must never be committed. Release stays unsigned for now.
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Env matrix enforcement (config-cache safe):
//  1. Lock androidApp to the single flavor that matches -Penv. Since :core:config bakes BuildKonfig
//     from the same -Penv, the app flavor and the compiled config can never desync — exactly one
//     env is buildable per Gradle invocation. To build another env, pass its -Penv.
//  2. Never produce a debuggable mainnet variant outside CI.
androidComponents {
    beforeVariants { variant ->
        val flavor = variant.productFlavors.firstOrNull { it.first == "env" }?.second
        if (flavor != null && flavor != buildEnv) {
            variant.enable = false
        }
        if (!isCi && flavor == "mainnet" && variant.buildType == "debug") {
            variant.enable = false
        }
    }
}