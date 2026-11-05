package com.livesustainably.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.livesustainably.ui.components.*

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
        if (state.messages.isNotEmpty() || state.isReplying || state.needsReply) {
            list.animateScrollToItem(lastItemIndex)
        } else {
            list.scrollToItem(0)
        }
    }

    Column(
        Modifier.fillMaxSize().imePadding()
    ) {

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SproutMascot(Modifier.size(50.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Sprout", style = MaterialTheme.typography.titleLarge)
                Text("Your little green guide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onAction(ChatAction.Clear) }, enabled = state.messages.isNotEmpty() || state.draft.isNotEmpty()) {
                Text(stringResource(R.string.chat_clear))
            }
        }

        LazyColumn(
            state = list, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                if (state.messages.isEmpty()) HeroCard("Hey there,\nplanet pal!", "Let’s make greener choices feel a little easier.")
                else Text(stringResource(R.string.chat_explanation), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.messages.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.chat_welcome), style = MaterialTheme.typography.titleMedium)
                    listOf(R.string.chat_prompt_transport, R.string.chat_prompt_food, R.string.chat_prompt_week, R.string.chat_prompt_month).forEach { id ->
                        val prompt = stringResource(id)
                        Surface(
                            onClick = { onAction(ChatAction.Ask(prompt)) }, enabled = enabled,
                            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(prompt + "  ↗", Modifier.fillMaxWidth().padding(16.dp),
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            items(state.messages, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(0.92f),
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp,
                            bottomStart = if (message.fromUser) 26.dp else 6.dp,
                            bottomEnd = if (message.fromUser) 6.dp else 26.dp),
                        color = if (message.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (message.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        border = if (message.fromUser) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(if (message.fromUser) R.string.comment_you else R.string.chat_assistant), style = MaterialTheme.typography.labelSmall)
                            Text(message.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (state.isReplying || state.needsReply) item {
                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.isReplying) Text("🌱  " + stringResource(R.string.chat_replying))
                    else {
                        Text(state.error?.let { dataErrorMessage(it) } ?: stringResource(R.string.chat_interrupted), color = MaterialTheme.colorScheme.error)
                        AppButton(stringResource(R.string.chat_retry), { onAction(ChatAction.Retry) })
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {

            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                AppTextField(
                    value = state.draft, onValueChange = { onAction(ChatAction.UpdateDraft(it)) },
                    modifier = Modifier.fillMaxWidth(), enabled = enabled, maxLines = 4,
                    label = { Text(stringResource(R.string.chat_message)) }, isError = state.invalidDraft,
                    supportingText = { Text(if (state.invalidDraft) stringResource(R.string.chat_validation) else stringResource(R.string.chat_counter, state.draft.length)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onAction(ChatAction.Send) }),
                )

                AppButton(stringResource(R.string.chat_send), { onAction(ChatAction.Send) },
                    modifier = Modifier.align(Alignment.End), enabled = enabled && state.draft.isNotBlank())
            }
        }
    }
}
