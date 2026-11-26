# Benchmark baseline

Recorded with `./gradlew :benchmarks:benchmark` (JMH via kotlinx-benchmark: 5 warm-up and 10
measurement iterations of 1 s, average time per operation). Lower is better.

| Benchmark | What it measures | Time (ms/op) |
|---|---|---|
| `parseLargeDocument` | One-shot parse of a 73 KB document (headings, lists, code, tables, quotes, footnotes) | 1.65 ± 0.13 |
| `streamTokensIncrementally` | 2,000 tokens (4.2 KB) appended one at a time with `IncrementalMarkdownParser`, a document produced after each, then `finish()` | 4.84 ± 0.24 |
| `streamTokensAfterLongDocument` | `setText` of the 73 KB document, then the same 2,000 tokens appended | 9.86 ± 0.52 |
| `streamTokensWithFullReparse` | The same 2,000 tokens, re-parsing the whole text with `MarkdownParser` after each one | 89.12 ± 5.46 |
| `renderHtml` | The 73 KB document rendered with `toHtml()` (default, safe options) | 1.07 ± 0.09 |
| `renderHtmlWithHeadingIds` | The same, with heading ids (`HtmlOptions(headingIds = true)`) | 1.17 ± 0.09 |
| `extractPlainText` | `plainText()` of the 73 KB document | 0.32 ± 0.03 |
| `listHeadings` | `headings()` of the 73 KB document, with anchors | 0.08 ± 0.004 |

Observations:

- Incremental streaming costs about 2.4 µs per token, about 18 times less than re-parsing the whole
  text after each token, which is what a renderer without incremental parsing does.
- Streaming after a 73 KB prefix costs about the same per token once the prefix itself is parsed
  (9.9 ms total, of which the 73 KB `setText` is roughly one full parse): the work per update is
  proportional to the open end of the document, not to its length.
- Rendering HTML takes about two thirds of the time of parsing, and plain text and headings are
  cheap enough to compute on every update of a streamed document.

Environment: OpenJDK 21.0.13, Linux, 4 vCPUs (Intel Xeon @ 2.10 GHz, shared cloud machine).
Absolute numbers vary between machines, and between runs on shared machines by 10 to 20%; compare
runs on the same machine, one after the other.

These are JVM numbers. Android performance is measured separately with Macrobenchmark on a device.
