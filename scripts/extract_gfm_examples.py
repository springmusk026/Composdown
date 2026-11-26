#!/usr/bin/env python3
"""Extracts the GitHub Flavored Markdown extension examples into a spec.json-style file.

Reads cmark-gfm's `test/spec.txt` (the GFM specification) and `test/extensions.txt` (cmark-gfm's
extension tests) and writes the examples that exercise GFM extensions:

- From spec.txt: examples marked `table`, `strikethrough` and `autolink`. The remaining examples
  describe CommonMark 0.29, which Composdown replaces with the CommonMark 0.31.2 suite.
- From extensions.txt: every example except the "HTML tag filter" section, which describes HTML
  rendering. Composdown never renders HTML, so tag filtering does not apply.

In both files `→` stands for a tab. An expected output of `<IGNORE>` means the input must only parse
without errors.

Usage:
    python3 scripts/extract_gfm_examples.py spec.txt extensions.txt > composdown-core/spec/gfm-0.29.0.gfm.14.json
"""

import json
import re
import sys

FENCE = "`" * 32
SPEC_EXTENSIONS = {"table", "strikethrough", "autolink"}
EXCLUDED_SECTIONS = {"HTML tag filter"}


def examples(path):
    section = None
    lines = open(path, encoding="utf-8").read().split("\n")
    i = 0
    while i < len(lines):
        line = lines[i]
        heading = re.match(r"^#{1,6} +(.*)$", line)
        if heading:
            section = heading.group(1).strip()
        if line.startswith(FENCE + " example"):
            kind = line[len(FENCE + " example"):].strip()
            i += 1
            markdown = []
            while lines[i] != ".":
                markdown.append(lines[i])
                i += 1
            i += 1
            html = []
            while lines[i] != FENCE:
                html.append(lines[i])
                i += 1
            yield {
                "kind": kind,
                "section": section,
                "markdown": "\n".join(markdown).replace("→", "\t") + "\n",
                "html": "\n".join(html).replace("→", "\t") + "\n",
            }
        i += 1


def main():
    spec_path, extensions_path = sys.argv[1], sys.argv[2]
    selected = [e for e in examples(spec_path) if e["kind"] in SPEC_EXTENSIONS]
    selected += [e for e in examples(extensions_path) if e["section"] not in EXCLUDED_SECTIONS]
    output = []
    for number, example in enumerate(selected, start=1):
        output.append(
            {
                "markdown": example["markdown"],
                "html": example["html"],
                "example": number,
                "section": example["section"],
            }
        )
    json.dump(output, sys.stdout, ensure_ascii=False, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
