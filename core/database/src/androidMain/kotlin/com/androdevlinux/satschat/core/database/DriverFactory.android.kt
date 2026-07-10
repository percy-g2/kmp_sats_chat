package com.androdevlinux.satschat.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Android driver backed by SQLCipher. [context] must be an application context (never leaked into
 * commonMain). TODO(security-review): key derivation/storage for [passphrase] (Keystore-wrapped).
 */
actual class DriverFactory(private val context: Context) {
    actual fun createDriver(databaseName: String, passphrase: String): SqlDriver {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(passphrase.encodeToByteArray())
        return AndroidSqliteDriver(
            schema = SatsChatDatabase.Schema,
            context = context,
            name = databaseName,
            factory = factory,
        )
    }
}
