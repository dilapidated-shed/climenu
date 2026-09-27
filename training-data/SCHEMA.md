# Training data schema

The filesystem is the source of truth.

Each record lives under `preference-training/users/<trainer>/records/<record-id>/`.

- `trainer.txt` — exact trainer name.
- `prompt.txt` — exact prompt text.
- `responses/a.txt`, `b.txt`, ... — exact response text.
- `preference.tsv` — directed preference edges with columns `preferred`, `rejected`, `relation`.
- `supervised.tsv` — authored target responses and their author.
- `metadata.tsv` — schema version, time, model/source labels, authored-response count.
- `complete` — empty marker written before the temporary directory is renamed into place.

`index.tsv` one level above `records/` is derived and may be deleted or rebuilt at any time.

Record IDs are timestamps plus a local collision suffix. They are identifiers for runs, not hashes of content. Identical prompts or responses may legitimately occur in multiple records.

This representation supports both pairwise preference training and supervised fine-tuning exports without forcing one rectangular schema to erase the distinction between original candidates and authored targets.
