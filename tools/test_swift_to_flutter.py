import tempfile
import unittest
from pathlib import Path

from swift_to_flutter import translate_source, translate_tree


class TranslatorTests(unittest.TestCase):
    def test_common_swift_types_and_functions(self):
        result = translate_source(
            "import Foundation\nstruct Dose { let amount: Double\nfunc value() -> String { return \"mg\" } }",
            "Piru/Domain/Dose.swift",
        )
        self.assertIn("class Dose", result)
        self.assertIn("final amount: double", result)
        self.assertIn("String value()", result)
        self.assertIn("// import Foundation", result)

    def test_optional_binding_is_not_silently_lost(self):
        result = translate_source("if let value = value { print(value) }", "Example.swift")
        self.assertIn("TODO Flutter", result)

    def test_swiftui_views_become_flutter_widgets(self):
        result = translate_source(
            'import SwiftUI\nstruct Demo: View {\n  @State private var enabled = false\n'
            '  var body: some View {\n    VStack {\n      Text("Piru")\n      Button("Log") { }\n      Divider()\n    }\n  }\n}',
            "Piru/Views/Demo.swift",
        )
        self.assertIn("package:flutter/material.dart", result)
        self.assertIn("class Demo extends StatelessWidget", result)
        self.assertIn("Widget build(BuildContext context)", result)
        self.assertIn("Column(children: [", result)
        self.assertIn('Text("Piru")', result)
        self.assertIn("TextButton(onPressed", result)
        self.assertIn("TODO Flutter state/input wiring", result)

    def test_translates_a_tree_and_preserves_relative_paths(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "swift"
            (root / "Piru/Domain").mkdir(parents=True)
            (root / "Piru/Domain/Thing.swift").write_text("struct Thing {}", encoding="utf-8")
            output = Path(directory) / "dart"
            self.assertEqual(translate_tree(root, output, ["Piru"], ()), 1)
            self.assertTrue((output / "Piru/Domain/Thing.dart").exists())


if __name__ == "__main__":
    unittest.main()
