rootProject.name = "SatsChat"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// --- Environment matrix (single source of truth) ---
// `-Penv=regtest|signet|mainnet` (default regtest) selects which env :core:config bakes into
// BuildKonfig. Exactly ONE env is built per invocation. verifyEnvMatrix (root build) asserts the
// active :androidApp flavor matches this env. (Validation only here — projectProperties is
// immutable in Gradle 9; the value is consumed directly via providers.gradleProperty("env").)
run {
    val allowed = listOf("regtest", "signet", "mainnet")
    val env = startParameter.projectProperties["env"] ?: "regtest"
    require(env in allowed) { "Unknown -Penv=$env (expected one of $allowed)" }
}

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
include(":core:model")
include(":core:crypto")
include(":core:database")
include(":core:config")
include(":messaging:transport")
include(":messaging:smp")
include(":lightning")
include(":feature:chat")
include(":feature:wallet")
include(":feature:payinchat")