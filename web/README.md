# Piru Web PWA

This is the first migration slice: a dependency-free PWA shell with an explicit
local-storage warning, a read-only substance catalog, and a Web Crypto
encrypted-export boundary and a local IndexedDB dose journal. The CI package copies the generated
`data/snapshots/substances.json` artifact into `catalog/`; it is a release
artifact, never hand-edited. The shell intentionally has no analytics,
telemetry, accounts, or sync code.

Serve this directory over HTTPS (or localhost); service workers and Web Crypto
are not available from arbitrary `file://` URLs.
