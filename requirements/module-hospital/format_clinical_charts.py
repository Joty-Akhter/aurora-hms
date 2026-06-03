#!/usr/bin/env python3
"""
Convert pasted clinical chart export (concatenated columns) into tab-separated columns.

Usage:
  python3 format_clinical_charts.py [INPUT.txt] [-o OUTPUT]
  python3 format_clinical_charts.py -i   # overwrite INPUT in place

Columns (tab-separated):
  section, raw_prefix, fee_block, investigation_name, code, ref, bill_group, rate
"""
from __future__ import annotations

import argparse
import csv
import io
import re
import sys
from pathlib import Path

# Trailing: 4-digit service code + ref (comma form x,xxx or short digits) + rest (dept + rate at end)
END_ROW = re.compile(
    r"(\d{4})((?:\d,\d{3})|\d{1,4})\s+(.+)$"
)
# Fee / list-price block immediately before investigation name starts with a letter
MID_SPLIT = re.compile(
    r"^(.*)((?:0\s+\d[\d, ]+|\d{2}\s+\d[\d,]+))([A-Za-z].*)$"
)
SKIP_PREFIXES = (
    "Served by",
    "RepoName Department SubDeptName HoGroupSubSubDeptNameAfRate",
    "Printed Date",
    "Please Log In",
)
SKIP_EXACT = frozenset({"Fee", "Fix", "Les", "Clinical Chart"})
HEADER_LINE = "SL Code Investigation Name Rate Ref"

# Line fragments from wrapped investigation names (not department sections)
CONTINUATION_WORDS = frozenset(
    {
        "Conventional",
        "Drink",
        "CUS",
        "Charge.",
        "Quadrant)",
        "Vessels",
        "Doppler",
        "Branches",
        "Grapt1",
        "Grapt2",
    }
)


def is_noise_line(s: str) -> bool:
    t = s.strip()
    if not t:
        return True
    if t in SKIP_EXACT:
        return True
    for p in SKIP_PREFIXES:
        if t.startswith(p):
            return True
    if "Powered by" in t and "Big Bang" in t:
        return True
    return False


def split_dept_rate(tail: str) -> tuple[str, str]:
    """Split 'Biochemistry Test750' or 'OPD200' into (bill group / label, rate)."""
    tail = tail.strip()
    m = re.search(r"(\d[\d,]*)$", tail)
    if not m:
        return tail, ""
    rate = m.group(1)
    dept = tail[: m.start()].strip()
    return dept, rate


def parse_merged_line(line: str) -> tuple[str, str, str, str, str, str, str, str] | None:
    """Return 8 fields or None if not a data row."""
    m_end = END_ROW.search(line)
    if not m_end:
        return None
    code, ref, tail = m_end.group(1), m_end.group(2), m_end.group(3)
    dept, rate = split_dept_rate(tail)
    prefix_and_name = line[: m_end.start()]
    m_mid = MID_SPLIT.match(prefix_and_name)
    if not m_mid:
        return None
    raw_prefix, fee_block, inv_name = m_mid.group(1), m_mid.group(2), m_mid.group(3)
    inv_name = re.sub(r"\s+", " ", inv_name.strip())
    return ("", raw_prefix, fee_block, inv_name, code, ref, dept, rate)


def iter_records(lines: list[str]):
    """Yield (section_label, merged_data_line) preserving section headers."""
    buf: list[str] = []
    section = ""
    for raw in lines:
        line = raw.strip()
        if not line:
            continue
        sec = classify_section(line)
        if sec is not None:
            section = sec
            continue
        if END_ROW.search(line):
            if buf:
                yield section, " ".join(buf + [line])
                buf = []
            else:
                yield section, line
        else:
            buf.append(line)
    if buf:
        yield section, " ".join(buf)


def classify_section(line: str) -> str | None:
    """Heuristic: short non-data lines printed as section headers (e.g. Canteen, DENTAL)."""
    s = line.strip()
    if s in SKIP_EXACT:
        return None
    if s in CONTINUATION_WORDS:
        return None
    if END_ROW.search(s):
        return None
    if len(s) > 80:
        return None
    if re.search(r"\d{4}", s):
        return None
    if "(" in s or ")" in s:
        return None
    # Long single Titlecase words are usually wrapped name fragments (e.g. Conventional)
    if re.match(r"^[A-Z][a-z]+$", s) and len(s) > 10:
        return None
    return s


def preprocess(lines: list[str]) -> list[str]:
    """Drop page headers/repeated column titles."""
    out: list[str] = []
    skip_block = False
    for raw in lines:
        line = raw.rstrip("\n\r")
        t = line.strip()
        if not t:
            continue
        if t == HEADER_LINE:
            skip_block = True
            continue
        if skip_block and t in SKIP_EXACT:
            continue
        if skip_block and t.startswith("RepoName Department"):
            skip_block = False
            continue
        if is_noise_line(line):
            continue
        out.append(line)
    return out


def format_file(text: str) -> str:
    lines = preprocess(text.splitlines())

    buf = io.StringIO()
    w = csv.writer(buf, delimiter="\t", lineterminator="\n", quoting=csv.QUOTE_MINIMAL)
    w.writerow(
        [
            "section",
            "repo_prefix",
            "fee_block",
            "investigation_name",
            "code",
            "ref",
            "bill_group",
            "rate",
        ]
    )

    for current_section, line in iter_records(lines):
        row = parse_merged_line(line)
        if row is None:
            w.writerow([current_section, line, "", "", "", "", "", ""])
            continue
        _, rp, fb, inv, code, ref, dept, rate = row
        w.writerow([current_section, rp, fb, inv, code, ref, dept, rate])

    return buf.getvalue()


def main() -> int:
    ap = argparse.ArgumentParser(description="Format clinical-charts export as TSV.")
    ap.add_argument(
        "input",
        nargs="?",
        default="clinical-charts.txt",
        help="Source text file, or '-' for stdin (default: clinical-charts.txt in cwd)",
    )
    ap.add_argument("-o", "--output", help="Write to this file (default: stdout or -i target)")
    ap.add_argument(
        "-i",
        "--in-place",
        action="store_true",
        help="Overwrite input file with TSV result",
    )
    args = ap.parse_args()

    if args.input == "-":
        raw = sys.stdin.read()
        if not raw.strip():
            print("Error: stdin empty", file=sys.stderr)
            return 1
        out_text = format_file(raw)
        if args.in_place:
            print("Error: --in-place cannot be used with stdin", file=sys.stderr)
            return 1
        if args.output:
            Path(args.output).write_text(out_text, encoding="utf-8")
            print(f"Wrote {len(out_text)} chars to {args.output}", file=sys.stderr)
        else:
            sys.stdout.write(out_text)
        return 0

    inp = Path(args.input)
    if not inp.is_file():
        print(f"Error: not found: {inp}", file=sys.stderr)
        return 1
    raw = inp.read_text(encoding="utf-8-sig", errors="replace")
    if not raw.strip():
        print(
            "Error: input file is empty. Save your editor buffer to disk, then re-run.",
            file=sys.stderr,
        )
        return 1

    out_text = format_file(raw)

    if args.in_place:
        outp = inp
    elif args.output:
        outp = Path(args.output)
    else:
        sys.stdout.write(out_text)
        return 0

    outp.write_text(out_text, encoding="utf-8")
    print(f"Wrote {len(out_text)} chars to {outp}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
