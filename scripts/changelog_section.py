#!/usr/bin/env python3
"""Prints the CHANGELOG.md section of a version, for release notes.

Usage: python3 scripts/changelog_section.py 0.1.0
Exits with an error if the changelog has no section for the version.
"""
import pathlib
import re
import sys


def main() -> None:
    version = sys.argv[1]
    changelog = (pathlib.Path(__file__).resolve().parent.parent / "CHANGELOG.md").read_text(encoding="utf-8")
    match = re.search(rf"^## \[{re.escape(version)}\][^\n]*\n(.*?)(?=^## \[|^\[[^\]]+\]: )", changelog, re.M | re.S)
    if not match:
        sys.exit(f"CHANGELOG.md has no section for {version}")
    print(match.group(1).strip())


if __name__ == "__main__":
    main()
