package chords.schema

import kotlinx.serialization.Serializable

/** Scientific sounding pitch: C4 = MIDI 60. */
@Serializable
data class Pitch(val spelling: String, val octave: Int, val midi: Int)

@Serializable
data class Tuning(
    val id: TuningId, val instrumentId: InstrumentId,
    val name: String, val openStrings: List<Pitch>,
)
