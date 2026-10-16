# Chord catalog plan

## Scope

An installable chord catalog for applications that select an instrument, tuning, chord, and fingering, then find comparable chords elsewhere. This package provides data and exact lookups; the application handles conversion, similarity, ranking, and UI.

Planning only for now; no implementation.

## Architecture

One package containing immutable typed catalog objects, build-time validation and derivation, and pure lookup helpers. Data loads in memory; no database or server is needed. Catalog updates arrive through dependency updates.

Choose the language and package format to match the consuming application; TypeScript is a candidate. Keep objects serializable for possible JSON exports. Start with simple lookups and add indexes only as needed. Generate outputs deterministically and version public types and catalog data with the package.

## Data model

Define shared objects once and connect them with stable IDs:

| Definition | Fields |
| --- | --- |
| Instrument | ID, name, family, string count, optional fret limit |
| Tuning | ID, instrument ID, name, ordered open-string pitches |
| FingeringShape | ID, string count, per-string instructions, permitted positions, optional finger assignments |
| Chord type | ID, interval formula, degree information needed for spelling |
| Named chord | ID, spelled root, type ID, full name, aliases; future extensions, alterations, and slash bass |
| ChordFingering | ID, tuning ID, shape ID, position, chord ID, optional tags and provenance |

An instrument has multiple tunings. Shapes work across compatible tunings and positions; named chords work across instruments and shapes. A ChordFingering's instrument comes through its tuning. Multiple named interpretations of the same tuning/shape/position are allowed when explicitly curated.

A named chord can have multiple ChordFingerings in the same tuning. For example, guitar G major can use `320003` or `3x0003` in conventional string-6-first notation. In this catalog's string-1-first order these are `[3, 0, 0, 0, 2, 3]` and `[3, 0, 0, 0, null, 3]`. They reference separate FingeringShapes but share the same `g-major` chord ID and tuning ID. A chord name is not a unique key for a ChordFingering; name lookups return all matching fingerings.

Export these shared collections without repeating definitions inside chord fingerings. A resolution helper returns an chord fingering with its referenced objects, absolute frets, and derived notes.

### Modeling conventions

- Keep IDs stable when labels change; do not reuse retired IDs. Treat example IDs as opaque identifiers, not strings callers should parse.
- In a typed language, distinguish ID types (`TuningId`, `ShapeId`, `ChordId`) to prevent passing one kind where another is expected.
- Define semantic uniqueness separately from IDs: a ChordFingering is unique by `(tuningId, shapeId, position, chordId)`. Aliases belong to the named chord rather than duplicate records.
- Canonicalize fully movable shapes so their minimum sounding offset is 0; adjust position accordingly. Fixed open shapes keep the nut as their anchor. Reject duplicate shapes using their complete instructions and position rules.
- Validate string instructions as mutually exclusive alternatives: open, offset, or muted. Missing finger assignments mean unknown/unspecified, never muted strings.
- Keep spelling equality, sounding-pitch equality, and pitch-class equality separate. Choose the appropriate comparison explicitly in each lookup.
- Derive notes and absolute frets from shared source definitions. Generate them during the build rather than authoring parallel copies.

### Strings and pitches

All per-string arrays start with **string 1 at index `0`**, the highest string on the initial instruments. Preserve string numbering for future reentrant tunings rather than sorting by pitch.

Use sounding pitches with spelling, octave, and numeric value, such as `{ spelling: "E", octave: 2, midi: 40 }`. Tuning string counts must match their instrument.

### Shapes and positions

A ChordFingering's `position` is an absolute anchor fret, not necessarily the root's fret. Each shape has one instruction per string:

| Instruction | Meaning |
| --- | --- |
| `null` | Muted |
| `{ open: true }` | Physical open string, always fret 0 |
| `{ offset: n }` | Fret `position + n`, with nonnegative integer `n` |

Movable shapes use offsets and muted strings, defaulting to `position >= 1`. Fixed open shapes permit only position 0. Shapes combining literal open strings with movable offsets must declare permitted positions explicitly: shifting fretted notes never shifts open strings.

Reuse a shape only with matching string counts and validated chord results. Triads mute unused strings; different string groups use separate shapes initially.

Optional finger-assignment variants include finger numbers and barres `{ finger, fromString, toString, offset }`. Barre string numbers are one-based and offsets share the shape's anchor. Full and partial barres can pass beneath higher fretted notes; sounding strings cannot be open or below the barre. Muted strings may remain muted within a span. A barre must resolve above the nut.

Generic assignments belong to the shape; instrument-specific variants and musical tags such as `caged-C` belong to chord fingerings.

### Chord identity and derived notes

Chord types describe intervals independently of root: major `[0, 4, 7]`, minor `[0, 3, 7]`, power `[0, 7]`. Named chords combine a type and spelled root. Preserve structured qualities, additions, extensions, and alterations as they enter the catalog; callers should not need to parse display names.

Keep root distinct from lowest sounding note. Preserve aliases and enharmonic spellings. Interval-degree information must distinguish identities that share pitch classes, such as added ninth and suspended second.

Derive absolute frets and sounding pitches during the build. Derive spelling using chord identity; validate any authored spelling overrides. Export notes aligned with strings, retaining octaves, repeated notes, and `null` for muted strings. Optional pitch-class sets and bass pitches support exact lookups. Derived notes may be stored by ChordFingering ID and exposed through the resolver.

Initially, every chord tone must be present and no extra pitch classes are allowed; octave doubling is valid. Future omissions need explicit rules.

### Example

