package chords.schema

import kotlinx.serialization.Serializable

/** A compound ninth is degree 9, semitones 14, not degree 2. */
@Serializable
data class ChordInterval(val degree: Int, val semitones: Int)

/** Root-independent chord degrees and semitone offsets, such as major: 1, 3, 5. */
@Serializable
data class ChordStructure(val id: ChordStructureId, val name: String, val intervals: List<ChordInterval>)
