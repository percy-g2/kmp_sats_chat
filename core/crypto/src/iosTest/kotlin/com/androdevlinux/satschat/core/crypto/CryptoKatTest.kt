package com.androdevlinux.satschat.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Same Known-Answer Tests as Android (:core:crypto androidDeviceTest), run on the iOS simulator
 * against the libsodium cinterop. Per-platform crypto gate: JVM-green != iOS-green.
 */
class CryptoKatTest {
    private val crypto = platformCrypto()

    @Test
    fun x25519_rfc7748_section_5_2() {
        val out = crypto.x25519ScalarMult(
            scalar = hexToBytes("a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4"),
            point = hexToBytes("e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c"),
        )
        assertEquals("c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552", out.toHex())
    }

    @Test
    fun x25519_dh_agreement() {
        val aSk = crypto.randomBytes(32)
        val bSk = crypto.randomBytes(32)
        val aPk = crypto.x25519PublicKey(aSk)
        val bPk = crypto.x25519PublicKey(bSk)
        assertEquals(crypto.x25519ScalarMult(aSk, bPk).toHex(), crypto.x25519ScalarMult(bSk, aPk).toHex())
    }

    @Test
    fun hkdf_sha256_rfc5869_case1() {
        val okm = crypto.hkdfSha256(
            ikm = hexToBytes("0b".repeat(22)),
            salt = hexToBytes("000102030405060708090a0b0c"),
            info = hexToBytes("f0f1f2f3f4f5f6f7f8f9"),
            length = 42,
        )
        assertEquals(
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
            okm.toHex(),
        )
    }

    @Test
    fun aead_xchacha20poly1305_ietf_draft_vector() {
        val key = hexToBytes("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f")
        val nonce = hexToBytes("404142434445464748494a4b4c4d4e4f5051525354555657")
        val aad = hexToBytes("50515253c0c1c2c3c4c5c6c7")
        val plaintext = hexToBytes(
            "4c616469657320616e642047656e746c656d656e206f662074686520636c6173" +
                "73206f66202739393a204966204920636f756c64206f6666657220796f75206f" +
                "6e6c79206f6e652074697020666f7220746865206675747572652c2073756e73" +
                "637265656e20776f756c642062652069742e",
        )
        val expected = "bd6d179d3e83d43b9576579493c0e939572a1700252bfaccbed2902c21396cbb" +
            "731c7f1b0b4aa6440bf3a82f4eda7e39ae64c6708c54c216cb96b72e1213b452" +
            "2f8c9ba40db5d945b11b69b982c1bb9e3f3fac2bc369488f76b2383565d3fff9" +
            "21f9664c97637da9768812f615c68b13b52e" + "c0875924c1c7987947deafd8780acf49"

        val sealed = crypto.aeadEncrypt(plaintext, nonce, key, aad)
        assertEquals(expected, sealed.toHex())
        val opened = crypto.aeadDecrypt(sealed, nonce, key, aad)
        assertTrue(opened != null && opened.toHex() == plaintext.toHex())
        val tampered = sealed.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertNull(crypto.aeadDecrypt(tampered, nonce, key, aad))
    }

    @Test
    fun ed25519_rfc8032_test1() {
        val seed = hexToBytes("9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60")
        val (pk, sk) = crypto.ed25519Keypair(seed)
        assertEquals("d75a980182b10ab7d54bfed3c964073a0ee172f3daa62325af021a68f707511a", pk.toHex())
        val sig = crypto.ed25519Sign(ByteArray(0), sk)
        assertEquals(
            "e5564300c360ac729086e2cc806e828a84877f1eb8e5d974d873e065224901555fb8821590a33bacc61e39701cf9b46bd25bf5f0595bbe24655141438e7a100b",
            sig.toHex(),
        )
        assertTrue(crypto.ed25519Verify(ByteArray(0), sig, pk))
    }
}
