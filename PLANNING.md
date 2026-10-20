# Chord catalog plan

## Scope

Shapes describe string/fret patterns only; hand placement is outside scope.

A language-agnostic JSON chord catalog for applications that select an instrument, tuning, chord, and shape, then find comparable chords elsewhere. This repository provides catalog data; the application handles conversion, similarity, ranking, and UI.

## Architecture

Author typed Kotlin objects and use Kotlin/JVM build tooling to validate them and generate one shared JSON catalog. Use Gradle Kotlin DSL, the Gradle wrapper, and kotlinx.serialization.

Consumers in any language parse JSON and follow stable IDs; they need no Kotlin/JVM runtime, database, or server. Lookup and resolution functions are internal build tooling, not a required consumer SDK.

Release deterministic JSON plus a JSON Schema, with separate catalog and format versions. Include derived notes without repeating shared definitions. Keep serialization field names and instruction tags independent of Kotlin class names.

## Data model

Define shared objects once and connect them with stable IDs:

| Definition | Fields |
| --- | --- |
| Instrument | ID, name, family, string count, optional fret limit |
| Tuning | ID, instrument ID, name, ordered open-string pitches |
| Shape | ID, string count, per-string instructions, permitted positions |
| Chord structure | ID, name, intervals with degree and semitone offset |
| Chord | ID, spelled root, structure ID, full name, aliases, optional slash bass |
| ChordShape | ID, tuning ID, shape ID, position, chord ID, optional tags |

An instrument has multiple tunings. Shapes work across compatible tunings and positions; chords work across instruments and shapes. A ChordShape's instrument comes through its tuning. Multiple named interpretations of the same tuning/shape/position are allowed when explicitly curated.

A chord can have multiple ChordShapes in the same tuning. For example, guitar G major can use `320003` or `3x0003` in conventional string-6-first notation. In this catalog's string-1-first order these are `[3, 0, 0, 0, 2, 3]` and `[3, 0, 0, 0, null, 3]`. They reference separate Shapes but share the same `g-major` chord ID and tuning ID. A chord name is not a unique key for a ChordShape; consumer name lookups should return all matching shapes.

Author shared definitions in `catalog/`; the build gathers them into `CatalogSource` and assembles the exported `Catalog` with derived notes. Export these shared collections without repeating definitions inside chord shapes. Internal resolution assembles referenced definitions and derives absolute frets and notes.

### Modeling conventions

- Keep IDs stable when labels change; do not reuse retired IDs. Treat example IDs as opaque identifiers, not strings callers should parse.
- In a typed language, distinguish ID types (`TuningId`, `ShapeId`, `ChordId`) to prevent passing one kind where another is expected.
- Define semantic uniqueness separately from IDs: a ChordShape is unique by `(tuningId, shapeId, position, chordId)`. Aliases belong to the chord rather than duplicate records.
- Canonicalize fully movable shapes so their minimum sounding offset is 0; adjust position accordingly. Fixed open shapes keep the nut as their anchor. Reject duplicate shapes using their complete instructions and position rules.
- Validate string instructions as mutually exclusive alternatives: open, offset, or muted.
- Keep spelling equality, sounding-pitch equality, and pitch-class equality separate. Choose the appropriate comparison explicitly in each lookup.
- Derive notes and absolute frets from shared source definitions. Generate them during the build rather than authoring parallel copies.

### Strings and pitches

All per-string arrays start with **string 1 at index `0`**, the highest string on the initial instruments. Preserve string numbering for future reentrant tunings rather than sorting by pitch.

Use sounding pitches with spelling, octave, and numeric value, such as `{ spelling: "E", octave: 2, midi: 40 }`. Tuning string counts must match their instrument.

### Shapes and positions

A ChordShape's `position` is an absolute anchor fret, not necessarily the root's fret. Each shape has one instruction per string:

