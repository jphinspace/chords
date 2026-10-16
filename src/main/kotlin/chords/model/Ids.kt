package chords.model

import kotlinx.serialization.Serializable

private fun requireId(value: String) {
    require(Regex("[a-z0-9]+(?:-[a-z0-9]+)*").matches(value)) {
        "ID must be nonempty lowercase ASCII kebab-case: $value"
    }
}

@Serializable
@JvmInline
value class InstrumentId(val value: String) {
    init { requireId(value) }
}

@Serializable
@JvmInline
value class TuningId(val value: String) {
    init { requireId(value) }
}

@Serializable
@JvmInline
value class ShapeId(val value: String) {
    init { requireId(value) }
}

@Serializable
@JvmInline
value class ChordTypeId(val value: String) {
    init { requireId(value) }
}

@Serializable
@JvmInline
value class ChordId(val value: String) {
    init { requireId(value) }
}

@Serializable
@JvmInline
value class ChordFingeringId(val value: String) {
    init { requireId(value) }
}
