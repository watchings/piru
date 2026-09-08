# Piru platform contract v1

This directory contains the stable interchange boundary for iOS, Android, and
the Web PWA. It is deliberately independent of SwiftData, Room, IndexedDB,
SwiftUI, Compose, and any server.

- `piru-native.schema.json` is the first versioned export envelope.
- Timestamps are RFC 3339 and represent instants in UTC.
- Unknown properties must be preserved when possible and ignored when not
  understood.
- A platform database is an implementation detail; database files are never a
  synchronization protocol.

The first migration slice is intentionally small. New entities and fields must
extend this contract with fixtures and migration notes before platform code
depends on them.