| Instruction | Meaning |
| --- | --- |
| `{"kind":"muted"}` | Muted |
| `{"kind":"open"}` | Physical open string, always fret 0 |
| `{"kind":"offset","offset":n}` | Fret `position + n`, with nonnegative integer `n` |

Movable shapes use offsets and muted strings, defaulting to `position >= 1`. Fixed open shapes permit only position 0. Shapes combining literal open strings with movable offsets must declare permitted positions explicitly: shifting fretted notes never shifts open strings.

Position rules are explicit fixed fret sets or inclusive ranges. See [DATA_MODEL.md](docs/DATA_MODEL.md) for the implemented format and validation rules. Derived frets/notes still use null for muted strings.

Define each shape once, independently of tunings. Chord shapes explicitly select tuning, shape, position, chord, and tags. Adding a tuning does not automatically apply existing templates; reuse compatible shapes or author new geometry.

Use `fixedShape` for absolute nut-anchored patterns and `Shape` for explicit movable offsets. Reuse a shape only with matching string counts and validated chord results. Triads mute unused strings; different string groups use separate shapes initially.

### Chord identity and derived notes

Chord structures describe intervals independently of root: major `[0, 4, 7]`, minor `[0, 3, 7]`, power `[0, 7]`. Chords combine a chord structure and spelled root. Preserve structured qualities, additions, extensions, and alterations as they enter the catalog; callers should not need to parse display names.

Keep root distinct from lowest sounding note. Preserve aliases and enharmonic spellings. Interval-degree information must distinguish identities that share pitch classes, such as added ninth and suspended second.

Derive absolute frets and sounding pitches during the build. Derive spelling using chord identity. Export notes aligned with strings, retaining octaves, repeated notes, and `null` for muted strings. Include derived notes keyed by ChordShape ID in the shared JSON catalog.

Initially, every chord tone must be present and no extra pitch classes are allowed; octave doubling is valid. Future omissions need explicit rules.

### Example

Example using the implemented Kotlin types (shared tuning and chord definitions are omitted):

```kotlin
val shape = Shape(
    id = ShapeId("six-string-major-movable"),
    stringCount = 6,
    strings = listOf(0, 0, 1, 2, 2, 0).map { StringInstruction.Offset(it) },
    positions = PermittedPositions.Range(min = 1),
)

val fMajor = ChordShape(
    id = ChordShapeId("guitar-f-major-movable"),
    tuningId = TuningId("guitar-6-e-standard"),
    shapeId = shape.id,
    position = 1,
    chordId = ChordId("f-major"),
)
val gMajor = fMajor.copy(
    id = ChordShapeId("guitar-g-major-movable"),
    position = 3,
    chordId = ChordId("g-major"),
)
```

Movable examples below use numbers for offsets and `x` for muted strings, all in string-1-first order:

| Shape | Offsets | Position | Absolute frets | Guitar E-standard chord |
| --- | --- | --- | --- | --- |
| Movable major | 0 0 1 2 2 0 | 1 | 1 1 2 3 3 1 | F major |
| Same shape | 0 0 1 2 2 0 | 3 | 3 3 4 5 5 3 | G major |
| Power | x x x 2 2 0 | 3 | x x x 5 5 3 | G5 |
| Same power | x x x 2 2 0 | 5 | x x x 7 7 5 | A5 |
| Major triad | 0 0 1 x x x | 5 | 5 5 6 x x x | A major |
| Same triad | 0 0 1 x x x | 7 | 7 7 8 x x x | B major |

Movable major and triad examples demonstrate model support; full catalog coverage comes later.

## Initial catalog

The authored catalog contains 3 instruments, 3 tunings, 2 chord structures, 17 chords, 30 shapes, and 162 chord shapes.

### Instruments and tunings

| Instrument | Tuning | Open strings, string 1 first | MIDI values |
| --- | --- | --- | --- |
| Six-string guitar | E standard | E4 B3 G3 D3 A2 E2 | 64, 59, 55, 50, 45, 40 |
| Four-string bass | E standard | G2 D2 A1 E1 | 43, 38, 33, 28 |
| Five-string bass | B standard | G2 D2 A1 E1 B0 | 43, 38, 33, 28, 23 |

