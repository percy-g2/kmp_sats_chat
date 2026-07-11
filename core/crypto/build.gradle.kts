import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    // libsodium cinterop for iOS. Vendored static slices live under src/nativeInterop/libsodium
    // (gitignored; fetch with tooling/fetch-libsodium-ios.sh). Device and simulator link DIFFERENT
    // libsodium.a — wired per target to avoid the classic slice-mismatch link error.
    val libsodiumDir = layout.projectDirectory.dir("src/nativeInterop/libsodium")
    listOf(
        iosArm64() to "ios-arm64_arm64e",
        iosSimulatorArm64() to "ios-arm64_arm64e_x86_64-simulator",
    ).forEach { (target, slice) ->
        val headerDir = libsodiumDir.dir("$slice/Headers/Clibsodium")
        val libDir = libsodiumDir.dir(slice)
        target.compilations.getByName("main").cinterops.create("sodium") {
            definitionFile.set(layout.projectDirectory.file("src/nativeInterop/cinterop/sodium.def"))
            compilerOpts("-I${headerDir.asFile.absolutePath}")
        }
        target.binaries.all {
            linkerOpts("-L${libDir.asFile.absolutePath}", "-lsodium")
        }
    }

    androidLibrary {
        namespace = "com.androdevlinux.satschat.core.crypto"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest { }
        // Crypto KATs run as INSTRUMENTED tests on a device/emulator: lazysodium loads native
        // libsodium.so via JNI, which a plain JVM host test cannot do. Task: :core:crypto:connectedAndroidTest
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            // @aar is MANDATORY: these AARs ship the Android .so (libsodium + libjnidispatch).
            implementation("${libs.lazysodium.android.get()}@aar")
            implementation("${libs.jna.get()}@aar")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.testExt.junit)
        }
    }
}
