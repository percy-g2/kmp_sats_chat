package com.androdevlinux.satschat.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS driver. TODO(security-review): the DB is currently UNENCRYPTED — SQLCipher on iOS
 * (CocoaPods `pod("SQLCipher")` + `linkSqlite = false` + PRAGMA key) is pending, so [passphrase] is
 * intentionally ignored for now. Do not ship to users until this is closed.
 */
actual class DriverFactory {
    actual fun createDriver(databaseName: String, passphrase: String): SqlDriver =
        NativeSqliteDriver(SatsChatDatabase.Schema, databaseName)
}
