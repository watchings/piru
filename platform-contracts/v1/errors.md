# Import validation errors

| Code | Meaning | Required behavior |
|---|---|---|
| `INVALID_ENVELOPE` | Root is not an object or has the wrong version | Reject without mutating local data |
| `MISSING_REQUIRED_FIELD` | A required field is absent | Identify the JSON path and reject |
| `INVALID_TIMESTAMP` | Timestamp is not non-negative epoch milliseconds | Reject the affected document |
| `INVALID_AMOUNT` | Amount is negative, NaN, or infinite | Reject the affected record |
| `INVALID_ENUM` | Route or kind is not recognized | Preserve only if the platform supports an unknown value; otherwise reject with path |
| `DUPLICATE_ID` | An imported ID conflicts with existing data | Keep existing data and apply the documented deduplication policy |
| `UNSUPPORTED_VERSION` | A newer contract version is encountered | Do not guess; offer export/update guidance |
| `BACKUP_DECRYPTION_FAILED` | Passphrase or encrypted envelope is invalid | Leave existing data unchanged |

Errors must not include full notes, locations, or other sensitive payloads in
logs or telemetry. Piru has no telemetry.
