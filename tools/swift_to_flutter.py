#!/usr/bin/env python3
"""Translate the Swift source tree into a reviewable Dart starting point.

SwiftUI view declarations and common layout primitives are converted to actual
Flutter widgets. SwiftData, property wrappers, and Apple frameworks still need
platform-specific designs; unsupported constructs are kept as TODO comments so
the pass never silently loses behavior.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path


DEFAULT_ROOTS = ("Piru", "Shared", "PiruWidget", "PiruLiveActivityExtension")
DEFAULT_EXCLUDES = ("Tests", "Preview Content")
UI_IMPORT = "import 'package:flutter/material.dart';"


def _generic_type(value: str) -> str:
    value = re.sub(r"\bAny\b", "dynamic", value)
    value = re.sub(r"\bAnyObject\b", "Object", value)
    value = re.sub(r"\bString\b", "String", value)
    value = re.sub(r"\bInt64?\b", "int", value)
    value = re.sub(r"\bUInt64?\b", "int", value)
    value = re.sub(r"\bDouble\b|\bFloat\b", "double", value)
    value = re.sub(r"\bBool\b", "bool", value)
    value = re.sub(r"\bDate\b", "DateTime", value)
    value = re.sub(r"\bUUID\b", "String", value)
    value = re.sub(r"\[([^:\]]+):\s*([^\]]+)\]", r"Map<\1, \2>", value)
    value = re.sub(r"\[([^\]]+)\]", r"List<\1>", value)
    return value


def _translate_line(line: str) -> str:
    indent = line[: len(line) - len(line.lstrip())]
    text = line.strip()
    if not text:
        return ""
    has_optional_binding = bool(re.search(r"\b(?:if|guard)\s+let\b", text))
    if text.startswith("import "):
        return f"{indent}// {text} (Swift framework import; add the Flutter equivalent)"
    if text.startswith("@") and not (
        text.startswith("@State ")
        or text.startswith("@Binding ")
        or text.startswith("@Environment")
    ):
        return f"{indent}// {text} (Swift annotation requires a Flutter design)"
    if text.startswith(("///", "//")):
        return line

    ui = _translate_ui_line(indent, text)
    if ui is not None:
        return ui

    text = re.sub(r"^(public|private|internal|fileprivate|open|final)\s+", "", text)
    text = re.sub(r"^(nonisolated|override|static)\s+", "", text)
    text = re.sub(r"^(struct|class)\s+(\w+)", r"class \2", text)
    text = re.sub(r"\benum\s+(\w+)", r"enum \1", text)
    text = re.sub(r"\b(let|var)\s+(\w+)\s*:", r"final \2:", text)
    text = re.sub(r"\b(let|var)\s+(\w+)\s*=", r"final \2 =", text)
    text = text.replace("nil", "null")
    text = text.replace("true", "true").replace("false", "false")
    text = text.replace("&&", "&&").replace("||", "||")

    # Swift's trailing return syntax is the most common mechanically
    # translatable function form.
    text = re.sub(
        r"func\s+(\w+)\s*\(([^)]*)\)\s*->\s*([^{]+)\s*\{?",
        lambda m: f"{_generic_type(m.group(3).strip())} {m.group(1)}({m.group(2)}) {{",
        text,
    )
    text = re.sub(r"\bfunc\s+(\w+)\s*\(([^)]*)\)\s*\{?", r"void \1(\2) {", text)
    text = re.sub(r"\bthrows\b|\basync\b|\bawait\b", "", text)
    text = re.sub(r"\bguard\b|\bwhere\b", "/* unsupported Swift control flow */", text)
    text = re.sub(r"\belse\s+if\b", "else if", text)
    text = re.sub(r"\b(String|Int64?|UInt64?|Double|Float|Bool|Date|UUID|AnyObject|Any)\b", lambda m: _generic_type(m.group(1)), text)
    text = re.sub(r"\b([A-Za-z_]\w*)\?\b", r"\1?", text)
    text = re.sub(r"\b([A-Za-z_]\w*)\.now\b", r"DateTime.now()", text)
    text = re.sub(r"\b([A-Za-z_]\w*)\.lowercased\(\)", r"\1.toLowerCase()", text)
    text = re.sub(r"\b([A-Za-z_]\w*)\.uppercased\(\)", r"\1.toUpperCase()", text)
    text = text.replace("String(localized:", "/* localized */ String(")
    text = text.replace("...", "/* range */")

    if text.startswith("switch "):
        text = "// TODO Flutter: replace Swift switch with Dart switch\n" + text
    if has_optional_binding:
        text = "// TODO Flutter: rewrite Swift optional binding: " + text
    return indent + text


def _translate_ui_line(indent: str, text: str) -> str | None:
    """Translate SwiftUI's structural syntax into Flutter widget syntax."""
    view_match = re.match(r"(?:struct|class)\s+(\w+)\s*:\s*View\s*\{?", text)
    if view_match:
        return f"{indent}class {view_match.group(1)} extends StatelessWidget {{"
    if re.match(r"var\s+body\s*:\s*some\s+View\s*\{?", text):
        return f"{indent}  @override\n{indent}  Widget build(BuildContext context) {{"
    if text.startswith("@State ") or text.startswith("@Binding ") or text.startswith("@Environment"):
        declaration = re.sub(r"^@\w+(?:\([^)]*\))?\s+", "", text)
        declaration = re.sub(r"\b(private|public|internal)\s+", "", declaration)
        declaration = re.sub(r"\bvar\s+(\w+)", r"dynamic \1", declaration)
        return f"{indent}// TODO Flutter state/input wiring: {declaration}"
    if text.startswith("VStack"):
        return f"{indent}Column(children: ["
    if text.startswith("HStack"):
        return f"{indent}Row(children: ["
    if text.startswith("ZStack"):
        return f"{indent}Stack(children: ["
    if text.startswith("LazyVStack") or text.startswith("LazyHStack"):
        widget = "Column" if "VStack" in text else "Row"
        return f"{indent}{widget}(children: [ // TODO Flutter: lazy builder"
    if re.match(r"ScrollView(?:<[^>]+>)?\s*\{?", text):
        return f"{indent}SingleChildScrollView(child: Column(children: ["
    if text.startswith("List"):
        return f"{indent}ListView(children: ["
    if text.startswith("NavigationStack") or text.startswith("NavigationView"):
        return f"{indent}Scaffold(body: Navigator("
    if text.startswith("TabView"):
        return f"{indent}Scaffold(body: // TODO Flutter: map SwiftUI tabs"
    if text.startswith("Form"):
        return f"{indent}Form(child: Column(children: ["
    if text.startswith("Section"):
        return f"{indent}Column(crossAxisAlignment: CrossAxisAlignment.start, children: ["
    if text.startswith("Group"):
        return f"{indent}Column(children: ["
    if text.startswith("Text("):
        return f"{indent}{text}"
    if text.startswith("Image(systemName:"):
        icon = re.search(r'"([^"]+)"', text)
        return f"{indent}Icon(Icons.help_outline), // Swift icon: {icon.group(1) if icon else 'unknown'}"
    if text.startswith("Divider"):
        return f"{indent}const Divider(),"
    if text.startswith("Spacer"):
        return f"{indent}const Spacer(),"
    if text.startswith("ProgressView"):
        return f"{indent}const CircularProgressIndicator(),"
    if text.startswith("Button("):
        return f"{indent}// TODO Flutter: convert SwiftUI Button closure\n{indent}TextButton(onPressed: () {{}}, child: const Text('Button')),"
    if text.startswith("Toggle("):
        return f"{indent}// TODO Flutter: bind Toggle state\n{indent}const SizedBox.shrink(),"
    if text.startswith("EmptyView"):
        return f"{indent}const SizedBox.shrink(),"
    if text in ("}", "},"):
        return f"{indent}]),"
    if text.startswith("."):
        return f"{indent}// TODO Flutter modifier: {text}"
    return None


