package com.androdevlinux.satschat.feature.chat

import com.androdevlinux.satschat.core.model.ChatMessage
import com.androdevlinux.satschat.core.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * In-memory conversation state for the Phase 3 chat UI. Sending appends a local message and, after a
 * short delay, a fake peer echo — purely so the UX is exercisable on-device.
 *
 * TODO(security-review): NOT wired to the SMP agent, the double ratchet, or a relay yet — nothing
 * here is encrypted or actually sent. Replaced by the real repository once Phases 1–2 land.
 */
class ChatStore(
    private val scope: CoroutineScope,
    private val conversationId: String = "demo",
) {
    private val _messages = MutableStateFlow(seed())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var seq = 3L

    fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val n = seq++
        _messages.update {
            it + ChatMessage("m$n", conversationId, body, fromMe = true, seq = n, status = MessageStatus.SENT)
        }
        scope.launch {
            delay(700)
            val r = seq++
            _messages.update {
                it + ChatMessage("m$r", conversationId, "echo: $body", fromMe = false, seq = r, status = MessageStatus.DELIVERED)
            }
        }
    }

    private fun seed(): List<ChatMessage> = listOf(
        ChatMessage("m1", conversationId, "gm — welcome to SatsChat", fromMe = false, seq = 1, status = MessageStatus.DELIVERED),
        ChatMessage("m2", conversationId, "this screen runs on-device; encryption + Lightning come next", fromMe = true, seq = 2, status = MessageStatus.READ),
    )
}
