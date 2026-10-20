package chords.catalog

import chords.schema.*

/** Author records under one tuning; exported records still carry their tuning ID. */
private class TuningChordShapes(private val tuningId: TuningId) {
    val records = mutableListOf<ChordShape>()

    fun chord(id: String, shape: String, chord: String, tags: List<String>, position: Int = 0) {
        records += ChordShape(ChordShapeId(id), tuningId, ShapeId(shape), position, ChordId(chord), tags)
    }

    fun expand(idPrefix: String, shapeId: ShapeId, chordsByPosition: Map<Int, ChordId>, tags: List<String>) {
        records += expandChordShape(idPrefix, tuningId, shapeId, chordsByPosition, tags)
    }
}

private fun chordShapesFor(tuningId: TuningId, declarations: TuningChordShapes.() -> Unit): List<ChordShape> =
    TuningChordShapes(tuningId).apply(declarations).records.toList()

/** Expand explicitly authored positions; no assumptions about chord quality, root string, or tuning. */
internal fun expandChordShape(
    idPrefix: String,
    tuningId: TuningId,
    shapeId: ShapeId,
    chordsByPosition: Map<Int, ChordId>,
    tags: List<String> = emptyList(),
): List<ChordShape> = chordsByPosition.toSortedMap().map { (position, chordId) ->
    ChordShape(ChordShapeId("$idPrefix-$position"), tuningId, shapeId, position, chordId, tags)
}

/** A finite list of named power chords for the explicitly selected positions. */
private fun powerChordsFrom(root: String, positions: IntRange): Map<Int, ChordId> {
    val pitchClass = roots.indices.single { roots[it].spelling == root }
    return positions.associateWith { ChordId("${roots[(pitchClass + it) % 12].id}-power") }
}

