package com.androdevlinux.satschat.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates the platform [SqlDriver] for the SatsChat database and opens a typed [SatsChatDatabase].
 *
 * TODO(security-review): at-rest encryption. Android opens the DB through SQLCipher
 * (SupportOpenHelperFactory) keyed by [passphrase]. iOS currently opens an UNENCRYPTED DB — SQLCipher
 * on iOS (CocoaPods `pod("SQLCipher")` + `linkSqlite = false` + PRAGMA key) is pending, so on iOS the
 * passphrase is not yet applied. Do not ship the iOS build to users until this is closed.
 */
expect class DriverFactory {
    fun createDriver(databaseName: String, passphrase: String): SqlDriver
}

fun createDatabase(factory: DriverFactory, databaseName: String, passphrase: String): SatsChatDatabase =
    SatsChatDatabase(factory.createDriver(databaseName, passphrase))
