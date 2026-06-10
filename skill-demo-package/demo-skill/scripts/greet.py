#!/usr/bin/env python3
"""Generate a tiny greeting artifact for demo-skill."""

from __future__ import annotations

import argparse


def build_message(name: str, style: str) -> str:
    if style == "concise":
        return f"Hello, {name}."
    return f"Hello, {name}. This greeting came from the demo-skill helper script."


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate a demo greeting.")
    parser.add_argument("--name", default="world", help="Name to greet.")
    parser.add_argument(
        "--style",
        choices=("friendly", "concise"),
        default="friendly",
        help="Greeting style.",
    )
    args = parser.parse_args()
    print(build_message(args.name, args.style))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
