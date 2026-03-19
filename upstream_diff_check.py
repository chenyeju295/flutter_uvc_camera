#!/usr/bin/env python3
"""
Quick drift checker between local plugin core and a reference AndroidUSBCamera copy.

Usage:
  python3 upstream_diff_check.py
  python3 upstream_diff_check.py --base /path/to/repo
"""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


def sha1(path: Path) -> str:
    h = hashlib.sha1()
    with path.open("rb") as f:
        while True:
            chunk = f.read(1024 * 1024)
            if not chunk:
                break
            h.update(chunk)
    return h.hexdigest()


def collect_files(root: Path) -> dict[str, Path]:
    result: dict[str, Path] = {}
    for p in root.rglob("*"):
        if p.is_file():
            result[p.relative_to(root).as_posix()] = p
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", type=Path, default=Path(__file__).resolve().parent)
    args = parser.parse_args()

    base = args.base.resolve()
    local_dir = base / "android" / "src" / "main" / "java" / "com" / "jiangdg" / "ausbc"
    ref_dir = (
        base
        / "android"
        / "AndroidUSBCamera"
        / "libausbc"
        / "src"
        / "main"
        / "java"
        / "com"
        / "jiangdg"
        / "ausbc"
    )

    if not local_dir.exists():
        print(f"[ERROR] Local dir missing: {local_dir}")
        return 2
    if not ref_dir.exists():
        print(f"[ERROR] Reference dir missing: {ref_dir}")
        return 2

    local = collect_files(local_dir)
    ref = collect_files(ref_dir)

    local_keys = set(local.keys())
    ref_keys = set(ref.keys())
    common = sorted(local_keys & ref_keys)
    only_local = sorted(local_keys - ref_keys)
    only_ref = sorted(ref_keys - local_keys)

    changed: list[str] = []
    for rel in common:
        if sha1(local[rel]) != sha1(ref[rel]):
            changed.append(rel)

    print("== Upstream Drift Check ==")
    print(f"local files      : {len(local_keys)}")
    print(f"reference files  : {len(ref_keys)}")
    print(f"common files     : {len(common)}")
    print(f"content changed  : {len(changed)}")
    print(f"only local       : {len(only_local)}")
    print(f"only reference   : {len(only_ref)}")

    if changed:
        print("\n[Changed files]")
        for rel in changed:
            print(f"  - {rel}")
    if only_local:
        print("\n[Only local]")
        for rel in only_local:
            print(f"  - {rel}")
    if only_ref:
        print("\n[Only reference]")
        for rel in only_ref:
            print(f"  - {rel}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

