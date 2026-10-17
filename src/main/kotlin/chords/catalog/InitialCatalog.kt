package chords.catalog

import chords.model.*

private val provenance = listOf(Provenance(
    source = "PLANNING.md initial catalog specification and user-provided G-major variants",
    license = "CC0-1.0",
    notes = "Authored in this repository; no third-party chord dataset imported.",
))

/** Explicit fixed-shape declarations; chord identity is independent of geometry. */
private val openShapes = listOf(
    fixedShape("guitar-6-c-open", listOf(0, 1, 0, 2, 3, null)),
    fixedShape("guitar-6-a-open", listOf(0, 2, 2, 2, 0, null)),
    fixedShape("guitar-6-g-open", listOf(3, 0, 0, 0, 2, 3)),
    fixedShape("guitar-6-g-open-muted-a", listOf(3, 0, 0, 0, null, 3)),
    fixedShape("guitar-6-e-open", listOf(0, 0, 1, 2, 2, 0)),
    fixedShape("guitar-6-d-open", listOf(2, 3, 2, 0, null, null)),
)

private fun authoredFingering(
    id: String, tuning: TuningId, shape: String, chord: String, tags: List<String>,
) = ChordFingering(ChordFingeringId(id), tuning, ShapeId(shape), 0, ChordId(chord),
    tags = tags, provenance = provenance)

private val guitarStandard = TuningId("guitar-6-e-standard")
private val openFingerings = listOf(
    authoredFingering("guitar-6-e-standard-c-major-open", guitarStandard,
        "guitar-6-c-open", "c-major", listOf("open", "caged-C")),
    authoredFingering("guitar-6-e-standard-a-major-open", guitarStandard,
        "guitar-6-a-open", "a-major", listOf("open", "caged-A")),
    authoredFingering("guitar-6-e-standard-g-major-open", guitarStandard,
        "guitar-6-g-open", "g-major", listOf("open", "caged-G")),
    authoredFingering("guitar-6-e-standard-g-major-open-muted-a", guitarStandard,
        "guitar-6-g-open-muted-a", "g-major", listOf("open", "caged-G")),
    authoredFingering("guitar-6-e-standard-e-major-open", guitarStandard,
        "guitar-6-e-open", "e-major", listOf("open", "caged-E")),
    authoredFingering("guitar-6-e-standard-d-major-open", guitarStandard,
        "guitar-6-d-open", "d-major", listOf("open", "caged-D")),
)

// Geometry is registered once, independently of the tunings using it.
private val powerShapes = listOf(6, 4, 5).flatMap { count ->
    listOf(count, count - 1).flatMap { rootString ->
        listOf(2, 3).flatMap { noteCount ->
            listOf(true, false).map { openRoot ->
                val strings = MutableList<StringInstruction>(count) { StringInstruction.Muted }
                strings[rootString - 1] = if (openRoot) StringInstruction.Open else StringInstruction.Offset(0)
                strings[rootString - 2] = StringInstruction.Offset(2)
                if (noteCount == 3) strings[rootString - 3] = StringInstruction.Offset(2)
                FingeringShape(
                    ShapeId("strings-$count-power-root-$rootString-notes-$noteCount-${if (openRoot) "open" else "movable"}"),
                    count, strings.toList(),
                    if (openRoot) PermittedPositions.Fixed(listOf(0)) else PermittedPositions.Range())
            }
        }
    }
}

// Adding a tuning does not implicitly apply any templates to it.
private val powerCoverage = listOf(
    TuningId("guitar-6-e-standard") to 6,
    TuningId("bass-4-e-standard") to 4,
    TuningId("bass-5-b-standard") to 5,
)

/** Finite expansion of curated placements, not a runtime fingering search. */
fun initialCatalog(): CatalogSource = assembleInitialCatalog(tunings)

internal fun assembleInitialCatalog(catalogTunings: List<Tuning>): CatalogSource {
    val placements = powerCoverage.flatMap { (tuningId, count) ->
        val tuning = catalogTunings.single { it.id == tuningId }
        listOf(count, count - 1).flatMap { rootString ->
            listOf(2, 3).flatMap { noteCount ->
                (0..12).map { position ->
                    val root = roots[(tuning.openStrings[rootString - 1].midi + position) % 12]
                    val variant = if (position == 0) "open" else "movable"
                    ChordFingering(
                        ChordFingeringId("${tuningId.value}-power-root-$rootString-notes-$noteCount-fret-$position"),
                        tuningId, ShapeId("strings-$count-power-root-$rootString-notes-$noteCount-$variant"),
                        position, ChordId("${root.id}-power"),
                        tags = listOf("power", variant), provenance = provenance)
                }
            }
        }
    }
    return CatalogSource("0.1.0", instruments.toList(), catalogTunings.toList(), openShapes + powerShapes,
        chordTypes.toList(), chords.toList(), openFingerings + placements)
}
