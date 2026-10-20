package chords.catalog

import chords.schema.*
import chords.acceptance.*
import chords.processing.*

/** Future coverage is test-only; the shipped catalog retains its agreed scope. */
internal fun extendedCatalogFixture(): CatalogSource {
    val base = catalogSource()
    val standard = base.tunings.single { it.id == TuningId("guitar-6-e-standard") }
    val dropD = standard.copy(id = TuningId("guitar-6-drop-d"), name = "Drop D",
        openStrings = standard.openStrings.dropLast(1) + Pitch("D", 2, 38))
    val flat = standard.copy(id = TuningId("guitar-6-e-flat-standard"), name = "E♭ standard",
        openStrings = listOf(Pitch("E♭", 4, 63), Pitch("B♭", 3, 58), Pitch("G♭", 3, 54),
            Pitch("D♭", 3, 49), Pitch("A♭", 2, 44), Pitch("E♭", 2, 39)))
    val minor = ChordStructure(ChordStructureId("minor"), "Minor",
        listOf(ChordInterval(1, 0), ChordInterval(3, 3), ChordInterval(5, 7)))
    val seventh = ChordStructure(ChordStructureId("dominant-seventh"), "Dominant seventh",
        listOf(ChordInterval(1, 0), ChordInterval(3, 4), ChordInterval(5, 7), ChordInterval(7, 10)))
    val triad = Shape(ShapeId("guitar-upper-major-triad"), 6,
        listOf(StringInstruction.Offset(0), StringInstruction.Offset(0), StringInstruction.Offset(1),
            StringInstruction.Muted, StringInstruction.Muted, StringInstruction.Muted),
        PermittedPositions.Range())
    val dropPower = Shape(ShapeId("guitar-drop-power"), 6,
        List(3) { StringInstruction.Muted } + List(3) { StringInstruction.Offset(0) },
        PermittedPositions.Range())
    val openMinor = fixedShape("guitar-a-minor-open", listOf(0, 1, 2, 2, 0, null))
    val openSeventh = fixedShape("guitar-e-seven-open", listOf(0, 0, 1, 0, 2, 0))
    val bassMinor = fixedShape("bass-a-minor-open", listOf(5, 2, 0, null))
    fun chordShape(id: String, tuning: TuningId, shape: ShapeId, position: Int, chord: String) =
        ChordShape(ChordShapeId(id), tuning, shape, position, ChordId(chord))
    val assembled = catalogSource(base.tunings + listOf(dropD, flat))
    return assembled.copy(
        shapes = assembled.shapes + listOf(triad, dropPower, openMinor, openSeventh, bassMinor),
        chordStructures = assembled.chordStructures + listOf(minor, seventh),
        chords = assembled.chords + listOf(
            Chord(ChordId("a-minor"), "A", minor.id, "A minor"),
            Chord(ChordId("e-seven"), "E", seventh.id, "E seventh")),
        chordShapes = assembled.chordShapes + listOf(
            chordShape("triad-a", standard.id, triad.id, 5, "a-major"),
            chordShape("triad-c", standard.id, triad.id, 8, "c-major"),
            chordShape("triad-drop-a", dropD.id, triad.id, 5, "a-major"),
            chordShape("triad-flat-a", flat.id, triad.id, 6, "a-major"),
            chordShape("drop-e-power", dropD.id, dropPower.id, 2, "e-power"),
            chordShape("flat-f-power", flat.id, ShapeId("strings-6-power-root-6-notes-3-movable"), 2, "f-power"),
            chordShape("open-a-minor", standard.id, openMinor.id, 0, "a-minor"),
            chordShape("open-e-seven", standard.id, openSeventh.id, 0, "e-seven"),
            chordShape("bass-open-a-minor", TuningId("bass-4-e-standard"), bassMinor.id, 0, "a-minor"),
        ),
    )
}
