# SwiftUI → Web/Android UI translation workflow

`pipeline/ui_translate.py` inventories the complete `Piru/Views` tree and emits
deterministic, reviewable Web `.tsx` and Android `.kt` stubs for every Swift
source file containing a SwiftUI `View`.

Run an inventory without writing files:

```bash
python3 pipeline/ui_translate.py --check
```

Generate both target trees into a temporary directory:

```bash
python3 pipeline/ui_translate.py --output /tmp/piru-ui-translation
```

The output is deliberately not production UI. Every generated file contains
explicit TODOs for state, navigation, persistence, localization, accessibility,
and platform integrations. Engineers replace each stub with a native component,
then compare it against the corresponding iOS screen using screenshot and
interaction tests. The manifest is the review queue and prevents a generated
stub from being mistaken for a completed 1:1 port.
