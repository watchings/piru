# Web capability policy

The Web target is a local-first PWA. It has no account, sync service,
analytics, telemetry, or remote journal API.

The complete first cross-platform release must provide:

- local journal, inventory, tolerance, insights, PK/interaction views;
- encrypted export/import using a passphrase-derived key;
- PDF report export generated locally;
- barcode capture where the browser grants camera access;
- notifications where the browser grants permission.

If a browser cannot provide a capability (for example background notification
delivery), the UI must state the limitation rather than imply that the action
completed. The browser-local-data warning and manual backup responsibility are
part of the product contract.
