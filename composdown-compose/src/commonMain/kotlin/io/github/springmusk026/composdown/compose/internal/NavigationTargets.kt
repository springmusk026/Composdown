package io.github.springmusk026.composdown.compose.internal

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.springmusk026.composdown.core.ast.Block
import io.github.springmusk026.composdown.core.ast.DocumentHeading
import io.github.springmusk026.composdown.core.ast.Inline
import io.github.springmusk026.composdown.core.ast.MarkdownDocument
import io.github.springmusk026.composdown.core.ast.headings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Where the navigation targets of the rendered document are on screen: headings (for `#anchor`
 * links, as on GitHub), footnotes (for footnote references) and the first reference to each
 * footnote (for a footnote's way back).
 *
 * One instance lives as long as the `Composdown` that renders the document, and follows the document
 * as it changes, so that providing it never makes blocks recompose.
 */
internal class NavigationTargets {
    private var document: MarkdownDocument? = null
    private var headings: List<DocumentHeading> = emptyList()
    private val headingTargets = HashMap<String, BringIntoViewRequester>()
    private val footnoteTargets = HashMap<String, BringIntoViewRequester>()
    private val referenceTargets = LinkedHashMap<String, MutableList<BringIntoViewRequester>>()

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

    fun registerHeading(
        anchor: String,
        requester: BringIntoViewRequester,
    ): () -> Unit = register(headingTargets, anchor, requester)

    fun registerFootnote(
        label: String,
        requester: BringIntoViewRequester,
    ): () -> Unit = register(footnoteTargets, label, requester)

    /** Registers text that references the footnote [label]; the first one registered is the way back. */
    fun registerReference(
        label: String,
        requester: BringIntoViewRequester,
    ): () -> Unit {
        referenceTargets.getOrPut(label) { mutableListOf() } += requester
        return {
            referenceTargets[label]?.let { requesters ->
                requesters.remove(requester)
                if (requesters.isEmpty()) referenceTargets.remove(label)
            }
        }
    }

    /**
     * Scrolls the heading that [destination] (`#anchor`) links to into view and returns `true`, or
     * returns `false` if it does not link to a heading of the document.
     */
    fun scrollToHeading(
        destination: String,
        scope: CoroutineScope,
    ): Boolean {
        if (!destination.startsWith('#')) return false
        return bringIntoView(headingTargets[destination.substring(1).lowercase()], scope)
    }

    /** Scrolls the definition of the footnote [label] into view, if it is shown. */
    fun scrollToFootnote(
        label: String,
        scope: CoroutineScope,
    ): Boolean = bringIntoView(footnoteTargets[label], scope)

    /** Scrolls the first text that references the footnote [label] into view, if it is shown. */
    fun scrollToReference(
        label: String,
        scope: CoroutineScope,
    ): Boolean = bringIntoView(referenceTargets[label]?.firstOrNull(), scope)

    private fun register(
        targets: HashMap<String, BringIntoViewRequester>,
        key: String,
        requester: BringIntoViewRequester,
    ): () -> Unit {
        targets[key] = requester
        return { if (targets[key] === requester) targets.remove(key) }
    }

    private fun bringIntoView(
        requester: BringIntoViewRequester?,
        scope: CoroutineScope,
    ): Boolean {
        if (requester == null) return false
        scope.launch { requester.bringIntoView() }
        return true
    }
}

/** Provided by `Composdown`; `null` where targets cannot be navigated to (such as in lazy lists). */
internal val LocalNavigationTargets = staticCompositionLocalOf<NavigationTargets?> { null }

/** A modifier that makes [heading] a target of `#anchor` links, if it has an anchor. */
@Composable
internal fun rememberHeadingTarget(heading: Block.Heading): Modifier {
    val targets = LocalNavigationTargets.current ?: return Modifier
    val anchor = remember(targets, heading) { targets.anchorOf(heading) } ?: return Modifier
    return rememberTarget(targets, anchor) { requester -> targets.registerHeading(anchor, requester) }
}

/** A modifier that makes a footnote's definition the target of its references. */
@Composable
internal fun rememberFootnoteTarget(label: String): Modifier {
    val targets = LocalNavigationTargets.current ?: return Modifier
    return rememberTarget(targets, label) { requester -> targets.registerFootnote(label, requester) }
}

/** A modifier that makes text containing footnote references the target of the footnotes' way back. */
@Composable
internal fun rememberReferenceTarget(inlines: List<Inline>): Modifier {
    val targets = LocalNavigationTargets.current ?: return Modifier
    val labels = remember(inlines) { inlines.footnoteLabels() }
    if (labels.isEmpty()) return Modifier
    return rememberTarget(targets, labels) { requester ->
        val unregister = labels.map { targets.registerReference(it, requester) }
        return@rememberTarget { unregister.forEach { it() } }
    }
}

@Composable
private fun rememberTarget(
    targets: NavigationTargets,
    key: Any,
    register: (BringIntoViewRequester) -> () -> Unit,
): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val currentRegister by rememberUpdatedState(register)
    DisposableEffect(targets, key, requester) {
        val unregister = currentRegister(requester)
        onDispose { unregister() }
    }
    return Modifier.bringIntoViewRequester(requester)
}

/** The labels of the footnotes referenced in these inline elements, including nested ones. */
private fun List<Inline>.footnoteLabels(): List<String> =
    buildList {
        fun visit(inlines: List<Inline>) {
            for (inline in inlines) {
                when (inline) {
                    is Inline.FootnoteReference -> add(inline.label)
                    is Inline.Emphasis -> visit(inline.children)
                    is Inline.Strong -> visit(inline.children)
                    is Inline.Strikethrough -> visit(inline.children)
                    is Inline.Link -> visit(inline.children)
                    else -> Unit
                }
            }
        }
        visit(this@footnoteLabels)
    }.distinct()
