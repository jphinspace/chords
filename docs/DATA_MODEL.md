# Catalog format 1

Kotlin types live in `chords.model`; [JSON Schema](../schema/catalog.schema.json) defines the language-independent structure. All fields use the declared camelCase names. Use `catalogJson` for serialization.

## Structure

The root contains `formatVersion`, `catalogVersion`, `instruments`, `tunings`, `shapes`, `chordTypes`, `chords`, `chordFingerings`, and `derivedNotes`. Shared collections are arrays. IDs serialize as strings; derived notes form an object keyed by ChordFingering ID.

- `formatVersion: 1` identifies this contract. Incompatible changes require a new format version; readers reject unsupported versions.
- `catalogVersion` is a numeric `major.minor.patch` release identifier, independent of the format.
- Emit optional defaults explicitly: absent assignments/aliases/tags are empty arrays; absent bass, fret limit, and provenance details are null.
- Fields with Kotlin defaults may be omitted on input. Required collections must be present, even when empty.
- Unknown fields and instruction tags are rejected. No JVM class names appear in JSON.

## Shapes and hand placement

Each string uses exactly one tagged instruction:

```json
[{"kind":"open"},{"kind":"offset","offset":1},{"kind":"muted"}]
```

Unlike a shape instruction, a null in derived notes/frets means muted. A null finger assignment means unspecified hand placement. Finger values are `thumb`, `index`, `middle`, `ring`, and `little`.

Positions are either `{"kind":"fixed","frets":[0]}` or `{"kind":"range","min":1,"max":null}`. Fixed frets are sorted, unique, and nonnegative; ranges are inclusive with max >= min. Null max means unbounded by the shape. Literal open strings always remain at fret zero.

All per-string arrays use string 1 first. Barres use one-based `fromString <= toString` with at least two strings, plus an offset from the anchor. Entry-specific finger assignments are alternatives to generic shape assignments, not replacements.

## Musical identity

Spelling uses uppercase A–G with Unicode sharps/flats (up to double accidentals). ASCII spellings are name aliases. Octaves follow scientific pitch notation; MIDI is 0–127. Other integer fields fit signed 32-bit integers; their schema minimums are nonnegative except pitch octave. Derived arithmetic must check overflow before converting to Int. These limits define format 1.

Chord intervals contain degree and semitones, preserving compound degrees: a ninth is `{"degree":9,"semitones":14}`. Roots and slash bass are spellings without octave. Chord types carry musical structure; names and aliases are display/search labels. New extensions/alterations must have explicit interval definitions rather than be inferred from text.

Kotlin properties and collections are read-only interfaces, not deep runtime immutability. Authors must not retain and mutate backing lists. Exported JSON carries no mutability guarantee for consumers.

## Validation rules

Constructor guards cover ID syntax, positions, nonnegative offsets, and version syntax. The schema checks structure and scalar bounds. Neither alone proves a valid catalog. Kotlin decoding is not a substitute for schema validation: most data-class constructors intentionally leave scalar and musical checks to the validation stage. Range ordering and fixed-fret sorting need checks beyond JSON Schema.

Step 4 will implement these cross-record and musical checks:

- IDs are unique within each collection; every reference resolves to the correct collection.
- A ChordFingering is unique by tuning, shape, position, and chord ID. Multiple shapes may reference the same chord; multiple named interpretations may share the same shape/position.
- Named chord identity is root + type + optional bass, preserving spelling. Aliases do not create duplicate chords and may be ambiguous across chords.
- Duplicate chord types have the same degree/semitone formula. Shape identity is string count + instructions + permitted positions, excluding hand-assignment variants.
- Fully movable shapes have minimum sounding offset 0. Fixed open shapes use position 0. Mixed open/movable patterns declare positions explicitly. No shape is entirely muted.
- Shape string count, tuning length, assignment length, and instrument string count agree. Positions satisfy shape limits; resolved frets respect any instrument fret limit.
- Every pitch spelling/octave agrees with MIDI. Derived frets and notes match tuning + shape + position, preserve string order, and use null on exactly the muted strings.
- Derived-note keys match ChordFingering IDs exactly; there are no missing or orphaned results.
- Chord intervals have unique degrees, include root (degree 1, semitones 0), and agree with their declared musical name. Every required chord pitch class is present, with no extra classes; doublings are allowed.
- A slash bass agrees with the lowest sounding note. The anchor need not be the root or bass.
- Barre spans are in range and resolve above fret 0. Sounding strings inside a barre are at or above its fret; higher notes can use another finger. Assigned fingers agree with the resolved frets; open/muted strings have no assigned fretting finger.
- Reject duplicate aliases/tags, empty labels, and duplicate assignment variants. Preserve provenance and review playability manually.
- Check initial coverage specified in PLANNING.md.

Normalization is an authoring operation, not a silent JSON repair: shift movable offsets down to minimum 0 and increase position accordingly, preserving absolute frets. Shift barre offsets and position bounds consistently. Open-string instructions are never shifted.

Catalog is the complete export type and requires derived notes. Step 3 authors the shared definition lists separately; step 4 validates those lists, derives notes, assembles Catalog, validates the completed output, and only then writes JSON. Do not use an empty derivedNotes map as a production placeholder. Test fixtures may supply expected derived values directly.
