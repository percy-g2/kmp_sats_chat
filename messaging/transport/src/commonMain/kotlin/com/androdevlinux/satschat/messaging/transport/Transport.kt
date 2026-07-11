package com.androdevlinux.satschat.messaging.transport

/**
 * A message-oriented, full-duplex byte transport. Real implementations frame messages over a byte
 * stream (see [TcpTransport]); [FakeTransport] is an in-memory pair for tests. All calls suspend and
 * must be driven within structured concurrency with an explicit dispatcher chosen by the caller.
 */
interface Transport {
    /** Send one message. Suspends until the message is accepted by the transport. */
    suspend fun send(message: ByteArray)

    /** Receive the next message. Suspends until one is available; fails once the transport is closed and drained. */
    suspend fun receive(): ByteArray

    /** Close the transport. Subsequent [receive] calls fail after any buffered messages are drained. */
    suspend fun close()
}