Illustrative objects, not a final language or API. Instrument, tuning, and chord definitions are shared separately.

```ts
const shape = {
  id: "six-string-major-barre", stringCount: 6,
  strings: [
    { offset: 0 }, { offset: 0 }, { offset: 1 },
    { offset: 2 }, { offset: 2 }, { offset: 0 },
  ],
};

const chordFingerings = [
  {
    id: "guitar-f-major-barre",
    tuningId: "guitar-6-e-standard",
    shapeId: "six-string-major-barre", position: 1, chordId: "f-major",
  },
  {
    id: "guitar-g-major-barre",
    tuningId: "guitar-6-e-standard",
    shapeId: "six-string-major-barre", position: 3, chordId: "g-major",
  },
];
```

Movable examples below use numbers for offsets and `x` for muted strings, all in string-1-first order:

| Shape | Offsets | Position | Absolute frets | Guitar E-standard chord |
| --- | --- | --- | --- | --- |
| Major barre | 0 0 1 2 2 0 | 1 | 1 1 2 3 3 1 | F major |
| Same barre | 0 0 1 2 2 0 | 3 | 3 3 4 5 5 3 | G major |
| Power | x x x 2 2 0 | 3 | x x x 5 5 3 | G5 |
| Same power | x x x 2 2 0 | 5 | x x x 7 7 5 | A5 |
| Major triad | 0 0 1 x x x | 5 | 5 5 6 x x x | A major |
| Same triad | 0 0 1 x x x | 7 | 7 7 8 x x x | B major |

The major barre can use finger 1 across strings 1–6 at offset 0. Movable shapes do not inherently require a barre. Barre and triad examples demonstrate model support; full catalog coverage comes later.

## Initial catalog

### Instruments and tunings

| Instrument | Tuning | Open strings, string 1 first | MIDI values |
| --- | --- | --- | --- |
| Six-string guitar | E standard | E4 B3 G3 D3 A2 E2 | 64, 59, 55, 50, 45, 40 |
| Four-string bass | E standard | G2 D2 A1 E1 | 43, 38, 33, 28 |
| Five-string bass | B standard | G2 D2 A1 E1 B0 | 43, 38, 33, 28, 23 |

Four- and five-string bass are separate instrument definitions in the bass-guitar family.

### Open CAGED

Proposed scope: five open major guitar chords. Minor, seventh, extended, and movable barre variants come later.

| Chord | Absolute frets, strings 1–6 (`x` = muted) |
| --- | --- |
| C | 0 1 0 2 3 x |
| A | 0 2 2 2 0 x |
| G | 3 0 0 0 2 3 |
| E | 0 0 1 2 2 0 |
| D | 2 3 2 0 x x |

Bass initially has power chords only. Curate any future open major bass shapes explicitly rather than truncating guitar shapes.

### Power chords

- Cover all twelve roots on each initial tuning with curated root/fifth shapes and optional root-octave doubling.
- Proposed range: root in the bass, on either of the lowest two strings, at frets 0–12 inclusive. Other notes may extend beyond fret 12.
- Reuse movable shapes at explicit positions; open-root variants need explicit permission for position 0. Validate pitches against each tuning, including unequal string intervals.
- Mute unused strings and avoid exhaustive string-subset generation.
- Choose canonical root spellings and useful enharmonic aliases, such as C♯5/D♭5; the suffix 5 denotes a power chord.
- Build-time templates may generate finite chord fingerings. Runtime lookup does not synthesize fingerings.

Confirm range, templates, and CAGED interpretation before implementation.

## Lookup helpers

Export readonly data and helpers to:

- List instruments and their tunings; fetch definitions or chord fingerings by ID.
- Resolve chord fingerings into shared definitions, absolute frets, and notes.
- Find chord fingerings by instrument, tuning, chord identity/name/alias, shape, position, exact fret pattern, pitch-class set, or tag.

Return deterministic results, empty collections for unmatched searches, and a documented absent value for unknown IDs. Reject invalid arguments separately. No match means absent catalog coverage, not an unplayable chord. Expose no mutation API and keep helpers independent of UI frameworks.

## Validation

Fail the build for duplicate definitions or chord fingering combinations, broken references, string-count mismatches, invalid offsets/positions/frets, entirely muted shapes, inconsistent pitches/spellings/chord identities, invalid finger assignments/barres, or missing promised coverage.

Verify open C major, muted/open strings, enharmonic spelling, the guitar G-to-B interval, low B on bass, full/partial barres, and triads. Test shape reuse across positions and tunings, fixed open strings, resolution, and ambiguous or missing lookup results. Verify both G-major patterns above resolve to the same named chord and are returned by a G-major lookup in guitar E standard.

Human review establishes playability. Record provenance and check licensing for imported data.

## Delivery

1. Confirm runtime/language, naming conventions, and initial coverage limits.
2. Define shared types, IDs, shapes, positions, and validation rules.
3. Author initial instruments, tunings, shapes, chord definitions, and chord fingerings.
4. Build deterministic exports, derive notes, and validate coverage.
5. Verify package imports and lookup behavior in the consuming runtime.
6. Document usage, coverage, versioning, and catalog extensions; publish JSON if needed.

The first release lets an application install the package and resolve every promised chord fingering into its definitions and notes for use by the application's conversion algorithm.

Later additions include instruments, tunings, chord types, inversions, barre/triad coverage, finger assignments, capo support, and notation conventions. Microtonal support requires revisiting the semitone pitch model. Procedural fingering generation, if needed, belongs in a separate library.
