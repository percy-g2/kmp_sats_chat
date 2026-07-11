package com.androdevlinux.satschat.core.crypto

/**
 * The Signal Double Ratchet (signal.org/docs/specifications/doubleratchet), ported faithfully onto
 * the KAT-verified [Crypto] primitives:
 *   DH        = X25519            (crypto.x25519* )
 *   KDF_RK    = HKDF-SHA256       (root chain: salt=RK, ikm=DH output)
 *   KDF_CK    = HMAC-SHA256       (symmetric chain: mk=HMAC(ck,0x01), ck'=HMAC(ck,0x02))
 *   AEAD      = XChaCha20-Poly1305 with a per-message key+nonce derived from mk via HKDF
 * The header (sender DH pubkey, previous-chain length, message number) is authenticated as AEAD AD.
 *
 * TODO(security-review): this is a spec-faithful port verified by interop/out-of-order/tamper tests
 * (see androidDeviceTest / iosTest). Before production, ALSO cross-test against a reference impl
 * (libsignal) vectors, and add key zeroization. Any change here ships updated tests in the same PR.
 */
class DhKeyPair(val public: ByteArray, val secret: ByteArray)

/** Wire header: 32-byte DH public key || 4-byte big-endian PN || 4-byte big-endian N. */
class RatchetHeader(val dh: ByteArray, val pn: Int, val n: Int) {
    fun encode(): ByteArray = dh + pn.beBytes() + n.beBytes()

    companion object {
        const val SIZE = 40

        fun decode(bytes: ByteArray): RatchetHeader {
            require(bytes.size >= SIZE) { "header too short" }
            return RatchetHeader(bytes.copyOfRange(0, 32), bytes.beInt(32), bytes.beInt(36))
        }
    }
}

private data class SkippedKeyId(val dhHex: String, val n: Int)