Four- and five-string bass are separate instrument definitions in the bass-guitar family.

### Open CAGED

Initial scope: C, A, G, E, and D open major guitar chords, including both G-major shapes. Minor, seventh, extended, and movable variants come later.

| Chord | Absolute frets, strings 1–6 (`x` = muted) |
| --- | --- |
| C | 0 1 0 2 3 x |
| A | 0 2 2 2 0 x |
| G | 3 0 0 0 2 3 |
| G (alternative) | 3 0 0 0 x 3 |
| E | 0 0 1 2 2 0 |
| D | 2 3 2 0 x x |

Bass initially has power chords only. Curate any future open major bass shapes explicitly rather than truncating guitar shapes.

### Power chords

- Cover all twelve roots on each initial tuning with both two-note root/fifth and three-note root/fifth/octave shapes.
- Initial coverage range: root in the bass, on either of the lowest two strings, at frets 0–12 inclusive. Other notes may extend beyond fret 12.
- Reuse movable shapes at explicit positions; open-root variants need explicit permission for position 0. Validate pitches against each tuning, including unequal string intervals.
- Mute unused strings and avoid exhaustive string-subset generation.
- Choose canonical root spellings and useful enharmonic aliases, such as C♯5/D♭5; the suffix 5 denotes a power chord.
- Build-time templates may generate finite chord shapes. Runtime lookup does not synthesize shapes.

The fret range limits initial catalog coverage, not model capability. The model can represent movable major and triad shapes. Test-only fixtures cover movable triads, Drop D, E♭ standard, open minor/seventh patterns, and shape reuse across tunings. These do not expand the initial catalog. Movable major patterns are also tested.

Canonical names use readable labels such as `G major` and `G power chord`; symbols such as `G` and `G5` are aliases.

## Consumer access

The JSON contains instruments, tunings, shapes, chord structures, chords, chord shapes, and derived notes. Consumers build their own lookups using IDs and structured fields. Name/alias searches must support multiple matching shapes. Missing catalog coverage does not imply a chord is unplayable.

## Validation

Fail the build for duplicate definitions or chord shape combinations, broken references, string-count mismatches, invalid offsets/positions/frets, entirely muted shapes, inconsistent pitches/spellings/chord identities, or missing promised coverage. Acceptance tests check record validity and the promised initial content before `buildCatalog` writes JSON. The generator calculates notes and serializes the catalog.

Use representative calculation cases for open C major, muted/open strings, enharmonic spelling, the guitar G-to-B interval, low B on bass, movable patterns, and triads. Keep catalog-wide acceptance checks for references, uniqueness, musical consistency, and required coverage; ordinary additions do not need per-chord expected-note tests. Test shape reuse across positions and tunings, fixed open strings, ID resolution, and missing references. Verify both G-major patterns above reference the same chord in guitar E standard. Name/alias search belongs to consuming applications.

Data is authored in this repository; external collection imports and separate human playability-review tracking are outside scope.

## Delivery

1. Set up Kotlin build tooling and record naming and coverage conventions.
2. Define shared types, IDs, shapes, positions, and validation rules.
3. Author initial instruments, tunings, shapes, chord definitions, and chord shapes.
4. Build deterministic exports, derive notes, and validate coverage.
5. Verify JSON parsing and reference resolution with a non-JVM consumer.
6. Document usage, coverage, versioning, and extensions; release JSON and its schema.

The first release lets an application parse the JSON and resolve every promised chord shape into its definitions and notes for use by the application's conversion algorithm.

Later additions include instruments, tunings, chord structures, inversions, movable-major/triad coverage, capo support, and notation conventions. Microtonal support requires revisiting the semitone pitch model. Procedural shape generation, if needed, belongs in a separate library.
