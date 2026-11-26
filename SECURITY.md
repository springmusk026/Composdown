# Security Policy

Composdown is built to render Markdown from sources that may not be trusted: user-generated content,
third-party documents, and the output of language models, which can be steered by whatever text the
model was given. This document describes the security model, the protections in place, what remains
the application's responsibility, and how to report a vulnerability.

- [Supported versions](#supported-versions)
- [Reporting a vulnerability](#reporting-a-vulnerability)
- [Security model](#security-model)
- [Application responsibilities](#application-responsibilities)
- [Scope](#scope)

## Supported versions

| Version | Supported |
|---|---|
| Latest 0.x release | Yes |
| Older releases | No |

Security fixes are released as a new patch or minor version and announced in a GitHub security
advisory.

## Reporting a vulnerability

**Do not report security vulnerabilities through public issues, discussions or pull requests.**

Report them privately with GitHub's
[private vulnerability reporting](https://github.com/springmusk026/Composdown/security/advisories/new)
(**Security** tab, then **Report a vulnerability**). Include:

- a description of the issue and its impact;
- the Markdown input or code that reproduces it;
- the affected versions and platforms.

| Step | Target |
|---|---|
| Acknowledgement | Within 7 days |
| Initial assessment | Within 14 days |
| Fix or mitigation | Depends on severity; we will agree on a disclosure date with you |

We credit reporters in the advisory unless they prefer to remain anonymous.

## Security model

Composdown treats every input as hostile. The defaults are chosen so that rendering untrusted Markdown
without further configuration is safe.

| Threat | Protection |
|---|---|
| **Markup and script injection** through raw HTML | HTML is never interpreted. HTML blocks are shown as source code and inline HTML as text. There is no WebView. |
| **Malicious links** (`javascript:`, `file:`, `intent:`, custom schemes that trigger app actions) | The default link handler opens only `http`, `https`, `mailto` and `tel`. Other links do nothing unless the application supplies `onLinkClick`. |
| **Tracking and IP disclosure** through remote images | Composdown loads no images by itself. `CoilImageRenderer` loads only HTTPS URLs by default, configurable with `canLoad`. |
| **Denial of service** through inputs that make parsing slow | The parser runs in linear time on known pathological inputs, verified by tests with time limits. |
| **Stack exhaustion** through deep nesting | Nesting deeper than 100 levels is flattened. |
| **Crashes** on malformed input | Every string is valid Markdown; the parser has no failure path for any input, verified by fuzzing in the streaming tests. |
| **Unsafe HTML output** from `toHtml()` | Raw HTML is escaped and `javascript:`, `vbscript:`, `file:` and non-image `data:` destinations are removed unless the application opts out (`HtmlOptions.CommonMark`). |
| **Invalid characters** | Null and invalid code points in character references are replaced with U+FFFD. |

The parser does no I/O, evaluates no expressions, and has no dependency other than the Kotlin standard
library and kotlinx-collections-immutable.

## Application responsibilities

Some decisions belong to the application:

- **Custom link handlers.** When you supply `onLinkClick`, Composdown passes every destination to it
  unfiltered. Validate the scheme and host before opening, and never pass a destination to an API that
  executes or loads it without checks.
- **Link text can deceive.** `[https://bank.example](https://evil.example)` shows one address and links
  to another. For sensitive contexts, consider confirming external navigation and showing the real
  destination.
- **Custom image renderers** decide which URLs are loaded. Loading arbitrary remote images reveals the
  reader's IP address and the time of reading to the image's server.
- **Custom components** that render HTML or math must apply their own sanitization.
- **Very large inputs.** Parsing is linear, but memory and layout grow with the input. Limit the size of
  untrusted documents you accept, and render long ones with `composdownItems` in a lazy list.

## Scope

In scope:

- Parser hangs, excessive memory use, or crashes on any input.
- Content that is interpreted or executed rather than displayed.
- Links opened, or images loaded, against the documented defaults.
- Any way for Markdown content to affect the application beyond its own rendering area.

Out of scope:

- Vulnerabilities in applications' own `onLinkClick`, `ImageRenderer` or component implementations.
- Vulnerabilities in third-party dependencies (Compose, Coil, Highlights) without a Composdown-specific
  impact; report those upstream.
- Content that is misleading but rendered as documented (see "Link text can deceive" above).
