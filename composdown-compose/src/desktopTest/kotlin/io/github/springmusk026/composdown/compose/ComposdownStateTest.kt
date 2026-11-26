package io.github.springmusk026.composdown.compose

import io.github.springmusk026.composdown.core.MarkdownParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComposdownStateTest {
    @Test
    fun queuedCommandsAreCombinedWithTheSameResult() =
        runTest {
            val scope = TestScope(StandardTestDispatcher(testScheduler))
            val state = ComposdownState(scope = scope, parsingContext = StandardTestDispatcher(testScheduler))
            // Queued while the parser is not running, so they are processed as one batch.
            state.append("ignored ")
            state.append("text")
            state.setText("# Title")
            state.append("\n\nA *para")
            state.append("graph*")
            state.finish()
            state.finish()
            state.append("\n\n- item")
            state.finish()
            scope.advanceUntilIdle()

            val expected = "# Title\n\nA *paragraph*\n\n- item"
            assertEquals(expected, state.text)
            assertEquals(MarkdownParser(ComposdownDefaults.Extensions).parse(expected), state.document)
            assertTrue(state.isFinished)
            scope.cancel()
        }

    @Test
    fun textSentAfterTheScopeIsCancelledIsIgnored() =
        runTest {
            val scope = TestScope(StandardTestDispatcher(testScheduler))
            val state = ComposdownState(initialText = "start", scope = scope)
            scope.advanceUntilIdle()
            scope.cancel()
            scope.advanceUntilIdle()
            state.append(" more")
            state.finish()
            scope.advanceUntilIdle()
            assertEquals("start", state.text)
        }
}
