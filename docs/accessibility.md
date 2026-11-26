# Accessibility

Composdown renders Markdown as native Compose text and layouts, so content is available to TalkBack on
Android, VoiceOver on iOS, and the accessibility APIs of desktop platforms, and it respects the user's
font size settings.

- [What Composdown provides](#what-composdown-provides)
- [Right-to-left and bidirectional text](#right-to-left-and-bidirectional-text)
- [Responsibilities of the application](#responsibilities-of-the-application)
- [Known limitations](#known-limitations)
- [Manual verification checklist](#manual-verification-checklist)

## What Composdown provides

| Content | Behavior |
|---|---|
| Headings | Marked as headings, so screen reader users can navigate by heading |
| Links | Focusable and activatable; announced as links |
| Task list items | Expose checked or unchecked state |
| Bullets | Decorative: drawn, but hidden from assistive technologies |
| Ordered list numbers | Read as text |
| Tables | Expose collection semantics: number of rows and columns, and each cell's position |
| Images | Content description from the image's Markdown description (`![description](url)`), through `CoilImageRenderer` or any renderer following the `ImageRenderer` contract |
| Alerts | The title (for example "Warning") is read before the content |
| Footnote references | Read as their number; the definitions follow at the end of the document |
| Text size | All text styles use `sp` units from the theme and scale with system font size |
| Selection | Text can be selected and copied, across blocks |

Rendering is verified by Compose UI tests that assert the semantics of headings, links, task items and
tables.

## Right-to-left and bidirectional text

Each block is laid out in the direction of its own first strongly directional character. A Hebrew or
Arabic paragraph is right-aligned, and its list bullets, quote bars and table columns are placed on the
right, even when the application's layout direction is left to right, and vice versa. Blocks without
directional text (only digits or punctuation) follow the surrounding direction. Text within a block
follows the Unicode bidirectional algorithm.

## Responsibilities of the application

- **Color contrast.** The default colors come from your Material color scheme. If you override colors,
  verify that text, links and code meet WCAG contrast (4.5:1 for body text).
- **Custom components.** Components you supply through `ComposdownComponents` must provide their own
  semantics. Use `Modifier.semantics { heading() }` for heading-like content, and give interactive
  elements (such as a copy button) a label.
- **Images.** Encourage meaningful image descriptions in your content. Images without a description
  are announced without a label.
- **Link handling.** Links whose scheme is not opened by default (see [Rendering](rendering.md#links))
  remain focusable but do nothing when activated. Provide `onLinkClick` if your content uses such links.
- **Live content.** A message that is still being streamed changes continuously. Consider announcing
  only the completed message (for example with a live region on a container that you update when
  `ComposdownState.isFinished` becomes `true`) rather than every update.

## Known limitations

- The checked and unchecked state descriptions of task items are in English.
- Math is exposed as its TeX source, which screen readers read character by character. A `mathBlock`
  component that typesets formulas should also provide a spoken description.
- Raw HTML is exposed as its source text.

## Manual verification checklist

Automated tests cannot judge the experience of a screen reader user. Before each release, and when
changing rendering, go through the [sample app](../sample)'s Showcase screen with a screen reader on
each platform:

**TalkBack (Android)** and **VoiceOver (iOS)**:

- [ ] Swiping through the document reads every block in order, including the footnotes at the end.
- [ ] The headings rotor or navigation (VoiceOver rotor "Headings", TalkBack "Headings" reading
      control) jumps between headings.
- [ ] Each link is reached as a separate element, announced as a link, and opens when activated.
- [ ] Task items announce "Checked" or "Not checked".
- [ ] Bullets are not announced; ordered list numbers are.
- [ ] Table cells announce their position, and the table announces its size.
- [ ] Images announce their description.
- [ ] Alerts announce their type before their content.
- [ ] With the largest system font size, no text is clipped and code and tables scroll horizontally.

**Right to left**:

- [ ] In the Chat screen, the "Write a greeting in Hebrew and Arabic" response shows its Hebrew and
      Arabic blocks aligned right, with bullets on the right, next to left-aligned English blocks.
- [ ] The same holds with the device language set to Arabic or Hebrew (mirrored application layout).

**Keyboard (Desktop)**:

- [ ] Tab moves focus through links in order, and Enter opens the focused link.
- [ ] Text can be selected with the mouse and copied with the platform shortcut.
