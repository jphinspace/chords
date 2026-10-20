package chords.catalog

import chords.acceptance.acceptCatalog
import chords.processing.catalogSource
import chords.schema.*
import kotlin.test.*

class ChordShapeExpansionTest {
    @Test
    fun `multiple power shapes and triads use the same expansion in drop D`() {
        val base = catalogSource()
        val standard = base.tunings.first()
        val dropD = standard.copy(id = TuningId("drop-d"), name = "Drop D",
            openStrings = standard.openStrings.dropLast(1) + Pitch("D", 2, 38))
        fun shape(id: String, offsets: List<Int?>) = Shape(ShapeId(id), 6,
            offsets.map { if (it == null) StringInstruction.Muted else StringInstruction.Offset(it) },
            PermittedPositions.Range())
        val dyad = shape("drop-d-dyad", listOf(null, null, null, null, 0, 0))
        val doubled = shape("drop-d-doubled", listOf(null, null, null, 0, 0, 0))
        val triad = shape("upper-triad", listOf(0, 0, 1, null, null, null))
        val otherTriad = shape("middle-triad", listOf(null, 1, 0, 2, null, null))
        val powerPositions = mapOf(2 to ChordId("e-power"), 5 to ChordId("g-power"))
        val additions = listOf(dyad, doubled).flatMap {
            expandChordShape(it.id.value, dropD.id, it.id, powerPositions)
        } + expandChordShape("upper", dropD.id, triad.id, mapOf(3 to ChordId("g-major"), 5 to ChordId("a-major"), 8 to ChordId("c-major"))) +
            expandChordShape("middle", dropD.id, otherTriad.id, mapOf(7 to ChordId("g-major")))
        val source = base.copy(tunings = base.tunings + dropD,
            shapes = base.shapes + listOf(dyad, doubled, triad, otherTriad), chordShapes = base.chordShapes + additions)
        acceptCatalog(source)
        assertEquals(2, additions.count { it.chordId == ChordId("e-power") })
        assertEquals(2, additions.count { it.chordId == ChordId("g-major") })
        assertEquals(listOf(2, 5), additions.filter { it.shapeId == dyad.id }.map { it.position })
    }
}
