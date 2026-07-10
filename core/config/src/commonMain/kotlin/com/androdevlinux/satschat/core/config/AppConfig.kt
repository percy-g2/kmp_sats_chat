package com.androdevlinux.satschat.core.config

/**
 * The single, env-aware config surface for the whole app. Values come from the generated
 * [BuildKonfig] object (populated by the active `-Penv` flavor); no other code reads env values
 * from anywhere else. Regtest loopback hosts are rewritten through [localhostAlias] so the Android
 * emulator (10.0.2.2) and the iOS simulator (127.0.0.1) both reach the host machine.
 */
object AppConfig {
    val envName: String get() = BuildKonfig.ENV_NAME
    val chain: String get() = BuildKonfig.CHAIN

    val electrumHost: String get() = rewriteLoopback(BuildKonfig.ELECTRUM_HOST)
    val electrumPort: Int get() = BuildKonfig.ELECTRUM_PORT
    val electrumTls: Boolean get() = BuildKonfig.ELECTRUM_TLS

    val lspNodeId: String get() = BuildKonfig.LSP_NODE_ID
    val lspUri: String get() = rewriteLoopback(BuildKonfig.LSP_URI)

    /** Comma-separated relay URIs; empty entries filtered out. */
    val smpServers: List<String>
        get() = BuildKonfig.SMP_SERVERS.split(",")
            .map { rewriteLoopback(it.trim()) }
            .filter { it.isNotEmpty() }

    val notificationServerUrl: String get() = BuildKonfig.NOTIFICATION_SERVER_URL
    val deeplinkScheme: String get() = BuildKonfig.DEEPLINK_SCHEME

    private fun rewriteLoopback(value: String): String {
        if (envName != "regtest") return value
        return value.replace("127.0.0.1", localhostAlias).replace("localhost", localhostAlias)
    }

    /**
     * iOS launch guard: throws if the framework's baked env disagrees with the Xcode scheme's
     * SATSCHAT_ENV (read from Info.plist and passed in from iOSApp.swift). This catches the case
     * where the embedAndSign phase didn't pass a matching `-Penv`. On Android the flavor lock in
     * :androidApp already makes this impossible, so it's a no-op there. An empty [expected]
     * (SATSCHAT_ENV not wired yet) is skipped rather than failing.
     */
    fun assertEnvMatches(expected: String) {
        check(expected.isEmpty() || expected == envName) {
            "SATSCHAT_ENV mismatch: framework built for '$envName' but the scheme says '$expected'. " +
                "The Xcode 'Compile Kotlin Framework' phase must pass -Penv=\$SATSCHAT_ENV."
        }
    }
}