internal val chordShapes = buildList {
    addAll(chordShapesFor(TuningId("guitar-6-e-standard")) {
        chord("guitar-6-e-standard-c-major-open",
            "guitar-6-c-open", "c-major", listOf("open", "caged-C"))
        chord("guitar-6-e-standard-a-major-open",
            "guitar-6-a-open", "a-major", listOf("open", "caged-A"))
        chord("guitar-6-e-standard-g-major-open",
            "guitar-6-g-open", "g-major", listOf("open", "caged-G"))
        chord("guitar-6-e-standard-g-major-open-muted-a",
            "guitar-6-g-open-muted-a", "g-major", listOf("open", "caged-G"))
        chord("guitar-6-e-standard-e-major-open",
            "guitar-6-e-open", "e-major", listOf("open", "caged-E"))
        chord("guitar-6-e-standard-d-major-open",
            "guitar-6-d-open", "d-major", listOf("open", "caged-D"))
        expand("guitar-6-e-standard-power-root-6-notes-2-fret",
            ShapeId("strings-6-power-root-6-notes-2-open"),
            powerChordsFrom("E", 0..0), listOf("power", "open"))
        expand("guitar-6-e-standard-power-root-6-notes-2-fret",
            ShapeId("strings-6-power-root-6-notes-2-movable"),
            powerChordsFrom("E", 1..12), listOf("power", "movable"))
        expand("guitar-6-e-standard-power-root-6-notes-3-fret",
            ShapeId("strings-6-power-root-6-notes-3-open"),
            powerChordsFrom("E", 0..0), listOf("power", "open"))
        expand("guitar-6-e-standard-power-root-6-notes-3-fret",
            ShapeId("strings-6-power-root-6-notes-3-movable"),
            powerChordsFrom("E", 1..12), listOf("power", "movable"))
        expand("guitar-6-e-standard-power-root-5-notes-2-fret",
            ShapeId("strings-6-power-root-5-notes-2-open"),
            powerChordsFrom("A", 0..0), listOf("power", "open"))
        expand("guitar-6-e-standard-power-root-5-notes-2-fret",
            ShapeId("strings-6-power-root-5-notes-2-movable"),
            powerChordsFrom("A", 1..12), listOf("power", "movable"))
        expand("guitar-6-e-standard-power-root-5-notes-3-fret",
            ShapeId("strings-6-power-root-5-notes-3-open"),
            powerChordsFrom("A", 0..0), listOf("power", "open"))
        expand("guitar-6-e-standard-power-root-5-notes-3-fret",
            ShapeId("strings-6-power-root-5-notes-3-movable"),
            powerChordsFrom("A", 1..12), listOf("power", "movable"))
    })
    addAll(chordShapesFor(TuningId("bass-4-e-standard")) {
        expand("bass-4-e-standard-power-root-4-notes-2-fret",
            ShapeId("strings-4-power-root-4-notes-2-open"),
            powerChordsFrom("E", 0..0), listOf("power", "open"))
        expand("bass-4-e-standard-power-root-4-notes-2-fret",
            ShapeId("strings-4-power-root-4-notes-2-movable"),
            powerChordsFrom("E", 1..12), listOf("power", "movable"))
        expand("bass-4-e-standard-power-root-4-notes-3-fret",
            ShapeId("strings-4-power-root-4-notes-3-open"),
            powerChordsFrom("E", 0..0), listOf("power", "open"))
        expand("bass-4-e-standard-power-root-4-notes-3-fret",
            ShapeId("strings-4-power-root-4-notes-3-movable"),
            powerChordsFrom("E", 1..12), listOf("power", "movable"))
        expand("bass-4-e-standard-power-root-3-notes-2-fret",
            ShapeId("strings-4-power-root-3-notes-2-open"),
            powerChordsFrom("A", 0..0), listOf("power", "open"))
        expand("bass-4-e-standard-power-root-3-notes-2-fret",
            ShapeId("strings-4-power-root-3-notes-2-movable"),
            powerChordsFrom("A", 1..12), listOf("power", "movable"))
        expand("bass-4-e-standard-power-root-3-notes-3-fret",
            ShapeId("strings-4-power-root-3-notes-3-open"),
            powerChordsFrom("A", 0..0), listOf("power", "open"))
        expand("bass-4-e-standard-power-root-3-notes-3-fret",
            ShapeId("strings-4-power-root-3-notes-3-movable"),
            powerChordsFrom("A", 1..12), listOf("power", "movable"))
    })
    addAll(chordShapesFor(TuningId("bass-5-b-standard")) {
        expand("bass-5-b-standard-power-root-5-notes-2-fret",
            ShapeId("strings-5-power-root-5-notes-2-open"),
            powerChordsFrom("B", 0..0), listOf("power", "open"))
        expand("bass-5-b-standard-power-root-5-notes-2-fret",
            ShapeId("strings-5-power-root-5-notes-2-movable"),
            powerChordsFrom("B", 1..12), listOf("power", "movable"))
        expand("bass-5-b-standard-power-root-5-notes-3-fret",
            ShapeId("strings-5-power-root-5-notes-3-open"),
            powerChordsFrom("B", 0..0), listOf("power", "open"))
        expand("bass-5-b-standard-power-root-5-notes-3-fret",
            ShapeId("strings-5-power-root-5-notes-3-movable"),
            powerChordsFrom("B", 1..12), listOf("power", "movable"))
        expand("bass-5-b-standard-power-root-4-notes-2-fret",
            ShapeId("strings-5-power-root-4-notes-2-open"),
            powerChordsFrom("E", 0..0), listOf("power", "open"))
        expand("bass-5-b-standard-power-root-4-notes-2-fret",
            ShapeId("strings-5-power-root-4-notes-2-movable"),
            powerChordsFrom("E", 1..12), listOf("power", "movable"))
        expand("bass-5-b-standard-power-root-4-notes-3-fret",
            ShapeId("strings-5-power-root-4-notes-3-open"),
            powerChordsFrom("E", 0..0), listOf("power", "open"))
        expand("bass-5-b-standard-power-root-4-notes-3-fret",
            ShapeId("strings-5-power-root-4-notes-3-movable"),
            powerChordsFrom("E", 1..12), listOf("power", "movable"))
    })
}
