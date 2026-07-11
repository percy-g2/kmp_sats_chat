package com.androdevlinux.satschat.core.crypto

/**
 * The primitives layer over a vetted libsodium (lazysodium on JVM/Android, a Clibsodium cinterop on
 * iOS). NOTHING here is hand-rolled except the HKDF construction, which is a standard RFC 5869
 * assembly over the platform's native HMAC-SHA256. The Signal double ratchet is built on top of this
 * in a later step, with its own KATs.
 *
 * TODO(security-review): the whole surface is security-critical; changes require updated KATs in the
 * same PR (see :core:crypto androidDeviceTest / iosTest).
 */
interface Crypto {
    fun randomBytes(size: Int): ByteArray

    /** Raw X25519 crypto_scalarmult(scalar, point). Throws on a low-order (all-zero) result. */
    fun x25519ScalarMult(scalar: ByteArray, point: ByteArray): ByteArray

    /** X25519 public key from a 32-byte secret (crypto_scalarmult_base). */
    fun x25519PublicKey(secret: ByteArray): ByteArray

    /** XChaCha20-Poly1305-IETF AEAD. Returns ciphertext||tag (message.size + 16 bytes). Nonce is 24 bytes. */
    fun aeadEncrypt(message: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray

    /** XChaCha20-Poly1305-IETF AEAD open. Returns null on authentication failure (never the plaintext). */
    fun aeadDecrypt(ciphertext: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray?

    /** HMAC-SHA256 with an arbitrary-length key (streaming, so it also serves HKDF-Extract). */
    fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray

    /** Ed25519 keypair (pk, sk). Deterministic from a 32-byte [seed] when given. */
    fun ed25519Keypair(seed: ByteArray? = null): Pair<ByteArray, ByteArray>

    fun ed25519Sign(message: ByteArray, secretKey: ByteArray): ByteArray

    fun ed25519Verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean

    /**
     * RFC 5869 HKDF-SHA256, built on [hmacSha256] so both platforms share identical KDF logic.
     * `salt == null` uses HashLen (32) zero bytes, per the RFC.
     */
    fun hkdfSha256(ikm: ByteArray, salt: ByteArray?, info: ByteArray, length: Int): ByteArray {
        require(length in 1..(255 * 32)) { "HKDF-SHA256 length must be 1..8160, was $length" }
        val prk = hmacSha256(salt ?: ByteArray(32), ikm) // Extract
        val out = ByteArray(length)
        var previous = ByteArray(0)
        var pos = 0
        var counter = 1
        while (pos < length) { // Expand
            previous = hmacSha256(prk, previous + info + byteArrayOf(counter.toByte()))
            val chunk = minOf(32, length - pos)
            previous.copyInto(out, pos, 0, chunk)
            pos += chunk
            counter++
        }
        return out
    }
}

/** The platform-backed [Crypto] implementation (lazysodium on Android; libsodium cinterop on iOS). */
expect fun platformCrypto(): Crypto
