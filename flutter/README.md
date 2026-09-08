# Piru Flutter build target

This directory is the first compileable Flutter shell for the migration. CI
runs `tools/swift_to_flutter.py` into `generated/` and publishes that output as
an artifact, then analyzes and builds this shell for web and Android.

The generated translation is intentionally not committed: it is a mechanical
starting point with `TODO Flutter` markers, not a feature-complete port. Ported
models and engines should replace generated files after review.
