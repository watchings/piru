"""Offline smoke tests for the versioned cross-platform export contract."""

import json
import math
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
CONTRACT = ROOT / "platform-contracts" / "v1"
UUID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
    re.IGNORECASE,
)


def load(name):
    return json.loads((CONTRACT / name).read_text(encoding="utf-8"))


def check_dose(dose):
    required = {"substance", "amount", "unit", "route", "timestamp", "tags", "isBackgroundMed"}
    assert required <= dose.keys()
    assert dose["substance"] and dose["unit"] and dose["route"]
    assert isinstance(dose["amount"], (int, float)) and not isinstance(dose["amount"], bool)
    assert math.isfinite(dose["amount"]) and dose["amount"] >= 0
    assert isinstance(dose["timestamp"], int) and dose["timestamp"] >= 0
    assert isinstance(dose["tags"], list)
    if dose.get("id") is not None:
        assert UUID_RE.fullmatch(dose["id"])


def check_export(document):
    assert document["piruExportVersion"] == 1
    assert isinstance(document["exportedAt"], int) and document["exportedAt"] >= 0
    for dose in document["orphanDoses"]:
        check_dose(dose)
    for session in document["sessions"]:
        assert UUID_RE.fullmatch(session["id"])
        assert isinstance(session["startDate"], int) and session["startDate"] >= 0
        for dose in session["doses"]:
            check_dose(dose)


def test_schema_is_valid_json_and_declares_v1():
    schema = load("piru-native.schema.json")
    assert schema["$schema"].endswith("2020-12/schema")
    assert schema["properties"]["piruExportVersion"]["const"] == 1
    assert schema["additionalProperties"] is True


def test_fixtures_are_valid_and_future_fields_are_tolerated():
    for fixture in (CONTRACT / "fixtures").glob("*.json"):
        check_export(json.loads(fixture.read_text(encoding="utf-8")))

    assert "futureField" in load("fixtures/unknown-fields.json")


def test_legacy_fixture_allows_missing_optional_fields():
    legacy = load("fixtures/legacy-export.json")
    dose = legacy["orphanDoses"][0]
    assert dose["id"] is None
    assert "inventory" not in legacy


if __name__ == "__main__":
    test_schema_is_valid_json_and_declares_v1()
    test_fixtures_are_valid_and_future_fields_are_tolerated()
    test_legacy_fixture_allows_missing_optional_fields()
    print("Piru Native v1 contract tests passed")
