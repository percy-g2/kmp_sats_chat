package com.androdevlinux.satschat.core.model

/** Delivery state of a chat message, surfaced in the UI as a status indicator. */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
}

/**
 * A single conversation message. `seq` orders messages within a conversation. NOTE: this is the UI/
 * domain shape; the on-the-wire, ratchet-encrypted form is defined by :messaging:smp once Phase 1
 * crypto lands. Nothing here is encrypted yet.
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val text: String,
    val fromMe: Boolean,
    val seq: Long,
    val status: MessageStatus,
)
