# Parser benchmark baseline

Recorded with `./gradlew :benchmarks:benchmark` (JMH via kotlinx-benchmark: 5 warm-up and 10
measurement iterations of 1 s, average time per operation). Lower is better.

| Benchmark | What it measures | Time (ms/op) |
|---|---|---|
| `parseLargeDocument` | One-shot parse of a 73 KB document (headings, lists, code, tables, quotes, footnotes) | 1.92 ± 0.18 |
| `streamTokensIncrementally` | 2,000 tokens (4.2 KB) appended one at a time with `IncrementalMarkdownParser`, a document produced after each, then `finish()` | 6.32 ± 0.55 |
| `streamTokensAfterLongDocument` | `setText` of the 73 KB document, then the same 2,000 tokens appended | 11.82 ± 1.64 |
| `streamTokensWithFullReparse` | The same 2,000 tokens, re-parsing the whole text with `MarkdownParser` after each one | 110.03 ± 7.49 |

Observations:

- Incremental streaming costs about 3 µs per token, about 17 times less than re-parsing the whole
  text after each token, which is what a renderer without incremental parsing does.
- Streaming after a 73 KB prefix costs about the same per token once the prefix itself is parsed
  (11.8 ms total, of which the 73 KB `setText` is roughly one full parse): the work per update is
  proportional to the open end of the document, not to its length.

Environment: OpenJDK 21.0.13, Linux, 4 vCPUs (Intel Xeon @ 2.10 GHz, shared cloud machine).
Absolute numbers vary between machines; compare runs on the same machine.

These are JVM numbers. Android performance is measured separately with Macrobenchmark on a device.
