package com.livesustainably.ui.screens.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livesustainably.domain.model.DataFailure
import com.livesustainably.domain.model.UserSession
import com.livesustainably.domain.repository.AuthRepository
import com.livesustainably.domain.repository.ChatRepository
import com.livesustainably.ui.common.dataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltViewModel
class ChatViewModel @Inject constructor(
    auth: AuthRepository,
    private val chat: ChatRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()
    private var response: Job? = null

    init {

        viewModelScope.launch {
            auth.session.filter { it != UserSession.Loading }
                .map { (it as? UserSession.SignedIn)?.userId }.distinctUntilChanged().collect { userId ->
                    response?.cancelAndJoin()
                    val sameOwner = userId != null && savedState.get<String>("chatOwner") == userId
                    val messages = if (sameOwner) restoreMessages() else emptyList()
                    _state.value = ChatUiState(
                        userId = userId,
                        messages = messages,
                        draft = if (sameOwner) savedState.get<String>("chatDraft").orEmpty().take(501) else "",
                        needsReply = messages.lastOrNull()?.fromUser == true,
                    )
                    save()
                }
        }
    }

    fun onAction(action: ChatAction) {

        when (action) {
            is ChatAction.UpdateDraft -> if (!state.value.isReplying && !state.value.needsReply) {
                _state.value = state.value.copy(draft = action.value.take(501), invalidDraft = false)
                save()
            }

            is ChatAction.Ask -> if (!state.value.isReplying && !state.value.needsReply) send(action.prompt)

            ChatAction.Send -> send(state.value.draft)

            ChatAction.Retry -> if (state.value.needsReply && !state.value.isReplying && state.value.userId != null) reply()

            ChatAction.Clear -> {
                response?.cancel()
                _state.value = ChatUiState(userId = state.value.userId)
                save()
            }
        }
    }

    private fun send(value: String) {
        val input = state.value
        if (input.userId == null || input.isReplying || input.needsReply) return
        val text = value.trim()

        if (text.length !in 1..500) {
            _state.value = input.copy(invalidDraft = true)
            return
        }

        _state.value = input.copy(
            messages = (input.messages + ChatMessage(nextId(), text, true)).takeLast(MAX_MESSAGES),
            draft = "", needsReply = true, invalidDraft = false, error = null,
        )
        save()
        reply()
    }

    private fun reply() {
        val input = state.value
        val message = input.messages.lastOrNull()?.takeIf { it.fromUser } ?: return
        _state.value = input.copy(isReplying = true, error = null)

        response = viewModelScope.launch {
            try {
                val answer = withTimeoutOrNull(10_000) { chat.reply(message.text) }
                if (answer == null) {
                    _state.value = state.value.copy(isReplying = false, error = DataFailure.Network)
                } else {
                    require(answer.isNotBlank() && answer.length <= 2000)
                    _state.value = state.value.copy(
                        messages = (state.value.messages + ChatMessage(nextId(), answer, false)).takeLast(MAX_MESSAGES),
                        isReplying = false, needsReply = false, error = null,
                    )
                }
                save()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _state.value = state.value.copy(isReplying = false, error = dataFailure(e))
                Log.e(TAG, "An error occurred while sending a reply", e)
            }
        }
    }

    private fun nextId(): Long = (state.value.messages.lastOrNull()?.id ?: 0) + 1

    private fun save() {
        savedState["chatOwner"] = state.value.userId
        savedState["chatDraft"] = state.value.draft
        savedState["chatMessages"] = Json.encodeToString(state.value.messages)
    }

    private fun restoreMessages(): List<ChatMessage> = runCatching {
        Json.decodeFromString<List<ChatMessage>>(savedState.get<String>("chatMessages") ?: "[]")
            .takeLast(MAX_MESSAGES).also { messages ->
                require(messages.all { it.id > 0 && it.text.isNotBlank() && it.text.length <= if (it.fromUser) 500 else 2000 })
                require(messages.zipWithNext().all { (a, b) -> a.id < b.id && a.fromUser != b.fromUser })
            }
    }.getOrDefault(emptyList())

    companion object {
        const val TAG = "ChatViewModel"
        const val MAX_MESSAGES = 20
    }
}
