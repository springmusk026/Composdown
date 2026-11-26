# Security Policy

## Supported versions

Composdown is pre-1.0. Security fixes are made on the latest release only.

| Version | Supported |
|---|---|
| Latest 0.x release | Yes |
| Older releases | No |

## Reporting a vulnerability

Please **do not** report security vulnerabilities through public GitHub issues, discussions or pull
requests.

Report them privately through GitHub's
[private vulnerability reporting](https://github.com/springmusk026/Composdown/security/advisories/new)
form (**Security** tab, then **Report a vulnerability**).

Please include:

- A description of the issue and its impact.
- The Markdown input or code needed to reproduce it.
- Affected versions and platforms.

You can expect an acknowledgement within 7 days. We will keep you informed while we work on a fix and
credit you in the advisory unless you prefer to remain anonymous.

## Scope

Composdown renders untrusted text (for example, output from language models). Issues of particular
interest include:

- Input that makes the parser hang, use excessive memory, or crash (denial of service).
- Raw HTML or link content that is interpreted or executed rather than displayed.
- Link handling that opens a URL without going through the application's link handler.
