package com.androdevlinux.satschat.core.config

/**
 * Host alias that reaches the developer machine's loopback from a device/emulator/simulator.
 * Only meaningful for the regtest env. Android emulator: 10.0.2.2. iOS simulator: 127.0.0.1.
 */
expect val localhostAlias: String
