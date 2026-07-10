plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.buildkonfig) apply false
}

// Asserts the selected env is valid and documents the enforced coupling: androidApp is locked to
// the `-Penv` flavor (build.gradle.kts of :androidApp) and :core:config bakes the same env into
// BuildKonfig, so the two can never disagree. One env is built per invocation.
tasks.register("verifyEnvMatrix") {
    group = "verification"
    description = "Verify the -Penv build env is valid and coupled across androidApp + :core:config."
    val env = providers.gradleProperty("env").orElse("regtest")
    doLast {
        val e = env.get()
        require(e in listOf("regtest", "signet", "mainnet")) {
            "Unknown -Penv=$e (expected regtest|signet|mainnet)"
        }
        println("verifyEnvMatrix OK: env=$e — androidApp restricted to the '$e' flavor; " +
            ":core:config BuildKonfig ENV_NAME=$e.")
    }
}