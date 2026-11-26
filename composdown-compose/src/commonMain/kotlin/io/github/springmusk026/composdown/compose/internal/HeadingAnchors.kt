package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.DocumentHeading
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.ast.headings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The headings of the rendered document and where they are on screen, so that links to `#anchor`
 * scroll to their heading, as on GitHub.
 *
 * One instance lives as long as the `Composdown` that renders the document, and follows the document
 * as it changes, so that providing it never makes blocks recompose.
 */
internal class HeadingAnchors {
    private var document: MarkdownDocument? = null
    private var headings: List<DocumentHeading> = emptyList()
    private val requesters = HashMap<String, BringIntoViewRequester>()

    /** Follows [document]; its headings are computed only when needed. */
    fun update(document: MarkdownDocument) {
        if (document !== this.document) {
            this.document = document
            headings = emptyList()
        }
    }

    /** The anchor of [heading], identified by instance, or `null` if it is not in the document. */
    fun anchorOf(heading: Block.Heading): String? {
        if (headings.isEmpty()) headings = document?.headings().orEmpty()
        return headings.firstOrNull { it.block === heading }?.anchor
    }

    fun register(
        anchor: String,
        requester: BringIntoViewRequester,
    ) {
        requesters[anchor] = requester
    }

    fun unregister(
        anchor: String,
        requester: BringIntoViewRequester,
    ) {
        if (requesters[anchor] === requester) requesters.remove(anchor)
    }

    /**
     * Scrolls the heading that [destination] (`#anchor`) links to into view and returns `true`, or
     * returns `false` if it does not link to a heading of the document.
     */
    fun scrollTo(
        destination: String,
        scope: CoroutineScope,
    ): Boolean {
        if (!destination.startsWith('#')) return false
        val requester = requesters[destination.substring(1).lowercase()] ?: return false
        scope.launch { requester.bringIntoView() }
        return true
    }
}

/** Provided by `Composdown`; `null` where headings cannot be navigated to (such as in lazy lists). */
internal val LocalHeadingAnchors = staticCompositionLocalOf<HeadingAnchors?> { null }

/** A modifier that makes [heading] a target of `#anchor` links, if it has an anchor. */
@Composable
internal fun rememberHeadingTarget(heading: Block.Heading): Modifier {
    val anchors = LocalHeadingAnchors.current ?: return Modifier
    val anchor = remember(anchors, heading) { anchors.anchorOf(heading) } ?: return Modifier
    val requester = remember { BringIntoViewRequester() }
    DisposableEffect(anchors, anchor, requester) {
        anchors.register(anchor, requester)
        onDispose { anchors.unregister(anchor, requester) }
    }
    return Modifier.bringIntoViewRequester(requester)
}
