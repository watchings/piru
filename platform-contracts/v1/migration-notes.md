# Migration notes

## From existing Piru Native exports

The current iOS exporter already writes `piruExportVersion` and epoch-millisecond
timestamps. Android and Web should import this shape directly instead of
inventing a second journal format.

- Missing optional fields decode to their documented defaults.
- Missing dose IDs receive new IDs; content deduplication remains an importer
  concern.
- Missing `inventory` means an empty inventory.
- Unknown fields are ignored and must not invalidate otherwise valid records.
- Invalid required fields fail the import before any local database mutation.

## From PsyLog

PsyLog is a separate compatibility input. Convert it into this contract through
an explicit adapter; do not label a PsyLog document as `piruExportVersion: 1`.
Fields PsyLog cannot represent (session title, location, inventory, custom
substances) remain absent or use documented defaults.
