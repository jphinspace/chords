package chords.schema

import kotlinx.serialization.Serializable

@Serializable
data class Chord(
    val id: ChordId, val root: String, val structureId: ChordStructureId, val name: String,
    val aliases: List<String> = emptyList(), val bass: String? = null,
)
