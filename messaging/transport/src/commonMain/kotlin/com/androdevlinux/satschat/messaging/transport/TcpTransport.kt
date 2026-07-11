package com.androdevlinux.satschat.messaging.transport

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByteArray
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Real plain-TCP transport over ktor-network. Messages are framed with a 4-byte big-endian length
 * prefix. Plain TCP works on Android and iOS native (ktor-network); TLS-over-raw-socket is NOT
 * supported on iOS (KTOR-2749), so the relay TLS transport (Phase 2) will use an NWConnection
 * cinterop actual on iOS. Integration-tested against the dockerized relay in Phase 2, not here.
 */
class TcpTransport internal constructor(
    private val socket: Socket,
    private val read: ByteReadChannel,
    private val write: ByteWriteChannel,
) : Transport {

    override suspend fun send(message: ByteArray) {
        write.writeByteArray(encodeLength(message.size))
        write.writeByteArray(message)
        write.flush()
    }

    override suspend fun receive(): ByteArray {
        val header = read.readByteArray(LENGTH_PREFIX_BYTES)
        val length = decodeLength(header)
        require(length in 0..MAX_FRAME_BYTES) { "invalid frame length $length" }
        return read.readByteArray(length)
    }

    override suspend fun close() {
        write.flushAndClose()
        socket.close()
    }

    companion object {
        private const val LENGTH_PREFIX_BYTES = 4
        private const val MAX_FRAME_BYTES = 16 * 1024 * 1024 // 16 MiB guard against a hostile length

        /** Connect to [host]:[port] over plain TCP. Caller owns the returned transport's lifecycle. */
        suspend fun connect(
            host: String,
            port: Int,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): TcpTransport {
            val selector = SelectorManager(dispatcher)
            val socket = aSocket(selector).tcp().connect(host, port)
            return TcpTransport(socket, socket.openReadChannel(), socket.openWriteChannel(autoFlush = false))
        }

        private fun encodeLength(value: Int): ByteArray = byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte(),
        )

        private fun decodeLength(header: ByteArray): Int =
            ((header[0].toInt() and 0xFF) shl 24) or
                ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)
    }
}
