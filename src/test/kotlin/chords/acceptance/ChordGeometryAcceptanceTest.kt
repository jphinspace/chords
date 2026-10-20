package chords.acceptance

import chords.catalog.*
import chords.schema.*
import chords.acceptance.*
import chords.processing.*
import kotlin.test.*

class ChordGeometryAcceptanceTest {
    private val shape = Shape(ShapeId("movable-major"), 6,
        listOf(0, 0, 1, 2, 2, 0).map { StringInstruction.Offset(it) }, PermittedPositions.Range())
    private fun fixture(pattern: Shape = shape, position: Int = 1, bass: String? = null): CatalogSource {
        val base = catalogSource()
        val chord = Chord(ChordId("f-major"), "F", ChordStructureId("major"), "F major", bass = bass)
        return CatalogSource("0.1.0", base.instruments.take(1), base.tunings.take(1), listOf(pattern),
            base.chordStructures.filter { it.id.value == "major" }, listOf(chord),
            listOf(ChordShape(ChordShapeId("f-major-movable"), base.tunings.first().id, pattern.id, position, chord.id)))
    }

    @Test
    fun `movable major shapes and triads produce expected notes`() {
        val full = acceptCatalog(fixture())
        assertEquals(listOf(1, 1, 2, 3, 3, 1), full.derivedNotes.values.single().frets)
        val triad = shape.copy(strings = shape.strings.take(3) + List(3) { StringInstruction.Muted })
        acceptCatalog(fixture(triad, bass = "A"))
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(bass = "A")) }
    }

    @Test
    fun `literal opens remain at zero in mixed patterns`() {
        val mixed = shape.copy(strings = listOf(StringInstruction.Open, StringInstruction.Offset(0)) + List(4) { StringInstruction.Muted },
            positions = PermittedPositions.Fixed(listOf(2, 5)))
        assertEquals(listOf(0, 2, null, null, null, null), absoluteFrets(mixed, 2))
        assertEquals(listOf(0, 5, null, null, null, null), absoluteFrets(mixed, 5))
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(mixed, 3)) }

    }

    @Test
    fun `normalization fret limits MIDI bounds and overflow are enforced`() {
        val unnormalized = shape.copy(strings = List(6) { StringInstruction.Offset(1) })
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(unnormalized)) }
        val limited = fixture().let { it.copy(instruments = it.instruments.map { i -> i.copy(fretLimit = 2) }) }
        assertFailsWith<IllegalArgumentException> { acceptCatalog(limited) }
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(position = 100)) }
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(position = Int.MAX_VALUE)) }
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(shape.copy(stringCount = 5))) }
        val wrong = shape.copy(strings = listOf(0, 0, 0, 2, 2, 0).map { StringInstruction.Offset(it) })
        assertFailsWith<IllegalArgumentException> { acceptCatalog(fixture(wrong)) }
    }
}
