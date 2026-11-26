# Specification test data

| File | Source | License |
|---|---|---|
| `commonmark-0.31.2.json` | [CommonMark Spec 0.31.2](https://spec.commonmark.org/0.31.2/spec.json) examples | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) |
| `gfm-0.29.0.gfm.14.json` | GFM extension examples from [cmark-gfm 0.29.0.gfm.14](https://github.com/github/cmark-gfm/tree/0.29.0.gfm.14/test): `spec.txt` (GitHub Flavored Markdown Spec) and `extensions.txt` | spec.txt: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/); extensions.txt: [BSD-2-Clause](https://github.com/github/cmark-gfm/blob/0.29.0.gfm.14/COPYING) |

CommonMark Spec copyright (C) 2014–2024 John MacFarlane. GitHub Flavored Markdown Spec and cmark-gfm
tests copyright GitHub, Inc. and their contributors.

`gfm-0.29.0.gfm.14.json` is produced by `scripts/extract_gfm_examples.py`, which documents which
examples are included and why.

These files are used only by the test suite and are not included in published artifacts. At build
time, `GenerateSpecTestsTask` (see `build-logic`) turns each file into a `kotlin.test` class in
`commonTest`, so the specification examples run on every platform.
