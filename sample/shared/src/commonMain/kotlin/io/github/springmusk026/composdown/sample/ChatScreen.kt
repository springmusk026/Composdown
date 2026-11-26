package io.github.springmusk026.composdown.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.springmusk026.composdown.compose.CodeHighlighter
import io.github.springmusk026.composdown.compose.ComposdownState
import io.github.springmusk026.composdown.compose.ImageRenderer
import io.github.springmusk026.composdown.compose.composdownItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One exchange of the chat: the user's prompt and the streamed response. */
private class ChatMessage(
    val id: Int,
    val prompt: String,
    val response: ComposdownState,
)

/**
 * A chat with a simulated language model. Choosing a prompt streams a canned response token by
 * token; each response's blocks are items of the chat's lazy list.
 */
@Composable
internal fun ChatScreen(
    imageRenderer: ImageRenderer,
    codeHighlighter: CodeHighlighter,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    val lastDocument = messages.lastOrNull()?.response?.document
    LaunchedEffect(messages.size, lastDocument) {
        // Follow the response as it streams in.
        if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }
    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
        ) {
            for (message in messages) {
                item(key = "${message.id}:prompt") { PromptBubble(message.prompt) }
                composdownItems(
                    message.response.document,
                    keyPrefix = message.id.toString(),
                    imageRenderer = imageRenderer,
                    codeHighlighter = codeHighlighter,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((prompt, response) in SampleContent.prompts) {
                AssistChip(
                    onClick = { messages += startResponse(messages.size, prompt, response, scope) },
                    label = { Text(prompt) },
                )
            }
        }
    }
}

private fun startResponse(
    id: Int,
    prompt: String,
    response: String,
    scope: CoroutineScope,
): ChatMessage {
    val state = ComposdownState(extensions = SampleExtensions, scope = scope)
    scope.launch {
        simulatedTokenStream(response).collect(state::append)
        state.finish()
    }
    return ChatMessage(id, prompt, state)
}

@Composable
private fun PromptBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text = text,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
