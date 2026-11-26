#!/usr/bin/env python3
"""Assembles the frames written by `./gradlew :tools:screenshots:run` into docs/images/streaming.gif.

Requires Pillow. Usage: python3 scripts/assemble_gif.py [frames directory] [output file]
"""
import pathlib
import sys

from PIL import Image

FRAME_MS = 70  # about 43 tokens per second at 3 tokens per frame: a fast language model
HOLD_FIRST_MS = 900
HOLD_LAST_MS = 3000


def main() -> None:
    root = pathlib.Path(__file__).resolve().parent.parent
    frames_dir = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else root / "tools/screenshots/build/frames"
    output = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else root / "docs/images/streaming.gif"
    paths = sorted(frames_dir.glob("frame_*.png"))
    if not paths:
        sys.exit(f"No frames in {frames_dir}; run ./gradlew :tools:screenshots:run first")

    # One palette for every frame, from the final frame, which contains every color used.
    last = Image.open(paths[-1]).convert("RGB")
    palette = last.quantize(colors=256, method=Image.Quantize.FASTOCTREE)
    frames = [Image.open(p).convert("RGB").quantize(palette=palette, dither=Image.Dither.NONE) for p in paths]
    durations = [FRAME_MS] * len(frames)
    durations[0] = HOLD_FIRST_MS
    durations[-1] = HOLD_LAST_MS
    frames[0].save(output, save_all=True, append_images=frames[1:], duration=durations, loop=0, optimize=True)
    print(f"Wrote {output} ({len(frames)} frames, {output.stat().st_size // 1024} KB)")


if __name__ == "__main__":
    main()
