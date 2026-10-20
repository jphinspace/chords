package chords.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface StringInstruction {
    @Serializable @SerialName("muted")
    data object Muted : StringInstruction

    @Serializable @SerialName("open")
    data object Open : StringInstruction

    @Serializable @SerialName("offset")
    data class Offset(val offset: Int) : StringInstruction
}

@Serializable
sealed interface PermittedPositions {
    @Serializable @SerialName("fixed")
    data class Fixed(val frets: List<Int>) : PermittedPositions

    /** Inclusive bounds; null max means no model-level upper limit. */
    @Serializable @SerialName("range")
    data class Range(val min: Int = 1, val max: Int? = null) : PermittedPositions
}

@Serializable
data class Shape(
    val id: ShapeId, val stringCount: Int,
    val strings: List<StringInstruction>,
    val positions: PermittedPositions,
)
