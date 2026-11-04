package com.livesustainably.ui.screens.chat

import androidx.compose.runtime.Stable
import com.livesustainably.domain.model.DataFailure
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean
)

@Stable
data class ChatUiState(
    val userId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isReplying: Boolean = false,
    val needsReply: Boolean = false,
    val invalidDraft: Boolean = false,
    val error: DataFailure? = null,
)

sealed interface ChatAction {
    data class UpdateDraft(val value: String) : ChatAction
    data class Ask(val prompt: String) : ChatAction
    data object Send : ChatAction
    data object Retry : ChatAction
    data object Clear : ChatAction
}
