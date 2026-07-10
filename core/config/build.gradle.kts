import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "com.androdevlinux.satschat.core.config"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest { }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// `-Penv=regtest|signet|mainnet` (default regtest) selects which env is baked into BuildKonfig.
// One env per Gradle invocation; verifyEnvMatrix asserts the :androidApp flavor matches ENV_NAME.
val env: String = (providers.gradleProperty("env").orNull ?: "regtest").also {
    require(it in listOf("regtest", "signet", "mainnet")) { "Unknown -Penv=$it" }
}

// One field table per env. signet/mainnet endpoints are placeholders until the infra exists —
// never commit real mainnet endpoints anywhere but here (scripts/no-hardcoded-endpoints.sh guards
// the rest of the tree). 127.0.0.1 is rewritten to localhostAlias (10.0.2.2 on the emulator) by AppConfig.
data class EnvFields(
    val chain: String,
    val electrumHost: String,
    val electrumPort: Int,
    val electrumTls: Boolean,
    val lspNodeId: String,
    val lspUri: String,
    val smpServers: String,
    val notificationServerUrl: String,
    val deeplinkScheme: String,
)

val fields = when (env) {
    "signet" -> EnvFields(
        chain = "signet",
        electrumHost = "TODO.signet.electrum.invalid",
        electrumPort = 50002,
        electrumTls = true,
        lspNodeId = "",
        lspUri = "",
        smpServers = "",
        notificationServerUrl = "",
        deeplinkScheme = "satschat-signet",
    )
    "mainnet" -> EnvFields(
        chain = "mainnet",
        electrumHost = "TODO.mainnet.electrum.invalid",
        electrumPort = 50002,
        electrumTls = true,
        lspNodeId = "",
        lspUri = "",
        smpServers = "",
        notificationServerUrl = "",
        deeplinkScheme = "satschat",
    )
    else -> EnvFields(
        chain = "regtest",
        electrumHost = "127.0.0.1",
        electrumPort = 50001,
        electrumTls = false,
        lspNodeId = "",
        lspUri = "127.0.0.1:9735",
        smpServers = "smp://127.0.0.1:5223",
        notificationServerUrl = "",
        deeplinkScheme = "satschat-rt",
    )
}

buildkonfig {
    packageName = "com.androdevlinux.satschat.core.config"
    defaultConfigs {
        buildConfigField(Type.STRING, "ENV_NAME", env)
        buildConfigField(Type.STRING, "CHAIN", fields.chain)
        buildConfigField(Type.STRING, "ELECTRUM_HOST", fields.electrumHost)
        buildConfigField(Type.INT, "ELECTRUM_PORT", fields.electrumPort.toString())
        buildConfigField(Type.BOOLEAN, "ELECTRUM_TLS", fields.electrumTls.toString())
        buildConfigField(Type.STRING, "LSP_NODE_ID", fields.lspNodeId)
        buildConfigField(Type.STRING, "LSP_URI", fields.lspUri)
        buildConfigField(Type.STRING, "SMP_SERVERS", fields.smpServers)
        buildConfigField(Type.STRING, "NOTIFICATION_SERVER_URL", fields.notificationServerUrl)
        buildConfigField(Type.STRING, "DEEPLINK_SCHEME", fields.deeplinkScheme)
    }
}
