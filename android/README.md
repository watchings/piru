# Piru Android

This module is the first Android migration slice. It targets Android API 24
(Android 7.0) and has no Google Play Services dependency, so the same APK can
be distributed through Google Play and non-Google channels.

The production migration must add Room, Health Connect (optional capability),
barcode scanning, notifications, inventory, tolerance, insights, and PDF
export behind platform interfaces. Those interfaces must consume the
versioned contracts in `../platform-contracts/`; they must not mirror
SwiftData implementation details.
