# Implementation checklist

Based on [PLANNING.md](PLANNING.md). Each checkbox is a separate task; implementation has not started.

## 1. Confirm runtime, naming, and coverage

- [ ] Choose the consuming runtime and implementation language.
- [ ] Choose the package format and build/test tools.
- [ ] Set ID, chord-name, and enharmonic-alias conventions.
- [ ] Confirm open CAGED coverage, including both G-major fingerings.
- [ ] Confirm power-chord shapes, root strings, and fret range.

## 2. Define types and rules

- [ ] Define distinct ID types for shared definitions and ChordFingerings.
- [ ] Define Instrument, Tuning, and sounding-pitch types.
- [ ] Define FingeringShape instructions and permitted positions.
- [ ] Define finger-assignment and barre types.
- [ ] Define chord-type and named-chord types.
- [ ] Define ChordFingering references and the resolved result type.
- [ ] Specify uniqueness, shape normalization, and validation rules.
- [ ] Specify lookup results for invalid input, missing IDs, and no matches.

## 3. Author the initial catalog

- [ ] Add six-string guitar, four-string bass, and five-string bass.
- [ ] Add their standard tunings in string-1-first order.
- [ ] Add major and power chord types.
- [ ] Add required named chords and aliases.
- [ ] Add open CAGED shapes, including both G-major variants.
- [ ] Add reusable power-chord shapes and open-root variants.
- [ ] Add ChordFingerings linking shapes, positions, tunings, and chords.
- [ ] Record provenance and review curated shapes for playability.

## 4. Build exports and validate data

- [ ] Set up the package and build scripts.
- [ ] Implement reference, uniqueness, and structural validation.
- [ ] Implement shape-to-absolute-fret resolution.
- [ ] Derive sounding pitches and chord-aware note spellings.
- [ ] Validate chord tones, finger assignments, and barres.
- [ ] Check promised chord and tuning coverage.
- [ ] Generate deterministic readonly catalog exports and derived notes.
- [ ] Make validation failures stop the build.

## 5. Implement and verify package lookups

- [ ] Implement instrument/tuning listing and ID lookups.
- [ ] Implement ChordFingering resolution.
- [ ] Implement the exact filters listed in PLANNING.md.
- [ ] Verify both G-major fingerings appear in the same chord lookup.
- [ ] Test shape reuse across positions and tunings.
- [ ] Test open strings, muted strings, enharmonic spelling, and low B.
- [ ] Test unequal string intervals, full/partial barres, and triad fixtures.
- [ ] Verify deterministic ordering and invalid/missing/ambiguous results.
- [ ] Verify consumers cannot mutate the shared catalog through public exports.
- [ ] Build the package and verify imports in the consuming runtime.

## 6. Document and release

- [ ] Document installation, lookups, and ChordFingering resolution.
- [ ] Document string order, shape offsets, and fret positions.
- [ ] Document coverage limits and multiple fingerings per chord.
- [ ] Document how to add and validate catalog definitions.
- [ ] Define package versioning and compatibility rules.
- [ ] Add generated JSON exports if a consumer needs them.
- [ ] Verify packaged contents, licensing, and usage examples.
- [ ] Prepare versioned release artifacts.
