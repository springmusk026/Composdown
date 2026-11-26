# Module composdown-core

A Markdown parser for Kotlin Multiplatform, with no UI dependencies.

It implements [CommonMark 0.31.2](https://spec.commonmark.org/0.31.2/) completely, GitHub Flavored
Markdown (tables, task lists, strikethrough, extended autolinks, footnotes), and optional alerts and
math, and is verified against the specifications' example suites on every platform.

- [io.github.springmusk026.composdown.core.MarkdownParser] parses complete text.
- [io.github.springmusk026.composdown.core.IncrementalMarkdownParser] parses text that grows over
  time, re-parsing only its unfinished end.
- [io.github.springmusk026.composdown.core.ast.MarkdownDocument] is the immutable, structurally
  comparable result.

# Package io.github.springmusk026.composdown.core

Parsers, syntax extensions and streaming policies.

# Package io.github.springmusk026.composdown.core.ast

The document model: blocks, inline content and footnotes.
