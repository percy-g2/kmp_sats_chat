package com.androdevlinux.satschat.core.database

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * DB open/read/write round-trip on the iOS simulator (native driver). Android encrypted-DB coverage
 * needs a device/Robolectric instrumented test (follow-up); the Android driver is compile-verified.
 */
class DatabaseTest {
    @Test
    fun insertsAndReadsBackRows() {
        val db = createDatabase(DriverFactory(), "satschat-test-${Random.nextLong()}.db", "unused-on-ios")
        val connections = db.connectionQueries

        assertEquals(0, connections.selectAllConnections().executeAsList().size)

        connections.insertConnection("conn-1", "Alice", 1_000L)
        connections.insertConnection("conn-2", null, 2_000L)

        val rows = connections.selectAllConnections().executeAsList()
        assertEquals(2, rows.size)
        assertEquals("Alice", rows.first().contactName)
        assertEquals("conn-2", connections.selectConnectionById("conn-2").executeAsOne().id)
        assertNull(connections.selectConnectionById("missing").executeAsOneOrNull())
    }
}
