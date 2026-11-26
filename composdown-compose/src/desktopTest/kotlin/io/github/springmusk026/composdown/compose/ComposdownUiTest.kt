package io.github.springmusk026.composdown.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.springmusk026.composdown.compose.internal.DefaultParagraph
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.plainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ComposdownUiTest {
    @Test
    fun headingsAreExposedAsHeadings() =
        runComposeUiTest {
            setContent { Composdown("# Title\n\nBody text") }
            onNode(hasText("Title") and isHeading()).assertExists()
            onNode(hasText("Body text") and isHeading()).assertDoesNotExist()
        }

    @Test
    fun clickingALinkCallsTheHandlerWithItsDestination() =
        runComposeUiTest {
            val clicked = mutableListOf<String>()
            setContent { Composdown("[docs](https://example.com/docs)", onLinkClick = { clicked += it }) }
            onNodeWithText("docs").performClick()
            assertEquals(listOf("https://example.com/docs"), clicked)
        }

    @Test
    fun defaultHandlerOpensWebLinksButNotOtherSchemes() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            val uriHandler =
                object : UriHandler {
                    override fun openUri(uri: String) {
                        opened += uri
                    }
                }
            setContent {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    Composdown("[web](https://example.com)\n\n[file](file:///etc/passwd)")
                }
            }
            onNodeWithText("web").performClick()
            onNodeWithText("file").performClick()
            assertEquals(listOf("https://example.com"), opened)
        }

    @Test
    fun standaloneImagesUseTheImageRenderer() =
        runComposeUiTest {
            val renderer = ImageRenderer { image, modifier -> Text("image: ${image.children.plainText()}", modifier) }
            setContent { Composdown("![A cat](https://example.com/cat.png)", imageRenderer = renderer) }
            onNodeWithText("image: A cat").assertExists()
        }

    @Test
    fun componentsReplaceTheDefaultRendering() =
        runComposeUiTest {
            val components =
                ComposdownComponents(
                    codeBlock =
                        BlockComponent {
                            block,
                            modifier,
                            ->
                            Text("custom ${block.language}", modifier.testTag("code"))
                        },
                )
            setContent { Composdown("```kotlin\nval x = 1\n```", components = components) }
            onNodeWithTag("code").assert(hasText("custom kotlin"))
        }

    @Test
    fun tablesExposeTheirStructure() =
        runComposeUiTest {
            setContent { Composdown("| a | b |\n| - | - |\n| 1 | 2 |\n| 3 | 4 |\n") }
            val table =
                SemanticsMatcher("is a 3x2 table") { node ->
                    node.config.getOrNull(SemanticsProperties.CollectionInfo)?.let {
                        it.rowCount == 3 &&
                            it.columnCount == 2
                    } ==
                        true
                }
            onNode(table).assertExists()
            onNodeWithText("4").assertExists()
        }

    @Test
    fun taskListItemsShowTheirState() =
        runComposeUiTest {
            setContent { Composdown("- [x] done\n- [ ] todo\n") }
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On)).assertExists()
            onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off),
            ).assertExists()
        }

    @Test
    fun footnotesAreRenderedAfterTheContent() =
        runComposeUiTest {
            setContent { Composdown("Claim[^1].\n\n[^1]: Source.\n") }
            onNodeWithText("Claim1.").assertExists()
            onNodeWithText("Source.").assertExists()
        }

    @Test
    fun streamedTextIsRenderedAsItArrives() =
        runComposeUiTest {
            val state =
                ComposdownState(scope = CoroutineScope(Dispatchers.Unconfined), parsingContext = Dispatchers.Unconfined)
            setContent { Composdown(state) }
            state.append("# Stre")
            waitForIdle()
            onNode(hasText("Stre") and isHeading()).assertExists()
            state.append("aming\n\nWith **bo")
            waitForIdle()
            onNodeWithText("With bo").assertExists()
            state.append("ld** text.")
            state.finish()
            waitForIdle()
            onNodeWithText("With bold text.").assertExists()
            assertTrue(state.isFinished)
        }

    @Test
    fun completedBlocksAreNotRecomposedWhileTextIsAppended() =
        runComposeUiTest {
            val compositions = mutableMapOf<String, Int>()
            val counting =
                BlockComponent<Block.Paragraph> { block, modifier ->
                    val text = block.content.plainText()
                    SideEffect { compositions[text] = (compositions[text] ?: 0) + 1 }
                    Box(modifier) { DefaultParagraph(block) }
                }
            val state =
                ComposdownState(scope = CoroutineScope(Dispatchers.Unconfined), parsingContext = Dispatchers.Unconfined)
            setContent {
                Composdown(
                    state,
                    components = ComposdownComponents(paragraph = counting),
                    selectable = false,
                )
            }
            state.append("First paragraph.\n\nSecond")
            waitForIdle()
            for (token in listOf(" paragraph", " keeps", " growing", ".")) {
                state.append(token)
                waitForIdle()
            }
            assertEquals(1, compositions["First paragraph."])
        }
}
