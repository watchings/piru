"""Static parity checks for the first production Journal screen."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
WEB = (ROOT / "web" / "index.html").read_text(encoding="utf-8")
CSS = (ROOT / "web" / "theme.css").read_text(encoding="utf-8")
ANDROID = (ROOT / "android/app/src/main/java/app/piru/android/MainActivity.kt").read_text(
    encoding="utf-8"
)


def test_primary_navigation_matches_ios_tabs():
    for label in ("Journal", "Library", "Tools", "Insights"):
        assert label in WEB
        assert label in ANDROID


def test_journal_regions_exist_on_both_platforms():
    for region in ("Log a dose", "Recent doses"):
        assert region in WEB
        assert region in ANDROID


def test_shared_visual_contract():
    for token in ("--piru-accent", "--piru-card", "--piru-input", "--piru-radius"):
        assert token in CSS
    for token in ("PiruTheme", "NavigationBar", "Card"):
        assert token in ANDROID


if __name__ == "__main__":
    test_primary_navigation_matches_ios_tabs()
    test_journal_regions_exist_on_both_platforms()
    test_shared_visual_contract()
    print("UI parity checks passed")
