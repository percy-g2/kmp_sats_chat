package com.androdevlinux.satschat.core.crypto

/** Lowercase-hex helpers, used by KATs and wire encoding. */
fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "hex must have even length" }
    return ByteArray(hex.length / 2) {
        ((hex[it * 2].digitToInt(16) shl 4) or hex[it * 2 + 1].digitToInt(16)).toByte()
    }
}

fun ByteArray.toHex(): String = buildString(size * 2) {
    for (b in this@toHex) {
        val v = b.toInt() and 0xFF
        append("0123456789abcdef"[v shr 4])
        append("0123456789abcdef"[v and 0x0F])
    }
}
