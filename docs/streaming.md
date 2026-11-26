# Live content

Some content is displayed while it is still being written: responses streamed from a language model,
collaborative or live-edited notes, logs and transcripts. Rendering such text by re-parsing and
re-composing everything on every change wastes work that grows with the document, and makes half-typed
syntax flicker on screen (`**` appearing as asterisks, then disappearing into bold text).

Composdown handles this with an incremental parser and a state holder built on it.

- [ComposdownState](#composdownstate)
- [Using a view model](#using-a-view-model)
- [Rendering in a lazy list](#rendering-in-a-lazy-list)
- [Streaming policies](#streaming-policies)
- [Guarantees](#guarantees)
- [Without Compose](#without-compose)
- [Performance characteristics](#performance-characteristics)

## ComposdownState

```kotlin
@Composable
fun AssistantMessage(response: Flow<String>) {
    val state = rememberComposdownState()

    LaunchedEffect(response) {
        response.collect { token -> state.append(token) }
        state.finish()
    }

    Composdown(state = state)
}
```

| Member | Purpose |
|---|---|
| `append(chunk)` | Adds text. Chunks may split the text anywhere, even inside a line ending or a multi-byte construct. |
| `setText(text)` | Replaces the text. If the new text extends the current one, this costs the same as `append`. |
| `finish()` | Declares the text complete. The document becomes exactly what a one-shot parse produces. |
| `document` | The current `MarkdownDocument`, observable by Compose. |
| `text` | The text parsed so far. |
| `isFinished` | Whether `finish()` was called and no text was added since. Observable. |

`append`, `setText` and `finish` return immediately and may be called from any thread. Parsing runs
on `Dispatchers.Default`, one update at a time. Text that arrives while an update is being parsed is
combined into the next update, so a fast producer never builds a backlog: the UI always converges on
the latest text.

`rememberComposdownState(initialText, extensions, streamingPolicy)` reads its parameters once, when the
state is created. To start over with a different configuration, key it:
`remember(messageId) { ... }` or create a new state.

## Using a view model

Create the state where the text is produced, with that component's scope, so that parsing survives
configuration changes and stops when the scope ends:

```kotlin
class ChatViewModel(private val client: ChatClient) : ViewModel() {
    val reply = ComposdownState(scope = viewModelScope)

    fun ask(prompt: String) {
        reply.setText("")
        viewModelScope.launch {
            client.stream(prompt).collect(reply::append)
            reply.finish()
        }
    }
}

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    Composdown(state = viewModel.reply)
}
```

`ComposdownState(initialText, extensions, streamingPolicy, scope, parsingContext)` lets you choose the
parsing context; the default is `Dispatchers.Default`.

## Rendering in a lazy list

A chat typically shows many messages, only one of which is still growing. Render each message's
current document with `composdownItems`:

```kotlin
LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    for (message in messages) {
        composdownItems(message.state.document, keyPrefix = message.id)
    }
}
```

Item keys are positional, and the incremental parser never reorders completed blocks, so as a message
grows, items above its end keep their keys and are neither moved nor recomposed. Only the last item of
the growing message changes. See the [sample](../sample) for a complete chat screen.

## Streaming policies

A streaming policy decides how unfinished syntax at the very end of the text is shown **before**
`finish()` is called. It never affects the final document.

| Policy | Behavior | Use when |
|---|---|---|
| `StreamingPolicy.OPTIMISTIC` (default) | Shows unfinished syntax in its likely final form and holds back fragments that would flicker | Displaying text to people |
| `StreamingPolicy.STRICT` | Parses the text exactly as written at every step | Tooling, tests, or when intermediate states must match a plain parse |

The optimistic policy:

| Text so far | Shown as | Reason |
|---|---|---|
| `**bold te` | **bold te** | Unclosed emphasis, strong, strikethrough and code spans are closed provisionally |
| `[docs](https://exa` | A link | A link whose destination is still being written |
| `Some text **` | Some text | A trailing `*`, `_`, `~` or `` ` `` run is held until the next character |
| `` ``` `` or `1.` or `-` on a new line | Nothing yet | A line that may become a block marker is held until it is complete |
| `\| Name \| Role \|` | Nothing yet | A possible table header is held until the next line shows whether a table starts |

If the text ends without completing the syntax (for example, a single `*` meant literally), the final
document differs from the last intermediate one in exactly that detail.

## Guarantees

- **Equivalence.** After `finish()`, the document is identical to `MarkdownParser(extensions).parse(text)`,
  whatever the chunking. This is verified by streaming every specification example character by
  character, in random chunk sizes, through random `setText` edits, and on randomly generated
  documents.
- **Stability.** Completed top-level blocks keep the same object instances across updates. Compose
  compares them by reference and skips them.
- **Bounded work.** Each update re-parses only the open block at the end of the document and the line
  being written.
- **Retroactive references.** A link reference definition (`[id]: https://...`) or footnote that
  arrives after the text using it updates the earlier, already completed blocks. This is the only way
  later text changes a completed block.

## Without Compose

`IncrementalMarkdownParser` in `composdown-core` offers the same engine synchronously, for any UI
toolkit or for server-side processing:

```kotlin
val parser = IncrementalMarkdownParser(
    extensions = MarkdownExtension.GFM,
    streamingPolicy = StreamingPolicy.OPTIMISTIC,
)
tokens.collect { token -> render(parser.append(token)) }
render(parser.finish())
```

Each call returns the updated document. Instances are not thread-safe; use one from a single thread at
a time.

## Performance characteristics

Measured on the JVM (see [`benchmarks/BASELINE.md`](../benchmarks/BASELINE.md)): streaming 2,000 tokens
and producing a document after every token takes 4.8 ms in total, against 89 ms when re-parsing the
whole text each time. Appending the same 2,000 tokens after 73 KB of existing content takes
9.9 ms: the document that came before adds little, because completed blocks are not parsed again.
The cost does grow with the size of the open block itself, so a single
paragraph or code block of many kilobytes costs more per update than many small blocks.
