package com.androdevlinux.satschat.messaging.transport

import kotlinx.coroutines.channels.Channel

/**
 * In-memory paired transport for deterministic tests: bytes sent on one endpoint arrive on the other.
 * Lives in commonMain (not commonTest) so other modules' tests — e.g. the Phase 2 two-agent SMP
 * tests — can drive a full lifecycle over it without a real relay. Not for production use.
 */
class FakeTransport private constructor(
    private val outbound: Channel<ByteArray>,
    private val inbound: Channel<ByteArray>,
) : Transport {
    override suspend fun send(message: ByteArray) {
        outbound.send(message)
    }

    override suspend fun receive(): ByteArray = inbound.receive()

    override suspend fun close() {
        outbound.close()
    }

    companion object {
        /** Returns two endpoints wired to each other (unbounded, so a test send never blocks). */
        fun pair(): Pair<FakeTransport, FakeTransport> {
            val aToB = Channel<ByteArray>(Channel.UNLIMITED)
            val bToA = Channel<ByteArray>(Channel.UNLIMITED)
            return FakeTransport(aToB, bToA) to FakeTransport(bToA, aToB)
        }
    }
}
