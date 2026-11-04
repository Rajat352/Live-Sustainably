package com.livesustainably.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.livesustainably.R
import com.livesustainably.ui.common.dataErrorMessage
import com.livesustainably.ui.components.AppButton

@Composable
fun ChatScreen(
    state: ChatUiState,
    onAction: (ChatAction) -> Unit
) {

    val list = rememberLazyListState()
    val enabled = state.userId != null && !state.isReplying && !state.needsReply

    val lastItemIndex = state.messages.size + (if (state.messages.isEmpty()) 1 else 0) +
        (if (state.isReplying || state.needsReply) 1 else 0)

    LaunchedEffect(state.messages.lastOrNull()?.id, state.isReplying, state.needsReply, state.error) {
        list.animateScrollToItem(lastItemIndex)
    }

    Column(
        Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)
    ) {

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.chat_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { onAction(ChatAction.Clear) }, enabled = state.messages.isNotEmpty() || state.draft.isNotEmpty()) {
                Text(stringResource(R.string.chat_clear))
            }
        }

        LazyColumn(
            state = list, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.chat_explanation), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.messages.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.chat_welcome), style = MaterialTheme.typography.titleMedium)
                    listOf(R.string.chat_prompt_transport, R.string.chat_prompt_food, R.string.chat_prompt_week, R.string.chat_prompt_month).forEach { id ->
                        val prompt = stringResource(id)
                        OutlinedButton(onClick = { onAction(ChatAction.Ask(prompt)) }, enabled = enabled) { Text(prompt) }
                    }
                }
            }

            items(state.messages, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.9f), shape = MaterialTheme.shapes.large,
                        color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(if (message.fromUser) R.string.comment_you else R.string.chat_assistant), style = MaterialTheme.typography.labelMedium)
                            Text(message.text)
                        }
                    }
                }
            }

            if (state.isReplying || state.needsReply) item {
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isReplying) Text(stringResource(R.string.chat_replying))
                    else {
                        Text(state.error?.let { dataErrorMessage(it) } ?: stringResource(R.string.chat_interrupted), color = MaterialTheme.colorScheme.error)
                        AppButton(stringResource(R.string.chat_retry), { onAction(ChatAction.Retry) })
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.draft, onValueChange = { onAction(ChatAction.UpdateDraft(it)) },
            modifier = Modifier.fillMaxWidth(), enabled = enabled, maxLines = 4,
            label = { Text(stringResource(R.string.chat_message)) }, isError = state.invalidDraft,
            supportingText = { Text(if (state.invalidDraft) stringResource(R.string.chat_validation) else stringResource(R.string.chat_counter, state.draft.length)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onAction(ChatAction.Send) }),
        )

        AppButton(stringResource(R.string.chat_send), { onAction(ChatAction.Send) },
            modifier = Modifier.align(Alignment.End).padding(bottom = 8.dp), enabled = enabled && state.draft.isNotBlank())
    }
}
