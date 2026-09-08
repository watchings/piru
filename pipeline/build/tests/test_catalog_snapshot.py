"""Validate the deterministic catalog artifact consumed by Web/Android bootstrap."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
SNAPSHOT = ROOT / "data" / "snapshots" / "substances.json"
MANIFEST = ROOT / "Piru" / "Data" / "manifest.json"


def test_snapshot_matches_manifest():
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    catalog = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    assert len(catalog) == manifest["substance_count"]
    assert len({item["name"] for item in catalog}) == len(catalog)
    assert all(item["name"] for item in catalog)
    assert all(isinstance(item.get("aliases", []), list) for item in catalog)


def test_snapshot_records_have_search_fields():
    catalog = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    for item in catalog:
        assert {"name", "display_name", "aliases", "category"} <= item.keys()
        assert isinstance(item["display_name"], str)
        assert isinstance(item["category"], str)


if __name__ == "__main__":
    test_snapshot_matches_manifest()
    test_snapshot_records_have_search_fields()
    print("Catalog snapshot tests passed")
