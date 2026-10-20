package chords.catalog

import chords.schema.*

/** Indexed by pitch class, C = 0. */
internal data class Root(val id: String, val spelling: String, val alternatives: List<String> = emptyList())
internal val roots = listOf(
    Root("c", "C"), Root("c-sharp", "C♯", listOf("C#", "D♭", "Db")),
    Root("d", "D"), Root("e-flat", "E♭", listOf("Eb", "D♯", "D#")),
    Root("e", "E"), Root("f", "F"), Root("f-sharp", "F♯", listOf("F#", "G♭", "Gb")),
    Root("g", "G"), Root("a-flat", "A♭", listOf("Ab", "G♯", "G#")),
    Root("a", "A"), Root("b-flat", "B♭", listOf("Bb", "A♯", "A#")), Root("b", "B"),
)

internal val chords = listOf("c", "a", "g", "e", "d", "f").map { id ->
    val root = roots.single { it.id == id }
    Chord(ChordId("$id-major"), root.spelling, ChordStructureId("major"),
        "${root.spelling} major", listOf(root.spelling, "${root.spelling}maj"))
} + roots.map { root ->
    Chord(ChordId("${root.id}-power"), root.spelling, ChordStructureId("power"),
        "${root.spelling} power chord",
        (listOf("${root.spelling}5") + root.alternatives.flatMap { listOf("${it}5", "$it power chord") }).distinct())
}
