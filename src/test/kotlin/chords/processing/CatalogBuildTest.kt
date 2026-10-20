package chords.processing

import chords.schema.*
import chords.acceptance.*
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class CatalogBuildTest {
    private val base = catalogSource()
    private fun rejected(source: CatalogSource, message: String) {
        val error = assertFailsWith<IllegalArgumentException> { acceptCatalog(source) }
        assertTrue(error.message.orEmpty().contains(message), error.message)
    }

    @Test
    fun `open C exports open fretted and muted strings in string order`() {
        val catalog = catalogJson.decodeFromString<Catalog>(buildCatalogJson(base))
        assertEquals(DerivedNotes(
            frets = listOf(0, 1, 0, 2, 3, null),
            notes = listOf(Pitch("E", 4, 64), Pitch("C", 4, 60), Pitch("G", 3, 55),
                Pitch("E", 3, 52), Pitch("C", 3, 48), null),
        ), catalog.derivedNotes.getValue(ChordShapeId("guitar-6-e-standard-c-major-open")))
    }

    @Test
    fun `movable power shape resolves references frets and sharp notes`() {
        val resolved = CalculateChordNotes(base).resolve(
            ChordShapeId("guitar-6-e-standard-power-root-6-notes-3-fret-9"))
        assertEquals(TuningId("guitar-6-e-standard"), resolved.tuning.id)
        assertEquals(InstrumentId("guitar-6"), resolved.instrument.id)
        assertEquals(ShapeId("strings-6-power-root-6-notes-3-movable"), resolved.shape.id)
        assertEquals(ChordId("c-sharp-power"), resolved.chord.id)
        assertEquals(DerivedNotes(
            frets = listOf(null, null, null, 11, 11, 9),
            notes = listOf(null, null, null, Pitch("C♯", 4, 61), Pitch("G♯", 3, 56), Pitch("C♯", 3, 49)),
        ), resolved.derived)
    }

    @Test
    fun `five string bass exports low B in its sounding octave`() {
        val catalog = generateCatalog(base)
        val notes = catalog.derivedNotes.getValue(ChordShapeId("bass-5-b-standard-power-root-5-notes-3-fret-0"))
        assertEquals(DerivedNotes(
            frets = listOf(null, null, 2, 2, 0),
            notes = listOf(null, null, Pitch("B", 1, 35), Pitch("F♯", 1, 30), Pitch("B", 0, 23)),
        ), notes)
    }

    @Test
    fun `definitions reject broken references duplicates and invalid musical data`() {
        val tuning = base.tunings.first()
        val shape = base.shapes.first()
        val f = base.chordShapes.first()
        val chord = base.chords.first()
        val structure = base.chordStructures.first()
        val cases = listOf(
            base.copy(instruments = base.instruments + base.instruments.first()) to "duplicate ID",
            base.copy(tunings = base.tunings + tuning.copy(id = TuningId("bad"), instrumentId = InstrumentId("missing"))) to "tuning bad",
            base.copy(tunings = listOf(tuning.copy(openStrings = tuning.openStrings.dropLast(1))) + base.tunings.drop(1)) to "open-string count",
            base.copy(tunings = listOf(tuning.copy(openStrings = tuning.openStrings.map { it.copy(midi = it.midi + 1) })) + base.tunings.drop(1)) to "spelling/octave",
            base.copy(shapes = base.shapes + shape.copy(id = ShapeId("duplicate"))) to "duplicate geometry",
            base.copy(shapes = base.shapes + shape.copy(id = ShapeId("muted"), strings = List(shape.stringCount) { StringInstruction.Muted })) to "entirely muted",
            base.copy(chords = base.chords + chord.copy(id = ChordId("duplicate"))) to "duplicate identity",
            base.copy(chords = base.chords + chord.copy(id = ChordId("broken"), structureId = ChordStructureId("missing"))) to "chord broken",
            base.copy(chordStructures = base.chordStructures + structure.copy(id = ChordStructureId("duplicate"), intervals = structure.intervals.reversed())) to "duplicate formula",
            base.copy(chordStructures = listOf(structure.copy(intervals = listOf(ChordInterval(1, 0), ChordInterval(3, 7)))) + base.chordStructures.drop(1)) to "disagrees with semitones",
            base.copy(chordShapes = base.chordShapes + f.copy(id = ChordShapeId("duplicate"))) to "duplicate placement",
            base.copy(chordShapes = listOf(f.copy(shapeId = ShapeId("missing"))) + base.chordShapes.drop(1)) to f.id.value,
            base.copy(chordShapes = listOf(f.copy(tuningId = TuningId("missing"))) + base.chordShapes.drop(1)) to f.id.value,
            base.copy(chordShapes = listOf(f.copy(chordId = ChordId("missing"))) + base.chordShapes.drop(1)) to f.id.value,
            base.copy(chordShapes = listOf(f.copy(position = 5)) + base.chordShapes.drop(1)) to "not permitted",
            base.copy(chordShapes = listOf(f.copy(tags = listOf("x", "x"))) + base.chordShapes.drop(1)) to "duplicate labels",
            base.copy(chords = listOf(chord.copy(aliases = listOf(" "))) + base.chords.drop(1)) to "blank",
            base.copy(catalogVersion = "latest") to "catalogVersion",
        )
        cases.forEach { (source, expected) -> rejected(source, expected) }
    }

    @Test
    fun `completed data rejects missing orphaned and incorrect derived values`() {
        val catalog = generateCatalog(base)
        val id = catalog.chordShapes.first().id
        for (broken in listOf(
            catalog.copy(derivedNotes = catalog.derivedNotes - id),
            catalog.copy(derivedNotes = catalog.derivedNotes + (ChordShapeId("orphan") to catalog.derivedNotes.getValue(id))),
            catalog.copy(derivedNotes = catalog.derivedNotes + (id to DerivedNotes(emptyList(), emptyList()))),
        )) assertFailsWith<IllegalArgumentException> { validateCompletedCatalog(broken) }
    }

    @Test
    fun `JSON is stable across collection ordering and repeated builds`() {
        val expected = buildCatalogJson(base)
        assertEquals(expected, buildCatalogJson(catalogSource()))
        assertEquals(expected, buildCatalogJson(base.copy(instruments = base.instruments.reversed(),
            tunings = base.tunings.reversed(), shapes = base.shapes.reversed(), chords = base.chords.reversed(),
            chordStructures = base.chordStructures.reversed(), chordShapes = base.chordShapes.reversed())))
    }

    @Test
    fun `failed generation leaves destination untouched`(@TempDir directory: Path) {
        val output = directory.resolve("catalog.json")
        Files.writeString(output, "previous release")
        assertFailsWith<NoSuchElementException> { writeCatalog(base.copy(shapes = emptyList()), output) }
        assertEquals("previous release", Files.readString(output))
        writeCatalog(base, output)
        assertEquals(buildCatalogJson(base), Files.readString(output))
    }

    @Test
    fun `spelling respects degrees compound intervals and octave boundaries`() {
        val structure = ChordStructure(ChordStructureId("major"), "Major", listOf(ChordInterval(1, 0), ChordInterval(3, 4), ChordInterval(5, 7)))
        val sharp = Chord(ChordId("sharp"), "C♯", structure.id, "C♯ major")
        assertEquals(mapOf(1 to "C♯", 5 to "E♯", 8 to "G♯"), chordSpellings(sharp, structure))
        assertEquals(Pitch("B♯", 3, 60), pitchFor(60, "B♯"))
        assertEquals(Pitch("C♭", 4, 59), pitchFor(59, "C♭"))
        assertEquals(Pitch("F♯♯", 4, 67), pitchFor(67, "F♯♯"))
        val ninth = structure.copy(intervals = structure.intervals + ChordInterval(9, 14))
        assertEquals("D♯", chordSpellings(sharp, ninth)[3])
        val enharmonic = structure.copy(intervals = structure.intervals + ChordInterval(4, 4))
        assertEquals("E♯", chordSpellings(sharp, enharmonic)[5])
    }
}
