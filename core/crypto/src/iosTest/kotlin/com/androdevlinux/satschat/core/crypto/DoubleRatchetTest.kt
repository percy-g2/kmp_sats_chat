package com.androdevlinux.satschat.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Double Ratchet interop tests on the iOS simulator (real libsodium cinterop). Mirrors androidDeviceTest. */
class DoubleRatchetTest {
    private val crypto = platformCrypto()
    private val ad = "satschat-ad".encodeToByteArray()

    private fun newSession(): Pair<DoubleRatchet, DoubleRatchet> {
        val sharedKey = crypto.randomBytes(32)
        val bob = DoubleRatchet.generateDh(crypto)
        return DoubleRatchet.initAlice(crypto, sharedKey, bob.public) to DoubleRatchet.initBob(crypto, sharedKey, bob)
    }

    @Test
    fun basicRoundTrip() {
        val (alice, bob) = newSession()
        assertEquals("hi bob", bob.decrypt(alice.encrypt("hi bob".encodeToByteArray(), ad), ad).decodeToString())
    }

    @Test
    fun backAndForthAcrossDhRatchet() {
        val (alice, bob) = newSession()
        repeat(4) { i ->
            assertEquals("a$i", bob.decrypt(alice.encrypt("a$i".encodeToByteArray(), ad), ad).decodeToString())
            assertEquals("b$i", alice.decrypt(bob.encrypt("b$i".encodeToByteArray(), ad), ad).decodeToString())
        }
    }

    @Test
    fun outOfOrderWithinChain() {
        val (alice, bob) = newSession()
        val c = (0 until 4).map { alice.encrypt("m$it".encodeToByteArray(), ad) }
        assertEquals("m3", bob.decrypt(c[3], ad).decodeToString())
        assertEquals("m1", bob.decrypt(c[1], ad).decodeToString())
        assertEquals("m0", bob.decrypt(c[0], ad).decodeToString())
        assertEquals("m2", bob.decrypt(c[2], ad).decodeToString())
    }

    @Test
    fun skippedMessageAcrossDhRatchet() {
        val (alice, bob) = newSession()
        val a0 = alice.encrypt("a0".encodeToByteArray(), ad)
        val a1 = alice.encrypt("a1".encodeToByteArray(), ad)
        assertEquals("a0", bob.decrypt(a0, ad).decodeToString())
        assertEquals("b0", alice.decrypt(bob.encrypt("b0".encodeToByteArray(), ad), ad).decodeToString())
        val a2 = alice.encrypt("a2".encodeToByteArray(), ad)
        assertEquals("a2", bob.decrypt(a2, ad).decodeToString())
        assertEquals("a1", bob.decrypt(a1, ad).decodeToString())
    }

    @Test
    fun tamperRejected() {
        val (alice, bob) = newSession()
        val ct = alice.encrypt("secret".encodeToByteArray(), ad).copyOf()
        ct[ct.size - 1] = (ct[ct.size - 1].toInt() xor 1).toByte()
        assertFailsWith<Throwable> { bob.decrypt(ct, ad) }
    }
}
