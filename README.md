# Chords

A language-agnostic chord catalog. Kotlin/JVM tools will validate authored data and generate JSON; consumers will not need Kotlin or Java.

## Development

Install JDK 21, then run:

```sh
./gradlew test
./gradlew build
```

The checked-in wrapper downloads Gradle; no system Gradle installation is needed. Windows users can use `gradlew.bat`. The first build requires internet access to download dependencies.

Pinned tools: Gradle 9.3.0, Kotlin 2.4.21, kotlinx.serialization 1.10.0, JDK toolchain 21. The JDK major version is fixed; compatible security patches are allowed. JUnit 5 runs the Kotlin tests.

Catalog generation and validation tasks will be added in later steps. See [PLANNING.md](PLANNING.md) and [IMPLEMENTATION.md](IMPLEMENTATION.md).

The shared Kotlin types are in `src/main/kotlin/chords/model`. See [DATA_MODEL.md](docs/DATA_MODEL.md) for format and validation rules and [catalog.schema.json](schema/catalog.schema.json) for the JSON contract. Tests check model round trips and schema acceptance/rejection using small fixtures.

Schema tests use [Eclipse Vert.x JSON Schema](https://vertx.io/docs/vertx-json-schema/java/), part of the [Eclipse Foundation's Vert.x project](https://projects.eclipse.org/projects/rt.vertx). It is a test-only dependency and supports our Draft 2020-12 schema; JSON consumers need no Vert.x dependencies.

## Catalog conventions

- IDs use lowercase ASCII kebab-case, are unique within their definition collection, and remain stable after release. Never parse IDs for musical meaning or reuse retired IDs.
- References use distinct Kotlin ID types. A ChordFingering is unique by tuning, shape, position, and chord ID.
- Canonical names are readable (`G major`, `G power chord`); symbols (`G`, `G5`) are aliases.
- Initial power-chord roots use C, C♯, D, E♭, E, F, F♯, G, A♭, A, B♭, B. ASCII spellings (`C#`, `Eb`, etc.) and alternate enharmonic names are lookup aliases; they do not replace canonical spellings. IDs use words such as `c-sharp-power`.
- Preserve spelling and octave in pitches. Distinguish spelling, sounding pitch, and pitch-class comparisons.
- Every per-string array starts with string 1 at index 0. String numbers in barre spans are one-based.
- A shape position is its anchor fret, not necessarily its root fret. Keep open strings distinct from relative fret offsets.
- Kotlin objects are the authored source; JSON and derived pitches are generated output. No separately authored resolved catalog is maintained.
