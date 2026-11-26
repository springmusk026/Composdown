# Documentation images

Renders the images in [`docs/images`](../../docs/images) with Composdown itself, using Compose's
offscreen renderer (`ImageComposeScene`). No display, emulator or device is needed, and the bundled
Inter and JetBrains Mono fonts make the output identical on every machine.

| Image | Content |
|---|---|
| `overview.png` | The same document in light and dark themes |
| `streaming.gif` | A chat reply streamed token by token |
| `customization.png` | Default rendering next to a custom theme and components |
| `rtl.png` | Right-to-left and mixed-direction text |
| `logo.png` | The logo rasterized from `logo.svg`, for places that do not accept SVG |

Regenerate them after any visible rendering change:

```bash
./gradlew :tools:screenshots:run
```

```bash
python3 scripts/assemble_gif.py
```

The first command writes the PNG images and the animation frames (to `build/frames`); the second,
which needs [Pillow](https://pypi.org/project/pillow/), assembles the frames into the GIF. Review the
images before committing them.
