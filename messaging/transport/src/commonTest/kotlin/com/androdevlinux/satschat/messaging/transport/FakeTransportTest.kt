package com.androdevlinux.satschat.messaging.transport

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeTransportTest {
    @Test
    fun echoesMessagesBetweenPairedEndpoints() = runTest {
        val (alice, bob) = FakeTransport.pair()

        val ping = byteArrayOf(1, 2, 3, 4, 5)
        alice.send(ping)
        assertTrue(ping.contentEquals(bob.receive()), "bob should receive alice's bytes verbatim")

        val pong = "pong".encodeToByteArray()
        bob.send(pong)
        assertEquals("pong", alice.receive().decodeToString(), "alice should receive bob's reply")
    }
}
