package chords.schema

import kotlinx.serialization.Serializable

@Serializable
data class Instrument(
    val id: InstrumentId, val name: String, val family: String,
    val stringCount: Int, val fretLimit: Int? = null,
)
