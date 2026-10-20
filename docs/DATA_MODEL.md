# Catalog format 1

Kotlin types live in `chords.schema`; [JSON Schema](../schema/catalog.schema.json) defines the language-independent structure. All fields use the declared camelCase names. Use `catalogJson` for serialization.

## Structure

The root contains `formatVersion`, `catalogVersion`, `instruments`, `tunings`, `shapes`, `chordStructures`, `chords`, `chordShapes`, and `derivedNotes`. Shared collections are arrays. IDs serialize as strings; derived notes form an object keyed by ChordShape ID.

- `formatVersion: 1` identifies this contract. Incompatible changes require a new format version. Consumers should reject unsupported versions; the schema and acceptance tests enforce version 1, but Kotlin decoding alone does not.
- `catalogVersion` is a numeric `major.minor.patch` release identifier, independent of the format.
- The exporter includes defaults explicitly: aliases/tags default to empty arrays, bass and fret limit to null, and range bounds to `min: 1`, `max: null`.
- Fields with Kotlin defaults may be omitted on input. Required collections must be present, even when empty.
- The JSON Schema and configured Kotlin decoder reject unknown fields and instruction tags. No JVM class names appear in JSON.

## Shapes and positions

Each string uses exactly one tagged instruction:

```json
[{"kind":"open"},{"kind":"offset","offset":1},{"kind":"muted"}]
```

Unlike a shape instruction, a null in derived notes/frets means muted.

Positions are either `{"kind":"fixed","frets":[0]}` or `{"kind":"range","min":1,"max":null}`. Fixed frets are sorted, unique, and nonnegative; ranges are inclusive with max >= min when max is present. Null max means unbounded by the shape. Literal open strings always remain at fret zero.

All per-string arrays use string 1 first.

## Musical identity

Spelling uses uppercase A–G with Unicode sharps/flats (up to double accidentals). ASCII spellings are name aliases. Octaves follow scientific pitch notation; MIDI is 0–127. Other integer fields fit signed 32-bit integers; their schema minimums are nonnegative except pitch octave. Fret and MIDI addition use overflow-safe arithmetic; acceptance tests check format bounds. These limits define format 1.

Chord intervals contain degree and semitones, preserving compound degrees: a ninth is `{"degree":9,"semitones":14}`. Roots and slash bass are spellings without octave. Chord structures carry musical structure; names and aliases are display/search labels. New extensions/alterations must have explicit interval definitions rather than be inferred from text.

Kotlin properties and collections are read-only interfaces, not deep runtime immutability. Authors must not retain and mutate backing lists. Exported JSON carries no mutability guarantee for consumers.

## Validation rules

Model constructors hold data; acceptance tests enforce ID syntax, positions, offsets, versions, and musical rules. JSON Schema checks structure and scalar bounds. Kotlin decoding alone does not validate a catalog. Range ordering and fixed-fret sorting are checked by the acceptance tests.

The catalog follows these cross-record and musical rules, checked by acceptance tests and representative calculation tests:

- IDs are unique within each collection; every reference resolves to the correct collection.
- A ChordShape is unique by tuning, shape, position, and chord ID. Multiple shapes may reference the same chord; multiple named interpretations may share the same shape/position.
- Chord identity is root + chord structure + optional bass, preserving spelling. Aliases do not create duplicate chords and may be ambiguous across chords.
- Duplicate chord structures have the same degree/semitone formula. Shape identity is string count + instructions + permitted positions.
- Fully movable shapes have minimum sounding offset 0. Fixed open shapes use position 0. Mixed open/movable patterns declare positions explicitly. No shape is entirely muted.
- Shape string count, tuning length, instrument string count agree. Positions satisfy shape limits; resolved frets respect any instrument fret limit.
- Every pitch spelling/octave agrees with MIDI. Derived frets and notes match tuning + shape + position, preserve string order, and use null on exactly the muted strings.
- Derived-note keys match ChordShape IDs exactly; there are no missing or orphaned results.
- Chord intervals have unique degrees, include root (degree 1, semitones 0), and have degree/semitone pairs within two semitones of the corresponding major/perfect interval. Display names are labels, not parsed formulas. Every required chord pitch class is present, with no extra classes; doublings are allowed.
- A slash bass agrees with the lowest sounding note. The anchor need not be the root or bass.
- Reject duplicate aliases/tags, empty labels.

Completeness tests preserve the original baseline described in [PLANNING.md](../PLANNING.md).

Test note calculation with a small set of representative expected outputs: open, muted, movable, bass, and accidental/octave boundaries. Catalog-wide acceptance checks cover references, uniqueness, and musical consistency. Add calculation cases when introducing new behavior or fixing a regression.

Normalization is an authoring operation, not a silent JSON repair: shift movable offsets down to minimum 0 and increase position accordingly, preserving absolute frets. Shift position bounds consistently. Open-string instructions are never shifted.

Catalog is the complete export type and requires derived notes. `CatalogSource` contains authored lists and ID-keyed lookup maps used only by build tooling; acceptance tests check the records and the generated Catalog against the rules and schema. After tests pass, the build derives notes, assembles Catalog, and writes JSON. Do not use an empty derivedNotes map as a production placeholder. Test fixtures may supply expected derived values directly.

## Build behavior

`./gradlew validateCatalog` runs the test suite without writing JSON; `./gradlew buildCatalog` writes `build/catalog/catalog.json`. `buildCatalog` depends on tests, including initial catalog completeness. The acceptance suite checks both record validity and the promised initial content. Catalog rules live in acceptance tests. The generator uses ordinary map lookups and overflow-safe arithmetic without a separate validation pipeline. Definitions are sorted by ID and derived-note keys use the same ordering; string arrays retain physical order.

Note letters follow the chord root and interval degree, with accidentals chosen to match sounding pitch. Spellings needing more than two accidentals are rejected. If multiple degrees share a pitch class, the lowest degree supplies its spelling; an explicit slash bass supplies the spelling of the lowest sounding notes. Scientific octaves account for spellings crossing C, such as B♯3 = MIDI 60. Slash bass must be a chord tone under the current no-extra-tones rule.
