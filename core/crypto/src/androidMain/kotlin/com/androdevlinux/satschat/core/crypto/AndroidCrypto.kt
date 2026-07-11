package com.androdevlinux.satschat.core.crypto

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.interfaces.AEAD
import com.goterl.lazysodium.interfaces.Auth
import com.goterl.lazysodium.interfaces.Sign

/**
 * Android [Crypto] backed by libsodium via lazysodium (JNI). Uses the raw [SodiumAndroid] Native
 * calls where byte-exact control matters (scalarmult, streaming HMAC), and the LazySodium wrappers
 * for AEAD/Sign/random. Runs on a device/emulator only (native .so).
 * TODO(security-review): key material is handled as plain ByteArray; zeroization/Keystore wrapping TBD.
 */
private class AndroidCrypto : Crypto {
    private val sodium = SodiumAndroid()
    private val ls = LazySodiumAndroid(sodium)

    override fun randomBytes(size: Int): ByteArray = ls.randomBytesBuf(size)

    override fun x25519ScalarMult(scalar: ByteArray, point: ByteArray): ByteArray {
        require(scalar.size == 32 && point.size == 32) { "X25519 inputs must be 32 bytes" }
        val q = ByteArray(32)
        check(sodium.crypto_scalarmult(q, scalar, point) == 0) { "crypto_scalarmult failed (low-order point?)" }
        return q
    }

    override fun x25519PublicKey(secret: ByteArray): ByteArray {
        require(secret.size == 32) { "X25519 secret must be 32 bytes" }
        val pk = ByteArray(32)
        check(sodium.crypto_scalarmult_base(pk, secret) == 0) { "crypto_scalarmult_base failed" }
        return pk
    }

    override fun aeadEncrypt(message: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray {
        require(key.size == AEAD.XCHACHA20POLY1305_IETF_KEYBYTES) { "AEAD key must be 32 bytes" }
        require(nonce.size == AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES) { "XChaCha nonce must be 24 bytes" }
        val cipher = ByteArray(message.size + AEAD.XCHACHA20POLY1305_IETF_ABYTES)
        val cipherLen = LongArray(1)
        check(
            ls.cryptoAeadXChaCha20Poly1305IetfEncrypt(
                cipher, cipherLen, message, message.size.toLong(),
                associatedData, associatedData.size.toLong(), null, nonce, key,
            ),
        ) { "AEAD encrypt failed" }
        return cipher.copyOf(cipherLen[0].toInt())
    }

    override fun aeadDecrypt(ciphertext: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray? {
        require(key.size == AEAD.XCHACHA20POLY1305_IETF_KEYBYTES) { "AEAD key must be 32 bytes" }
        require(nonce.size == AEAD.XCHACHA20POLY1305_IETF_NPUBBYTES) { "XChaCha nonce must be 24 bytes" }
        if (ciphertext.size < AEAD.XCHACHA20POLY1305_IETF_ABYTES) return null
        val out = ByteArray(ciphertext.size - AEAD.XCHACHA20POLY1305_IETF_ABYTES)
        val outLen = LongArray(1)
        val ok = ls.cryptoAeadXChaCha20Poly1305IetfDecrypt(
            out, outLen, null, ciphertext, ciphertext.size.toLong(),
            associatedData, associatedData.size.toLong(), nonce, key,
        )
        return if (ok) out.copyOf(outLen[0].toInt()) else null
    }

    override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        // Streaming form accepts arbitrary key length (so it serves HKDF-Extract's salt key too).
        val out = ByteArray(Auth.HMACSHA256_BYTES)
        val state = Auth.StateHMAC256()
        check(sodium.crypto_auth_hmacsha256_init(state, key, key.size) == 0) { "hmac init failed" }
        check(sodium.crypto_auth_hmacsha256_update(state, message, message.size.toLong()) == 0) { "hmac update failed" }
        check(sodium.crypto_auth_hmacsha256_final(state, out) == 0) { "hmac final failed" }
        return out
    }

    override fun ed25519Keypair(seed: ByteArray?): Pair<ByteArray, ByteArray> {
        val pk = ByteArray(Sign.PUBLICKEYBYTES)
        val sk = ByteArray(Sign.SECRETKEYBYTES)
        if (seed == null) {
            check(ls.cryptoSignKeypair(pk, sk)) { "ed25519 keypair failed" }
        } else {
            require(seed.size == Sign.SEEDBYTES) { "Ed25519 seed must be 32 bytes" }
            check(ls.cryptoSignSeedKeypair(pk, sk, seed)) { "ed25519 seed keypair failed" }
        }
        return pk to sk
    }

    override fun ed25519Sign(message: ByteArray, secretKey: ByteArray): ByteArray {
        val sig = ByteArray(Sign.BYTES)
        check(ls.cryptoSignDetached(sig, message, message.size.toLong(), secretKey)) { "ed25519 sign failed" }
        return sig
    }

    override fun ed25519Verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean =
        ls.cryptoSignVerifyDetached(signature, message, message.size, publicKey)
}

actual fun platformCrypto(): Crypto = AndroidCrypto()
