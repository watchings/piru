"""Cross-platform PK fixture smoke test."""

import json
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]


def remaining(minutes, half_life, peak=60):
    if minutes <= 0:
        return 1.0
    ke = math.log(2) / half_life
    ka = 4 * ke
    for _ in range(50):
        delta = ka - ke
        f = math.log(ka / ke) / delta - peak
        derivative = (delta / ka - math.log(ka / ke)) / (delta * delta)
        ka = max(ke * 1.01, ka - f / derivative)
    return (ka * math.exp(-ke * minutes) - ke * math.exp(-ka * minutes)) / (ka - ke)


def test_fixture():
    fixture = json.loads((Path(__file__).with_name("fixtures") / "pk-dose.json").read_text())
    for minutes, expected in zip(
        fixture["model"]["sampleMinutes"], fixture["model"]["expectedRemainingFraction"]
    ):
        assert abs(remaining(minutes, 300) - expected) < 0.01


if __name__ == "__main__":
    test_fixture()
    print("PK fixture test passed")
