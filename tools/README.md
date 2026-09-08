# Swift → Flutter starting-point translator

`swift_to_flutter.py` performs a dependency-free, project-wide syntax pass. It
keeps source paths, translates common Swift types and declarations, and emits
`TODO Flutter` markers for optional binding, SwiftUI, SwiftData, and other
Apple-specific behavior that must be redesigned rather than guessed.

From the repository root:

```bash
python3 tools/swift_to_flutter.py --output /tmp/piru-flutter/lib/generated
python3 -m unittest discover -s tools -p 'test_*.py'
```

The generated files are deliberately written outside the repository in the
example above. After reviewing a slice, copy only the Dart files that have
been adapted into a real Flutter package created with `flutter create`.

This tool does not claim that a regular-expression rewrite is a complete
SwiftUI-to-Flutter port. It makes the whole codebase searchable in Dart and
provides a repeatable first pass; persistence, navigation, charts, platform
services, and pharmacokinetic behavior still require native Flutter
implementations and tests.
