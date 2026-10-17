package chords.catalog

import chords.model.*

internal val instruments = listOf(
    Instrument(InstrumentId("guitar-6"), "Six-string guitar", "guitar", 6),
    Instrument(InstrumentId("bass-4"), "Four-string bass guitar", "bass-guitar", 4),
    Instrument(InstrumentId("bass-5"), "Five-string bass guitar", "bass-guitar", 5),
)

internal val tunings = listOf(
    Tuning(TuningId("guitar-6-e-standard"), InstrumentId("guitar-6"), "E standard", listOf(
        Pitch("E", 4, 64), Pitch("B", 3, 59), Pitch("G", 3, 55),
        Pitch("D", 3, 50), Pitch("A", 2, 45), Pitch("E", 2, 40),
    )),
    Tuning(TuningId("bass-4-e-standard"), InstrumentId("bass-4"), "E standard", listOf(
        Pitch("G", 2, 43), Pitch("D", 2, 38), Pitch("A", 1, 33), Pitch("E", 1, 28),
    )),
    Tuning(TuningId("bass-5-b-standard"), InstrumentId("bass-5"), "B standard", listOf(
        Pitch("G", 2, 43), Pitch("D", 2, 38), Pitch("A", 1, 33),
        Pitch("E", 1, 28), Pitch("B", 0, 23),
    )),
)

internal val chordTypes = listOf(
    ChordType(ChordTypeId("major"), "Major", listOf(
        ChordInterval(1, 0), ChordInterval(3, 4), ChordInterval(5, 7),
    )),
    ChordType(ChordTypeId("power"), "Power chord", listOf(ChordInterval(1, 0), ChordInterval(5, 7))),
)

/** Indexed by pitch class, C = 0. */
internal data class Root(val id: String, val spelling: String, val alternatives: List<String> = emptyList())
internal val roots = listOf(
    Root("c", "C"), Root("c-sharp", "C♯", listOf("C#", "D♭", "Db")),
    Root("d", "D"), Root("e-flat", "E♭", listOf("Eb", "D♯", "D#")),
    Root("e", "E"), Root("f", "F"), Root("f-sharp", "F♯", listOf("F#", "G♭", "Gb")),
    Root("g", "G"), Root("a-flat", "A♭", listOf("Ab", "G♯", "G#")),
    Root("a", "A"), Root("b-flat", "B♭", listOf("Bb", "A♯", "A#")), Root("b", "B"),
)

internal val chords = listOf("c", "a", "g", "e", "d").map { id ->
    val root = roots.single { it.id == id }
    NamedChord(ChordId("$id-major"), root.spelling, ChordTypeId("major"),
        "${root.spelling} major", listOf(root.spelling, "${root.spelling}maj"))
} + roots.map { root ->
    NamedChord(ChordId("${root.id}-power"), root.spelling, ChordTypeId("power"),
        "${root.spelling} power chord",
        (listOf("${root.spelling}5") + root.alternatives.flatMap { listOf("${it}5", "$it power chord") }).distinct())
}
