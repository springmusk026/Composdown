# Specification test data

| File | Source | License |
|---|---|---|
| `commonmark-0.31.2.json` | [CommonMark Spec 0.31.2](https://spec.commonmark.org/0.31.2/spec.json) examples | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |

Copyright (C) 2014–2024 John MacFarlane.

These files are used only by the test suite and are not included in published artifacts. At build
time, `GenerateSpecTestsTask` (see `build-logic`) turns each file into a `kotlin.test` class in
`commonTest`, so the specification examples run on every platform.
