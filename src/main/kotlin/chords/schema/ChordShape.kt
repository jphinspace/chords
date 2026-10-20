package chords.schema

import kotlinx.serialization.Serializable

@Serializable
data class ChordShape(
    val id: ChordShapeId, val tuningId: TuningId, val shapeId: ShapeId,
    val position: Int, val chordId: ChordId,
    val tags: List<String> = emptyList(),
)

/** Internal read view; deliberately has no JSON serializer. */
data class ResolvedChordShape(
    val chordShape: ChordShape,
    val instrument: Instrument,
    val tuning: Tuning,
    val shape: Shape,
    val chord: Chord,
    val chordStructure: ChordStructure,
    val derived: DerivedNotes,
)
