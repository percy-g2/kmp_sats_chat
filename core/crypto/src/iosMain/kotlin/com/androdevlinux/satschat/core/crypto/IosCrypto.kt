@file:OptIn(ExperimentalForeignApi::class)

package com.androdevlinux.satschat.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import sodium.crypto_aead_xchacha20poly1305_ietf_decrypt
import sodium.crypto_aead_xchacha20poly1305_ietf_encrypt
import sodium.crypto_auth_hmacsha256_final
import sodium.crypto_auth_hmacsha256_init
import sodium.crypto_auth_hmacsha256_state
import sodium.crypto_auth_hmacsha256_update
import sodium.crypto_scalarmult
import sodium.crypto_scalarmult_base
import sodium.crypto_sign_detached
import sodium.crypto_sign_keypair
import sodium.crypto_sign_seed_keypair
import sodium.crypto_sign_verify_detached
import sodium.randombytes_buf
import sodium.sodium_init

/** Pin this ByteArray and expose its start as an `unsigned char*`. Empty arrays get a 1-byte scratch. */
private inline fun <R> ByteArray.usePtr(block: (kotlinx.cinterop.CPointer<UByteVar>) -> R): R =
    if (isEmpty()) {
        ByteArray(1).usePinned { block(it.addressOf(0).reinterpret()) }
    } else {
        usePinned { block(it.addressOf(0).reinterpret()) }
    }

/**
 * iOS [Crypto] backed by libsodium via cinterop (see src/nativeInterop). Uses the same vetted
 * primitives as Android; byte layouts match so the KATs are identical.
 * TODO(security-review): key material handled as plain ByteArray; zeroization TBD.
 */
private class IosLibsodiumCrypto : Crypto {
    init {
        check(sodium_init() >= 0) { "sodium_init failed" }
    }

    override fun randomBytes(size: Int): ByteArray {
        val out = ByteArray(size)
        if (size > 0) out.usePtr { randombytes_buf(it, size.convert()) }
        return out
    }

    override fun x25519ScalarMult(scalar: ByteArray, point: ByteArray): ByteArray {
        require(scalar.size == 32 && point.size == 32) { "X25519 inputs must be 32 bytes" }
        val q = ByteArray(32)
        val rc = q.usePtr { qp -> scalar.usePtr { np -> point.usePtr { pp -> crypto_scalarmult(qp, np, pp) } } }
        check(rc == 0) { "crypto_scalarmult failed (low-order point?)" }
        return q
    }

    override fun x25519PublicKey(secret: ByteArray): ByteArray {
        require(secret.size == 32) { "X25519 secret must be 32 bytes" }
        val pk = ByteArray(32)
        check(pk.usePtr { p -> secret.usePtr { s -> crypto_scalarmult_base(p, s) } } == 0) { "scalarmult_base failed" }
        return pk
    }

    override fun aeadEncrypt(message: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray {
        require(key.size == 32 && nonce.size == 24) { "bad AEAD key/nonce size" }
        val out = ByteArray(message.size + 16)
        val rc = out.usePtr { c ->
            message.usePtr { m ->
                associatedData.usePtr { a ->
                    nonce.usePtr { np ->
                        key.usePtr { k ->
                            crypto_aead_xchacha20poly1305_ietf_encrypt(
                                c, null, m, message.size.convert(), a, associatedData.size.convert(), null, np, k,
                            )
                        }
                    }
                }
            }
        }
        check(rc == 0) { "AEAD encrypt failed" }
        return out
    }

    override fun aeadDecrypt(ciphertext: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray? {
        require(key.size == 32 && nonce.size == 24) { "bad AEAD key/nonce size" }
        if (ciphertext.size < 16) return null
        val out = ByteArray(ciphertext.size - 16)
        val rc = out.usePtr { m ->
            ciphertext.usePtr { c ->
                associatedData.usePtr { a ->
                    nonce.usePtr { np ->
                        key.usePtr { k ->
                            crypto_aead_xchacha20poly1305_ietf_decrypt(
                                m, null, null, c, ciphertext.size.convert(), a, associatedData.size.convert(), np, k,
                            )
                        }
                    }
                }
            }
        }
        return if (rc == 0) out else null
    }

    override fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val out = ByteArray(32)
        memScoped {
            val state = alloc<crypto_auth_hmacsha256_state>()
            check(key.usePtr { k -> crypto_auth_hmacsha256_init(state.ptr, k, key.size.convert()) } == 0) { "hmac init" }
            check(message.usePtr { m -> crypto_auth_hmacsha256_update(state.ptr, m, message.size.convert()) } == 0) { "hmac update" }
            check(out.usePtr { o -> crypto_auth_hmacsha256_final(state.ptr, o) } == 0) { "hmac final" }
        }
        return out
    }

    override fun ed25519Keypair(seed: ByteArray?): Pair<ByteArray, ByteArray> {
        val pk = ByteArray(32)
        val sk = ByteArray(64)
        val rc = if (seed == null) {
            pk.usePtr { p -> sk.usePtr { s -> crypto_sign_keypair(p, s) } }
        } else {
            require(seed.size == 32) { "Ed25519 seed must be 32 bytes" }
            pk.usePtr { p -> sk.usePtr { s -> seed.usePtr { sd -> crypto_sign_seed_keypair(p, s, sd) } } }
        }
        check(rc == 0) { "ed25519 keypair failed" }
        return pk to sk
    }

    override fun ed25519Sign(message: ByteArray, secretKey: ByteArray): ByteArray {
        val sig = ByteArray(64)
        val rc = sig.usePtr { sg -> message.usePtr { m -> secretKey.usePtr { sk -> crypto_sign_detached(sg, null, m, message.size.convert(), sk) } } }
        check(rc == 0) { "ed25519 sign failed" }
        return sig
    }

    override fun ed25519Verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        val rc = signature.usePtr { sg -> message.usePtr { m -> publicKey.usePtr { pk -> crypto_sign_verify_detached(sg, m, message.size.convert(), pk) } } }
        return rc == 0
    }
}

actual fun platformCrypto(): Crypto = IosLibsodiumCrypto()
