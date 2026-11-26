package io.github.springmusk026.composdown.core.internal.inline

/**
 * Mutable inline tree used while parsing. Nodes form doubly linked sibling lists so that emphasis and
 * link processing can move runs of nodes in constant time. The finished tree is converted to the
 * immutable [io.github.springmusk026.composdown.core.ast.Inline] model.
 */
internal class InlineNode(
    val kind: Kind,
    var literal: String = "",
) {
    enum class Kind { ROOT, TEXT, CODE, EMPHASIS, STRONG, LINK, IMAGE, HTML, SOFT_BREAK, HARD_BREAK }

    var destination: String = ""
    var title: String? = null

    var parent: InlineNode? = null
        private set
    var firstChild: InlineNode? = null
        private set
    var lastChild: InlineNode? = null
        private set
    var previous: InlineNode? = null
        private set
    var next: InlineNode? = null
        private set

    fun appendChild(child: InlineNode) {
        child.unlink()
        child.parent = this
        val last = lastChild
        if (last == null) {
            firstChild = child
        } else {
            last.next = child
            child.previous = last
        }
        lastChild = child
    }

    fun insertAfter(sibling: InlineNode) {
        sibling.unlink()
        val parent = checkNotNull(parent) { "Cannot insert after a detached node" }
        sibling.parent = parent
        sibling.previous = this
        sibling.next = next
        next?.previous = sibling
        next = sibling
        if (parent.lastChild === this) parent.lastChild = sibling
    }

    fun unlink() {
        val parent = parent ?: return
        val previous = previous
        val next = next
        if (previous == null) parent.firstChild = next else previous.next = next
        if (next == null) parent.lastChild = previous else next.previous = previous
        this.parent = null
        this.previous = null
        this.next = null
    }
}
