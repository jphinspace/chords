# Implementation checklist

Based on [PLANNING.md](PLANNING.md). Each checkbox is a separate task.

Build tooling: Kotlin/JVM, Gradle Kotlin DSL, and kotlinx.serialization. Author typed Kotlin objects and release one shared JSON catalog for consumers in any language. Lookup and resolution functions are internal tooling; no consumer SDK is required.

## 1. Set up tooling and confirm scope

- [x] Configure Kotlin/JVM, Gradle Kotlin DSL, and the Gradle wrapper.
- [x] Pin the JDK toolchain and dependency versions.
- [x] Configure kotlinx.serialization and Kotlin test tooling.
- [x] Set ID, chord-name, and enharmonic-alias conventions.
- [x] Confirm open CAGED coverage: C, A, G, E, D major, including both G-major fingerings.
- [x] Confirm two-/three-note power chords, roots on the lowest two strings, root frets 0–12.
- [x] Confirm barre/triad model support and planned tests; defer full catalog coverage.
- [x] Confirm readable chord names with symbolic aliases (`G major`/`G`, `G power chord`/`G5`).

## 2. Define types and rules

- [x] Define distinct Kotlin value classes for shared IDs and ChordFingering IDs.
- [x] Define read-only data classes for Instrument, Tuning, and sounding pitches.
- [x] Define sealed muted/open/offset instructions and permitted shape positions.
- [x] Define finger-assignment and barre types.
- [x] Define chord-type and named-chord types.
- [x] Define ChordFingering references and internal resolution types.
- [x] Define the JSON structure, explicit instruction tags, and stable field names.
- [x] Define catalog-version and format-version fields.
- [x] Define a JSON Schema for the exported catalog.
- [x] Specify uniqueness, shape normalization, and validation rules.

## 3. Author the initial catalog

- [x] Add six-string guitar, four-string bass, and five-string bass.
- [x] Add their standard tunings in string-1-first order.
- [x] Add major and power chord types.
- [x] Add required named chords and aliases.
- [x] Add open CAGED shapes, including both G-major variants.
- [x] Add reusable power-chord shapes and open-root variants.
- [x] Define shapes once and select template tunings explicitly.
- [x] Add ChordFingerings linking shapes, positions, tunings, and chords.
- [x] Assemble authored definitions separately from the exported Catalog; omit derived data until step 4.
- [x] Record provenance.

## 4. Build JSON and validate data

- [ ] Add a `validateCatalog` Gradle task.
- [ ] Implement internal ID resolution for validation and derivation.
- [ ] Implement reference, uniqueness, and structural validation.
- [ ] Implement shape-to-absolute-fret resolution.
- [ ] Derive sounding pitches and chord-aware note spellings.
- [ ] Validate chord tones, finger assignments, and barres.
- [ ] Check promised chord and tuning coverage.
- [ ] Add `buildCatalog`: validate source, derive notes, validate completed catalog/schema, then write JSON.
- [ ] Serialize one deterministic JSON catalog with shared definitions and derived notes.
- [ ] Validate generated JSON against its schema.
- [ ] Make invalid data stop generation with record IDs and actionable errors.

## 5. Verify data and cross-language consumption

- [ ] Verify internal ID resolution against the authored catalog.
- [x] Verify both G-major fingerings share a chord ID and remain distinct records.
- [x] Test source shape reuse across positions and tunings.
- [x] Test source open/muted strings, enharmonic aliases, tuning accidentals, and low B.
- [ ] Test generated chord-aware note spellings.
- [x] Test unequal string intervals, movable triads, and open minor/seventh fixtures.
- [x] Verify initial coverage by record ID and tuning while allowing additional records.
- [ ] Test full/partial barres.
- [ ] Exercise the production resolver and validator with the existing source fixtures.
- [ ] Verify broken references and invalid records fail validation.
- [ ] Verify repeated builds produce identical JSON.
- [ ] Parse the released JSON with a non-JVM JSON parser and resolve a sample ChordFingering by ID.
- [ ] Confirm consumers require no Kotlin or JVM runtime.

## 6. Document and release

- [ ] Document catalog download, JSON structure, and ID resolution.
- [ ] Document the `validateCatalog` and `buildCatalog` commands.
- [x] Document string order, shape offsets, and fret positions.
- [x] Document coverage limits and multiple fingerings per chord.
- [ ] Document how to add and validate catalog definitions.
- [ ] Define catalog and format versioning compatibility rules.
- [ ] Verify release contents, licensing, and usage examples.
- [ ] Prepare a versioned release containing the single JSON catalog and its schema.
