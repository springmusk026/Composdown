# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Project structure: `composdown-core` and `composdown-compose` Kotlin Multiplatform modules targeting
  Android, iOS and Desktop (JVM).
- Build tooling: Gradle version catalog, convention plugins, Spotless (ktlint), detekt and binary
  compatibility validation (JVM and KLib).
- Continuous integration on Linux and macOS.
- `MarkdownParser`, a pure Kotlin CommonMark 0.31.2 parser producing an immutable document model
  (`MarkdownDocument`, `Block`, `Inline`) with structural equality. The 652 specification examples run
  as tests on every platform.
- `plainText()` to extract the text of inline content, for example for image descriptions.
- GitHub Flavored Markdown, enabled by default and selectable per extension with
  `MarkdownExtension`: tables, task lists, strikethrough, extended autolinks (`www.`, `http(s)://`,
  `ftp://`, email, `mailto:` and `xmpp:`) and footnotes. The GFM specification's extension examples
  and cmark-gfm's extension tests run on every platform.
- GitHub alerts (`> [!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, `[!CAUTION]`) as an opt-in
  extension.
- Protection against hostile input: parsing runs in linear time on known pathological inputs, and
  block quotes, lists and inline elements nested deeper than 100 levels are flattened instead of
  exhausting the stack.

[Unreleased]: https://github.com/springmusk026/Composdown/commits/main
