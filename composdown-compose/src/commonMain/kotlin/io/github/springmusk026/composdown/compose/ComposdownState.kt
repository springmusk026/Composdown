package io.github.springmusk026.composdown.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import io.github.springmusk026.composdown.core.IncrementalMarkdownParser
import io.github.springmusk026.composdown.core.MarkdownExtension
import io.github.springmusk026.composdown.core.StreamingPolicy
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Markdown text that can grow while it is displayed, such as a response streamed from a language
 * model, together with its parsed [document].
 *
 * [append], [setText] and [finish] return immediately and can be called from any thread. Text sent
 * after [scope] is cancelled is ignored. Parsing
 * runs in [scope] on [parsingContext] (a background dispatcher by default), one update at a time;
 * text that arrives while an update is being parsed is combined into the next one. Only the open end
 * of the document is parsed again on each update, and completed blocks keep the same instances, so
 * they are not recomposed.
 *
 * Use [rememberComposdownState] in composition, or create one in a view model with its scope.
 *
 * @param initialText text to start with, parsed immediately on the calling thread.
 * @param extensions the syntax extensions to recognize.
 * @param streamingPolicy how incomplete syntax at the end of the text is shown before [finish].
 * @param scope the scope of the parsing coroutine; parsing stops when it is cancelled.
 * @param parsingContext where parsing runs.
 */
@Stable
public class ComposdownState(
    initialText: String = "",
    extensions: Set<MarkdownExtension> = ComposdownDefaults.Extensions,
    streamingPolicy: StreamingPolicy = StreamingPolicy.OPTIMISTIC,
    scope: CoroutineScope,
    parsingContext: CoroutineContext = Dispatchers.Default,
) {
    private sealed interface Command {
        class Append(
            val chunk: String,
        ) : Command

        class SetText(
            val text: String,
        ) : Command

        data object Finish : Command
    }

    private val parser = IncrementalMarkdownParser(extensions, streamingPolicy)

    /**
     * The commands that have the same effect as [commands] applied in order, with as few parses as
     * possible: consecutive appends are joined, and text replaced by [Command.SetText] is dropped.
     */
    private fun coalesce(commands: List<Command>): List<Command> {
        val result = ArrayList<Command>(commands.size)
        for (command in commands) {
            val last = result.lastOrNull()
            when {
                command is Command.SetText -> {
                    result.clear()
                    result += command
                }

                command is Command.Append && last is Command.Append -> {
                    result[result.lastIndex] = Command.Append(last.chunk + command.chunk)
                }

                command is Command.Append && last is Command.SetText -> {
                    result[result.lastIndex] = Command.SetText(last.text + command.chunk)
                }

                command is Command.Finish && last is Command.Finish -> {
                    // Finishing twice is the same as finishing once.
                }

                else -> {
                    result += command
                }
            }
        }
        return result
    }

    private val commands = Channel<Command>(Channel.UNLIMITED)

    /** The document for the text parsed so far. */
    public var document: MarkdownDocument by mutableStateOf(parser.setText(initialText))
        private set

    private var parsedText: String by mutableStateOf(initialText)

    /** The text parsed so far, which may lag behind text passed to [append] until it is parsed. */
    public val text: String get() = parsedText

    /** Whether [finish] was processed and no text was added since. */
    public var isFinished: Boolean by mutableStateOf(false)
        private set

    init {
        val worker =
            scope.launch(parsingContext) {
                for (first in commands) {
                    // Everything that arrived meanwhile is combined with the first command, so that a
                    // fast producer costs one parse per update rather than one per chunk.
                    val pending = mutableListOf(first)
                    while (true) pending += commands.tryReceive().getOrNull() ?: break
                    for (command in coalesce(pending)) apply(command)
                    val parsed = parser.document
                    val parsedText = parser.text
                    val finished = parser.isFinished
                    Snapshot.withMutableSnapshot {
                        document = parsed
                        this@ComposdownState.parsedText = parsedText
                        isFinished = finished
                    }
                }
            }
        // Once parsing stops, text sent afterwards is dropped instead of being kept forever.
        worker.invokeOnCompletion { commands.close() }
    }

    private fun apply(command: Command) {
        when (command) {
            is Command.Append -> parser.append(command.chunk)
            is Command.SetText -> parser.setText(command.text)
            Command.Finish -> parser.finish()
        }
    }

    /** Appends [chunk] to the text. Chunks may split the text anywhere. */
    public fun append(chunk: String) {
        commands.trySend(Command.Append(chunk))
    }

    /** Replaces the whole text. If it extends the current text, this is as cheap as [append]. */
    public fun setText(text: String) {
        commands.trySend(Command.SetText(text))
    }

    /**
     * Declares that the text is complete. The final document is exactly what parsing the whole text
     * at once produces.
     */
    public fun finish() {
        commands.trySend(Command.Finish)
    }
}

/**
 * Remembers a [ComposdownState] whose parsing runs while it is in the composition.
 *
 * ```
 * val state = rememberComposdownState()
 * LaunchedEffect(response) {
 *     response.tokens.collect(state::append)
 *     state.finish()
 * }
 * Composdown(state)
 * ```
 *
 * The parameters are read when the state is first created.
 */
@Composable
public fun rememberComposdownState(
    initialText: String = "",
    extensions: Set<MarkdownExtension> = ComposdownDefaults.Extensions,
    streamingPolicy: StreamingPolicy = StreamingPolicy.OPTIMISTIC,
): ComposdownState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ComposdownState(initialText, extensions, streamingPolicy, scope) }
}
