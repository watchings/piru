#!/usr/bin/env python3
"""Deterministic first-pass translator for SwiftUI source inventory.

This is intentionally a source-to-source aid, not a compiler. It preserves the
original file and emits reviewable target stubs with TODO markers for behavior,
platform APIs, and localization. Generated output is never used as production
code until manually corrected and tested.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "Piru" / "Views"
DEFAULT_OUT = ROOT / "pipeline" / "ui-translation-output"

VIEW_RE = re.compile(r"\bstruct\s+([A-Za-z_]\w*)\s*:\s*View\b")
STATE_RE = re.compile(r"@(?:State|Binding|Environment|Query|StateObject|ObservedObject)\b")


def swift_files(source: Path) -> list[Path]:
    return sorted(source.rglob("*.swift"))


def translate_body(name: str, source: str, target: str) -> str:
    states = len(STATE_RE.findall(source))
    if target == "web":
        return f"""import React from "react";

// Generated from SwiftUI {name}. Review behavior, navigation, localization,
// accessibility, persistence, and platform integrations before shipping.
export function {name}() {{
  // TODO: translate {states} SwiftUI state/environment dependencies.
  return <section data-piru-view="{name}"><h2>{name}</h2></section>;
}}
"""
    package = "app.piru.generated"
    return f"""package {package}

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

// Generated from SwiftUI {name}. Review behavior, navigation, localization,
// accessibility, persistence, and platform integrations before shipping.
@Composable
fun {name}() {{
    // TODO: translate {states} SwiftUI state/environment dependencies.
    Text("{name}")
}}
"""


def translate_file(path: Path, target: str) -> tuple[str, list[str]]:
    source = path.read_text(encoding="utf-8")
    views = VIEW_RE.findall(source)
    if not views:
        views = [path.stem]
    return "\n\n".join(translate_body(view, source, target) for view in views), views


def run(source: Path, output: Path, targets: tuple[str, ...]) -> dict:
    files = swift_files(source)
    manifest = {
        "translator": "pipeline/ui_translate.py",
        "source_root": str(source.relative_to(ROOT)),
        "generated": [],
        "policy": "Generated stubs require manual correction and platform tests.",
    }
    for file in files:
        relative = file.relative_to(source)
        for target in targets:
            extension = "tsx" if target == "web" else "kt"
            destination = output / target / relative.with_suffix(f".{extension}")
            destination.parent.mkdir(parents=True, exist_ok=True)
            content, views = translate_file(file, target)
            destination.write_text(content, encoding="utf-8")
            try:
                target_name = str(destination.relative_to(ROOT))
            except ValueError:
                target_name = str(destination)
            manifest["generated"].append({
                "source": str(file.relative_to(ROOT)),
                "target": target_name,
                "views": views,
                "status": "generated-stub",
            })
    output.mkdir(parents=True, exist_ok=True)
    (output / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUT)
    parser.add_argument("--target", choices=("web", "android"), action="append")
    parser.add_argument("--check", action="store_true", help="Only report source inventory.")
    args = parser.parse_args()
    source = args.source.resolve()
    targets = tuple(args.target or ("web", "android"))
    files = swift_files(source)
    if args.check:
        print(json.dumps({
            "source_root": str(source.relative_to(ROOT)),
            "swift_files": len(files),
            "targets": targets,
        }, ensure_ascii=False))
        return 0
    manifest = run(source, args.output.resolve(), targets)
    print(f"Generated {len(manifest['generated'])} reviewable stubs from {len(files)} Swift files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
