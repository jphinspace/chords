package chords.model

import kotlinx.serialization.SerialName
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

@Serializable
data class Instrument(
    val id: InstrumentId, val name: String, val family: String,
    val stringCount: Int, val fretLimit: Int? = null,
)

/** Scientific sounding pitch: C4 = MIDI 60. */
@Serializable
data class Pitch(val spelling: String, val octave: Int, val midi: Int)

@Serializable
data class Tuning(
    val id: TuningId, val instrumentId: InstrumentId,
    val name: String, val openStrings: List<Pitch>,
)

@Serializable
sealed interface StringInstruction {
    @Serializable @SerialName("muted")
    data object Muted : StringInstruction

    @Serializable @SerialName("open")
    data object Open : StringInstruction

    @Serializable @SerialName("offset")
    data class Offset(val offset: Int) : StringInstruction {
        init { require(offset >= 0) { "Offset must be nonnegative" } }
    }
}

@Serializable
sealed interface PermittedPositions {
    @Serializable @SerialName("fixed")
    data class Fixed(val frets: List<Int>) : PermittedPositions {
        init {
            require(frets.isNotEmpty() && frets.all { it >= 0 })
            require(frets == frets.distinct().sorted()) { "Fixed positions must be sorted and unique" }
        }
    }

    /** Inclusive bounds; null max means no model-level upper limit. */
    @Serializable @SerialName("range")
    data class Range(val min: Int = 1, val max: Int? = null) : PermittedPositions {
        init { require(min >= 0 && (max == null || max >= min)) }
    }
}

@Serializable
enum class Finger {
    @SerialName("thumb") THUMB,
    @SerialName("index") INDEX,
    @SerialName("middle") MIDDLE,
    @SerialName("ring") RING,
    @SerialName("little") LITTLE,
}

@Serializable
data class Barre(val finger: Finger, val fromString: Int, val toString: Int, val offset: Int)

@Serializable
data class FingerAssignment(
    val fingers: List<Finger?>,
    val barres: List<Barre> = emptyList(),
)

@Serializable
data class FingeringShape(
    val id: ShapeId, val stringCount: Int,
    val strings: List<StringInstruction>,
    val positions: PermittedPositions,
    val fingerAssignments: List<FingerAssignment> = emptyList(),
)

/** A compound ninth is degree 9, semitones 14, not degree 2. */
@Serializable
data class ChordInterval(val degree: Int, val semitones: Int)

@Serializable
data class ChordType(val id: ChordTypeId, val name: String, val intervals: List<ChordInterval>)

@Serializable
data class NamedChord(
    val id: ChordId, val root: String, val typeId: ChordTypeId, val name: String,
    val aliases: List<String> = emptyList(), val bass: String? = null,
)

@Serializable
data class Provenance(val source: String, val license: String? = null, val notes: String? = null)

@Serializable
data class ChordFingering(
    val id: ChordFingeringId, val tuningId: TuningId, val shapeId: ShapeId,
    val position: Int, val chordId: ChordId,
    val tags: List<String> = emptyList(),
    val provenance: List<Provenance> = emptyList(),
    val fingerAssignments: List<FingerAssignment> = emptyList(),
)

/** String-aligned generated values; null means muted. */
@Serializable
data class DerivedNotes(val frets: List<Int?>, val notes: List<Pitch?>)

@Serializable
data class Catalog(
    val formatVersion: Int,
    val catalogVersion: String,
    val instruments: List<Instrument>,
    val tunings: List<Tuning>,
    val shapes: List<FingeringShape>,
    val chordTypes: List<ChordType>,
    val chords: List<NamedChord>,
    val chordFingerings: List<ChordFingering>,
    val derivedNotes: Map<ChordFingeringId, DerivedNotes>,
) {
    init {
        require(formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported catalog format: $formatVersion" }
        require(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(catalogVersion)) {
            "Catalog version must be major.minor.patch"
        }
    }
}

/** Internal read view; deliberately has no JSON serializer. */
data class ResolvedChordFingering(
    val fingering: ChordFingering,
    val instrument: Instrument,
    val tuning: Tuning,
    val shape: FingeringShape,
    val chord: NamedChord,
    val chordType: ChordType,
    val derived: DerivedNotes,
)
