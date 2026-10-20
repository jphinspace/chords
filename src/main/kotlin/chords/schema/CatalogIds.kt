package chords.schema

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class InstrumentId(val value: String)

@Serializable
@JvmInline
value class TuningId(val value: String)

@Serializable
@JvmInline
value class ShapeId(val value: String)

@Serializable
@JvmInline
value class ChordStructureId(val value: String)

@Serializable
@JvmInline
value class ChordId(val value: String)

@Serializable
@JvmInline
value class ChordShapeId(val value: String)
