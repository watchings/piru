# Piru Native v1 schema

This contract formalizes the existing `PiruFile` wire format in
`Piru/Utilities/DataExportImport+PiruNative.swift`. It is an interchange format
for local export/import, not a database schema and not a synchronization API.

## Stability rules

- `piruExportVersion: 1` is the format version, not the app version.
- Existing fields retain their meaning. Additive fields are allowed while the
  version remains 1; unknown fields must be ignored by readers.
- A required-field or semantic change requires a new contract version and an
  explicit importer.
- The envelope's `inventory` field remains optional for files written before
  inventory was introduced.
- `id` on a dose is nullable for old exports; importers generate a fresh UUID
  when it is absent.

## Time and units

- All timestamps are non-negative Unix epoch milliseconds.
- An instant is stored in UTC; the viewer chooses the display timezone.
- `amount` is expressed in `unit`; no implicit conversion is permitted.
- `route` and `kind` are stable string raw values owned by each platform's
  domain adapter.

## Privacy

The file can contain sensitive journal notes, locations, inventory and health
observations. It must only be written after an explicit export action and should
be encrypted by the platform backup flow. It must not be uploaded, logged,
included in URLs, or sent to analytics.
