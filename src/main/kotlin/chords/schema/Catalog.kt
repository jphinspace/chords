package chords.schema

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Stable wire tags; strict fields and explicit defaults. */
val catalogJson = Json {
    classDiscriminator = "kind"
    encodeDefaults = true
    explicitNulls = true
    prettyPrint = true
}

const val CURRENT_FORMAT_VERSION = 1

/** String-aligned generated values; null means muted. */
@Serializable
data class DerivedNotes(val frets: List<Int?>, val notes: List<Pitch?>)

@Serializable
data class Catalog(
    val formatVersion: Int,
    val catalogVersion: String,
    val instruments: List<Instrument>,
    val tunings: List<Tuning>,
    val shapes: List<Shape>,
    val chordStructures: List<ChordStructure>,
    val chords: List<Chord>,
    val chordShapes: List<ChordShape>,
    val derivedNotes: Map<ChordShapeId, DerivedNotes>,
)
