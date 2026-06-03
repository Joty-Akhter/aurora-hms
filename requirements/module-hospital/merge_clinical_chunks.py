#!/usr/bin/env python3
"""Merge read_file-style chunks (line prefixes like '  12|...') into clinical-charts.txt."""
from __future__ import annotations

import glob
import re
from pathlib import Path

SKIP_LINE = re.compile(r"^\.\.\. \d+ lines not shown \.\.\.$")
LINE_PREFIX = re.compile(r"^\s*\d+\|(.*)$")


def strip_chunk(text: str) -> str:
    out: list[str] = []
    for line in text.splitlines():
        m = LINE_PREFIX.match(line)
        if not m:
            continue
        body = m.group(1)
        if SKIP_LINE.fullmatch(body.strip()):
            continue
        out.append(body)
    return "\n".join(out) + ("\n" if out else "")


def main() -> None:
    base = Path(__file__).resolve().parent
    parts = sorted(glob.glob(str(base / "clinical_chunks" / "chunk_*.txt")))
    if not parts:
        raise SystemExit("No clinical_chunks/chunk_*.txt files found")
    merged = "".join(strip_chunk(Path(p).read_text(encoding="utf-8")) for p in parts)
    out_path = base / "clinical-charts.txt"
    out_path.write_text(merged, encoding="utf-8")
    n = len(merged.encode("utf-8"))
    print(n)


if __name__ == "__main__":
    main()