class DoubleRatchet private constructor(
    private val crypto: Crypto,
    private var dhs: DhKeyPair,
    private var dhr: ByteArray?,
    private var rk: ByteArray,
    private var cks: ByteArray?,
    private var ckr: ByteArray?,
    private var ns: Int,
    private var nr: Int,
    private var pn: Int,
    private val skipped: MutableMap<SkippedKeyId, ByteArray>,
) {
    /** Encrypt [plaintext]; returns header||ciphertext ready for the wire. */
    fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
        val (nextCks, mk) = kdfCk(cks ?: error("no sending chain"))
        cks = nextCks
        val header = RatchetHeader(dhs.public, pn, ns)
        ns += 1
        val ciphertext = aead(mk, plaintext, associatedData + header.encode(), seal = true)
            ?: error("AEAD seal failed")
        return header.encode() + ciphertext
    }

    /** Decrypt a header||ciphertext message. Throws on authentication failure or excessive skip. */
    fun decrypt(message: ByteArray, associatedData: ByteArray): ByteArray {
        val header = RatchetHeader.decode(message)
        val ciphertext = message.copyOfRange(RatchetHeader.SIZE, message.size)
        val headerBytes = header.encode()

        // 1) A previously-skipped message key?
        val skippedId = SkippedKeyId(header.dh.toHex(), header.n)
        skipped[skippedId]?.let { mk ->
            skipped.remove(skippedId)
            return aead(mk, ciphertext, associatedData + headerBytes, seal = false)
                ?: error("AEAD open failed (skipped key)")
        }

        // 2) New DH ratchet key -> step the DH ratchet (after skipping the tail of the old chain).
        if (dhr == null || !header.dh.contentEquals(dhr)) {
            skipMessageKeys(header.pn)
            dhRatchet(header)
        }

        // 3) Skip within the current receiving chain, then derive this message's key.
        skipMessageKeys(header.n)
        val (nextCkr, mk) = kdfCk(ckr ?: error("no receiving chain"))
        ckr = nextCkr
        nr += 1
        return aead(mk, ciphertext, associatedData + headerBytes, seal = false)
            ?: error("AEAD open failed")
    }

    private fun skipMessageKeys(until: Int) {
        if (nr + MAX_SKIP < until) error("too many skipped messages ($until)")
        var ck = ckr ?: return
        val dhrHex = dhr!!.toHex()
        while (nr < until) {
            val (nextCk, mk) = kdfCk(ck)
            ck = nextCk
            skipped[SkippedKeyId(dhrHex, nr)] = mk
            nr += 1
        }
        ckr = ck
    }

    private fun dhRatchet(header: RatchetHeader) {
        pn = ns
        ns = 0
        nr = 0
        dhr = header.dh
        kdfRk(rk, crypto.x25519ScalarMult(dhs.secret, dhr!!)).let { (newRk, newCkr) -> rk = newRk; ckr = newCkr }
        dhs = generateDh(crypto)
        kdfRk(rk, crypto.x25519ScalarMult(dhs.secret, dhr!!)).let { (newRk, newCks) -> rk = newRk; cks = newCks }
    }

    private fun kdfRk(rootKey: ByteArray, dhOut: ByteArray): Pair<ByteArray, ByteArray> {
        val out = crypto.hkdfSha256(ikm = dhOut, salt = rootKey, info = INFO_RK, length = 64)
        return out.copyOfRange(0, 32) to out.copyOfRange(32, 64)
    }

    private fun kdfCk(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        val messageKey = crypto.hmacSha256(chainKey, byteArrayOf(0x01))
        val nextChainKey = crypto.hmacSha256(chainKey, byteArrayOf(0x02))
        return nextChainKey to messageKey
    }

    /** Derive a 32-byte AEAD key + 24-byte nonce from the message key, then seal/open. */
    private fun aead(messageKey: ByteArray, data: ByteArray, ad: ByteArray, seal: Boolean): ByteArray? {
        val keyNonce = crypto.hkdfSha256(ikm = messageKey, salt = ByteArray(32), info = INFO_MK, length = 56)
        val key = keyNonce.copyOfRange(0, 32)
        val nonce = keyNonce.copyOfRange(32, 56)
        return if (seal) crypto.aeadEncrypt(data, nonce, key, ad) else crypto.aeadDecrypt(data, nonce, key, ad)
    }

    companion object {
        const val MAX_SKIP = 1000
        private val INFO_RK = "SatsChat_DR_RootKey_v1".encodeToByteArray()
        private val INFO_MK = "SatsChat_DR_MsgKey_v1".encodeToByteArray()

        fun generateDh(crypto: Crypto): DhKeyPair {
            val secret = crypto.randomBytes(32)
            return DhKeyPair(crypto.x25519PublicKey(secret), secret)
        }

        /** Alice = the initiator; she already holds Bob's initial DH public key (from the handshake). */
        fun initAlice(crypto: Crypto, sharedKey: ByteArray, bobDhPublic: ByteArray): DoubleRatchet {
            require(sharedKey.size == 32 && bobDhPublic.size == 32)
            val dhs = generateDh(crypto)
            val out = crypto.hkdfSha256(
                ikm = crypto.x25519ScalarMult(dhs.secret, bobDhPublic),
                salt = sharedKey,
                info = INFO_RK,
                length = 64,
            )
            return DoubleRatchet(
                crypto, dhs, bobDhPublic,
                rk = out.copyOfRange(0, 32), cks = out.copyOfRange(32, 64), ckr = null,
                ns = 0, nr = 0, pn = 0, skipped = mutableMapOf(),
            )
        }

        /** Bob = the responder; he keeps the DH key pair whose public key Alice used to bootstrap. */
        fun initBob(crypto: Crypto, sharedKey: ByteArray, bobDhKeyPair: DhKeyPair): DoubleRatchet {
            require(sharedKey.size == 32)
            return DoubleRatchet(
                crypto, bobDhKeyPair, dhr = null,
                rk = sharedKey, cks = null, ckr = null,
                ns = 0, nr = 0, pn = 0, skipped = mutableMapOf(),
            )
        }
    }
}

private fun Int.beBytes(): ByteArray =
    byteArrayOf((this ushr 24).toByte(), (this ushr 16).toByte(), (this ushr 8).toByte(), this.toByte())

private fun ByteArray.beInt(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 24) or
        ((this[offset + 1].toInt() and 0xFF) shl 16) or
        ((this[offset + 2].toInt() and 0xFF) shl 8) or
        (this[offset + 3].toInt() and 0xFF)
