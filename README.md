# Preference Gym

A small Material Design 3 Android app for producing reusable preference-training records.

The Android shell is Java plus ordinary XML/Groovy build plumbing. There is no Kotlin or Compose in this branch.

## Trainers

Known trainer names:

- Jared
- Bill
- Steven
- Mxd
- Giuseppe
- isomorphismes

The app defaults to `isomorphismes`, so the present build does not ask for an identity on launch. The trainer field is still editable from the fixed list so the same APK can later be handed to the other trainers. The selected trainer is written to a plain `current-trainer.txt` configuration file and every judgment also contains its own `trainer.txt`.

## Training workflow

1. Enter or inject a prompt.
2. Enter candidate responses A and B.
3. Choose A or B.
4. Optionally write one or more better responses C/D/E/...
5. Save.

A/B creates one pairwise preference edge. Every authored better response becomes a supervised target and is also recorded as preferred to both A and B. Authored responses are not ranked against one another.

## Filesystem is canonical

SQLite is deliberately not used. Each judgment is an immutable directory:

    preference-training/
        users/
            isomorphismes/
                index.tsv
                records/
                    20260927-015200-123/
                        trainer.txt
                        prompt.txt
                        responses/
                            a.txt
                            b.txt
                            c.txt
                        preference.tsv
                        supervised.tsv
                        metadata.tsv
                        complete

`prompt.txt` and every response file contain the exact text entered by the trainer; they are not hashed, normalized, or deduplicated. Repeated runs of the same prompt remain separate records.

`index.tsv` is only a rectangular cache for quick scans and joins. The record directories are authoritative, and `training-data/rebuild-index` can regenerate the cache. This keeps the convenient relational projection without making a database the source of truth.

A ZIP export is only a transport wrapper around the ordinary files. Repeated prompts and metadata compress well when archived or packed in Git, so repetition is not a strong reason by itself to introduce SQLite.

## Build

Requires JDK 17, Android SDK 37, and Gradle 9.6.0.

    ./build

The output is:

    preference-gym.apk

On the MIRO phone, install by opening the APK in the Files app.

## Hosted acceptance

The pull-request workflow builds the APK, checks ZIP integrity, and publishes `preference-gym.apk` as an Actions artifact. Device installation, Material 3 rendering, file creation, trainer switching, and export remain device acceptance boundaries.
