# Implementation checklist

Based on [PLANNING.md](PLANNING.md). Each checkbox is a separate task.

Build tooling: Kotlin/JVM, Gradle Kotlin DSL, and kotlinx.serialization. Author typed Kotlin objects and release one shared JSON catalog for consumers in any language. Lookup and resolution functions are internal tooling; no consumer SDK is required.

## 1. Set up tooling and confirm scope

- [x] Configure Kotlin/JVM, Gradle Kotlin DSL, and the Gradle wrapper.
- [x] Pin the JDK toolchain and dependency versions.
- [x] Configure kotlinx.serialization and Kotlin test tooling.
- [x] Set ID, chord-name, and enharmonic-alias conventions.
- [x] Confirm open CAGED coverage: C, A, G, E, D major, including both G-major shapes.
- [x] Confirm two-/three-note power chords, roots on the lowest two strings, root frets 0–12.
- [x] Confirm movable-major/triad model support and planned tests; defer full catalog coverage.
- [x] Confirm readable chord names with symbolic aliases (`G major`/`G`, `G power chord`/`G5`).

## 2. Define types and rules

- [x] Define distinct Kotlin value classes for shared IDs and ChordShape IDs.
- [x] Define read-only data classes for Instrument, Tuning, and sounding pitches.
- [x] Define sealed muted/open/offset instructions and permitted shape positions.
- [x] Define ChordStructure and Chord records.
- [x] Define ChordShape references and internal resolution types.
- [x] Define the JSON structure, explicit instruction tags, and stable field names.
- [x] Define catalog-version and format-version fields.
- [x] Define a JSON Schema for the exported catalog.
- [x] Specify uniqueness, shape normalization, and validation rules.

## 3. Author the initial catalog

- [x] Add six-string guitar, four-string bass, and five-string bass.
- [x] Add their standard tunings in string-1-first order.
- [x] Add major and power chord structures.
- [x] Add required chords and aliases.
- [x] Add open CAGED shapes, including both G-major variants.
- [x] Add reusable power-chord shapes and open-root variants.
- [x] Define shapes once and expand explicit shape/tuning/position/chord combinations.
- [x] Add ChordShapes linking shapes, positions, tunings, and chords.
- [x] Assemble authored definitions in CatalogSource, separately from the exported Catalog and its derived notes.

## 4. Build JSON and validate data

- [x] Add a `validateCatalog` Gradle task that runs the acceptance suite and other tests.
- [x] Implement internal ID resolution for validation and derivation.
- [x] Implement reference, uniqueness, and structural acceptance checks.
- [x] Implement shape-to-absolute-fret resolution.
- [x] Derive sounding pitches and chord-aware note spellings.
- [x] Check chord tones and string/fret patterns in acceptance tests.
- [x] Check promised chord and tuning completeness in tests; make `buildCatalog` depend on tests.
- [x] Add `buildCatalog`: run acceptance tests on source and generated catalog/schema, then generate and write JSON.
- [x] Serialize one deterministic JSON catalog with shared definitions and derived notes.
- [x] Check generated JSON against its schema in acceptance tests.
- [x] Make catalog acceptance failures stop buildCatalog before writing output, with record IDs and reasons where applicable.

## 5. Verify data and cross-language consumption

- [x] Verify internal ID resolution with a representative authored chord shape.
- [x] Verify both G-major shapes share a chord ID and remain distinct records.
- [x] Test source shape reuse across positions and tunings.
- [x] Test source open/muted strings, enharmonic aliases, tuning accidentals, and low B.
- [x] Test generated chord-aware note spellings.
- [x] Test unequal string intervals, movable triads, and open minor/seventh fixtures.
- [x] Verify initial coverage by tuning and required patterns while allowing additional records.
- [x] Test movable major and triad patterns.
- [x] Use representative expected outputs for note calculations and shared acceptance checks for catalog data.
- [x] Verify acceptance checks reject broken references and invalid records.
- [x] Verify repeated builds produce identical JSON.
- [ ] Parse the released JSON with a non-JVM JSON parser and resolve a sample ChordShape by ID.
- [ ] Confirm consumers require no Kotlin or JVM runtime.

## 6. Document and release

- [x] Document JSON structure and reference relationships.
- [ ] Document release download and a complete consumer ID-resolution example.
- [x] Document the `validateCatalog` and `buildCatalog` commands.
- [x] Document string order, shape offsets, and fret positions.
- [x] Document coverage limits and multiple shapes per chord.
- [x] Document how to add and validate catalog definitions.
- [x] Document format-version changes for incompatible JSON changes.
- [ ] Define catalog-version bump rules and release compatibility policy.
- [ ] Verify release contents, licensing, and usage examples.
- [ ] Prepare a versioned release containing the single JSON catalog and its schema.