def translate_source(source: str, relative_path: str) -> str:
    is_ui = bool(re.search(r"\b(?:import\s+SwiftUI|:\s*View\b|some\s+View\b)", source))
    header = [
        "// GENERATED by tools/swift_to_flutter.py.",
        f"// Source: {relative_path}",
        "// Review TODO markers before using this in a Flutter target.",
        "",
    ]
    if is_ui:
        header.extend([UI_IMPORT, ""])
    return "\n".join(header + [_translate_line(line) for line in source.splitlines()]) + "\n"


def swift_files(roots: list[Path], excludes: tuple[str, ...]) -> list[Path]:
    files: list[Path] = []
    for root in roots:
        if root.is_file() and root.suffix == ".swift":
            files.append(root)
        elif root.is_dir():
            files.extend(path for path in root.rglob("*.swift") if not any(part in excludes for part in path.parts))
    return sorted(set(files))


def translate_tree(repo: Path, output: Path, roots: list[str], excludes: tuple[str, ...]) -> int:
    files = swift_files([repo / root for root in roots], excludes)
    for source_path in files:
        relative = source_path.relative_to(repo)
        target = output / relative.with_suffix(".dart")
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(translate_source(source_path.read_text(), str(relative)), encoding="utf-8")
    return len(files)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=Path.cwd(), help="Swift repository root")
    parser.add_argument("--output", type=Path, required=True, help="Directory for generated Dart files")
    parser.add_argument("--root", action="append", dest="roots", help="Source root (repeatable)")
    parser.add_argument("--exclude", action="append", default=[], help="Directory name to skip")
    args = parser.parse_args()
    roots = args.roots or list(DEFAULT_ROOTS)
    excludes = tuple(DEFAULT_EXCLUDES) + tuple(args.exclude)
    count = translate_tree(args.input.resolve(), args.output.resolve(), roots, excludes)
    print(f"Translated {count} Swift files to {args.output.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
