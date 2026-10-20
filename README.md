# Chords

A language-agnostic chord catalog. Kotlin/JVM tools generate JSON from authored data, with acceptance tests checking its correctness; consumers need no Kotlin or Java runtime.

## Example JSON entry

An open C-major chord on six-string guitar in E standard, from the catalog's `chordShapes` array:

```json
{
  "id": "guitar-6-e-standard-c-major-open",
  "tuningId": "guitar-6-e-standard",
  "shapeId": "guitar-6-c-open",
  "position": 0,
  "chordId": "c-major",
  "tags": [
    "open",
    "caged-C"
  ]
}
```

`tuningId`, `shapeId`, and `chordId` reference records in `tunings`, `shapes`, and `chords`. The tuning references the instrument. `position` is the shape's anchor fret; here it is 0.

This shape resolves to frets `[0, 1, 0, 2, 3, null]`, in string-1-first order (`null` means muted). Its sounding notes are E4, C4, G3, E3, C3, and muted. These calculated frets and pitches are stored in `derivedNotes`, keyed by the entry's `id`.

## Development

Install JDK 21, then run:

```sh
./gradlew test
./gradlew build
```

The checked-in wrapper downloads Gradle; no system Gradle installation is needed. Windows users can use `gradlew.bat`. The first build requires internet access to download dependencies.

Pinned tools: Gradle 9.3.0, Kotlin 2.4.21, kotlinx.serialization 1.10.0, JDK toolchain 21. The JDK major version is fixed; compatible security patches are allowed. JUnit 5 runs the Kotlin tests.

Build and validate the catalog:

```sh
./gradlew validateCatalog
./gradlew buildCatalog
```

`validateCatalog` runs the test suite, including catalog acceptance and JSON Schema checks, without writing a catalog. `check` and `build` also run those tests; generating the catalog requires `buildCatalog`. `buildCatalog` depends on the tests, then writes UTF-8 JSON to `build/catalog/catalog.json`. Shared collections and derived-note keys are sorted by ID; repeated builds produce identical bytes. The writer generates the complete output before replacing an existing file.

See [PLANNING.md](PLANNING.md) and [IMPLEMENTATION.md](IMPLEMENTATION.md).

The shared Kotlin types are in `src/main/kotlin/chords/schema`. See [DATA_MODEL.md](docs/DATA_MODEL.md) for format and validation rules and [catalog.schema.json](schema/catalog.schema.json) for the JSON contract. Tests check model round trips and schema acceptance/rejection using small fixtures.

Acceptance and schema tests use [Eclipse Vert.x JSON Schema](https://vertx.io/docs/vertx-json-schema/java/), part of the [Eclipse Foundation's Vert.x project](https://projects.eclipse.org/projects/rt.vertx). It is a test-only dependency supporting our Draft 2020-12 schema; JSON consumers need no Vert.x dependencies.

## Catalog conventions

- IDs use lowercase ASCII kebab-case, are unique within their definition collection, and remain stable after release. Never parse IDs for musical meaning or reuse retired IDs.
- References use distinct Kotlin ID types. A ChordShape is unique by tuning, shape, position, and chord ID.
- Canonical names are readable (`G major`, `G power chord`); symbols (`G`, `G5`) are aliases.
- Initial power-chord roots use C, C♯, D, E♭, E, F, F♯, G, A♭, A, B♭, B. ASCII spellings (`C#`, `Eb`, etc.) and alternate enharmonic names are lookup aliases; they do not replace canonical spellings. IDs use words such as `c-sharp-power`.
- Preserve spelling and octave in pitches. Distinguish spelling, sounding pitch, and pitch-class comparisons.
- Every per-string array starts with string 1 at index 0.
- A shape position is its anchor fret, not necessarily its root fret. Keep open strings distinct from relative fret offsets.
- Kotlin objects are the authored source; JSON and derived pitches are generated output. No separately authored resolved catalog is maintained.

## Catalog source

[BuildCatalog.kt](src/main/kotlin/chords/processing/BuildCatalog.kt) gathers authored definitions into `CatalogSource`, calculates notes, and creates the JSON export. The [catalog definitions](src/main/kotlin/chords/catalog) and generated JSON are the inventory of available instruments, tunings, chords, and shapes.

## Adding catalog data

Define shared records in `src/main/kotlin/chords/catalog`. Register each shape once; link it to a tuning, position, and chord through a `ChordShape` record. Chord-shape declarations are grouped by tuning, supplying its ID once per group. Each declaration selects a reusable shape and chords at finite positions. Multiple shapes can represent the same chord in a compatible tuning. Use `fixedShape` for nut-anchored absolute patterns and `Shape` for movable offsets.

Run `./gradlew validateCatalog` after edits. Acceptance failures report the affected record ID and reason. New chord structures supply degree/semitone formulas; display names are not parsed. Ordinary additions need only catalog definitions and the existing acceptance checks. Update documentation or add focused tests when behavior or the format changes. Completeness tests protect the original baseline.

## Source organization

- `schema/`: Kotlin types describing the catalog format and typed IDs.
- `catalog/`: authored instruments, tunings, chords, shapes, and records.
- `processing/`: `BuildCatalog.kt` builds and writes JSON; `CalculateChordNotes.kt` calculates frets and spelled sounding notes.

Tests mirror these packages, with `acceptance/` holding structural, musical, and JSON Schema checks. `CatalogCompletenessTest` checks the promised initial content. Calculation tests use representative open, muted, movable, bass, and accidental/octave cases. Shared acceptance checks apply to new catalog records automatically. The generator handles reference lookup, note calculation, and serialization; catalog rules are enforced by the required tests.
