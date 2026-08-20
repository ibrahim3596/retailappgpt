# Scanner lifecycle audit note

The barcode scanner previously initialized CameraX/ML Kit inside `AndroidView.update`, which could run again on recomposition and create multiple executors/scanners without deterministic cleanup.

The scanner lifecycle fix moves binding into `DisposableEffect`, keeps the current callback via `rememberUpdatedState`, and shuts down the analyzer/scanner on dispose.
