# Piru Web PWA

This is the first migration slice: a dependency-free PWA shell with an explicit
local-storage warning and a Web Crypto encrypted-export boundary. It is not yet
the complete journal. The shell intentionally has no analytics, telemetry,
accounts, or sync code.

Serve this directory over HTTPS (or localhost); service workers and Web Crypto
are not available from arbitrary `file://` URLs.
