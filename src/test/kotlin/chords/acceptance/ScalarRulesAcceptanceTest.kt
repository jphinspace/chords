package chords.acceptance

import chords.processing.catalogSource
import chords.schema.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ScalarRulesAcceptanceTest {
    private val source = catalogSource()

    @Test
    fun `IDs are checked by acceptance tests`() {
        for (bad in listOf("", "G major", "g--major", "-g", "g-", "g\n")) {
            assertFailsWith<IllegalArgumentException> {
                acceptCatalog(source.copy(chords = source.chords.map { it.copy(id = ChordId(bad)) }))
            }
        }
    }

    @Test
    fun `shape bounds are checked even for unused shapes`() {
        val shape = source.shapes.first()
        val invalidPositions = listOf(
            PermittedPositions.Fixed(emptyList()), PermittedPositions.Fixed(listOf(-1)),
            PermittedPositions.Fixed(listOf(2, 1)), PermittedPositions.Fixed(listOf(0, 0)),
            PermittedPositions.Range(-1), PermittedPositions.Range(5, 3),
        )
        val invalidShapes = invalidPositions.map { shape.copy(positions = it) } +
            shape.copy(strings = listOf(StringInstruction.Offset(-1)) + shape.strings.drop(1))
        for (invalid in invalidShapes) {
            assertFailsWith<IllegalArgumentException> {
                acceptCatalog(source.copy(shapes = listOf(invalid), chordShapes = emptyList()))
            }
        }
    }

    @Test
    fun `spelling limits apply to unused chords and open strings`() {
        val major = source.chords.first { it.structureId == ChordStructureId("major") }
        for (root in listOf("H", "", "C#", "C♯♯♯", "B♯♯")) {
            assertFailsWith<IllegalArgumentException> {
                acceptCatalog(source.copy(chords = listOf(major.copy(root = root)), chordShapes = emptyList()))
            }
        }
        val tuning = source.tunings.first()
        assertFailsWith<IllegalArgumentException> {
            acceptCatalog(source.copy(tunings = listOf(tuning.copy(openStrings =
                listOf(tuning.openStrings.first().copy(spelling = "E#")) + tuning.openStrings.drop(1)))))
        }
    }

    @Test
    fun `completed catalog versions are checked by acceptance tests`() {
        val catalog = acceptCatalog(source)
        assertFailsWith<IllegalArgumentException> { validateCompletedCatalog(catalog.copy(formatVersion = 2)) }
        assertFailsWith<IllegalArgumentException> { validateCompletedCatalog(catalog.copy(catalogVersion = "latest")) }
    }

    @Test
    fun `enharmonic collapse cannot hide unsupported interval spellings`() {
        val structure = ChordStructure(ChordStructureId("augmented-seventh"), "Augmented seventh",
            listOf(ChordInterval(1, 0), ChordInterval(7, 12)))
        val chord = Chord(ChordId("c-double-sharp-augmented-seventh"), "C♯♯", structure.id,
            "C double sharp augmented seventh")
        val error = assertFailsWith<IllegalArgumentException> {
            acceptCatalog(source.copy(chordStructures = source.chordStructures + structure,
                chords = source.chords + chord))
        }
        assertTrue(error.message.orEmpty().contains(chord.id.value))
        assertTrue(error.message.orEmpty().contains("B♯♯♯"))
    }

    @Test
    fun `completed catalog rejects placements outside shape positions`() {
        val catalog = acceptCatalog(source)
        val broken = catalog.copy(shapes = catalog.shapes.map {
            if (it.id == ShapeId("guitar-6-c-open")) it.copy(positions = PermittedPositions.Fixed(listOf(1))) else it
        })
        val error = assertFailsWith<IllegalArgumentException> { validateCompletedCatalog(broken) }
        assertTrue(error.message.orEmpty().contains("guitar-6-e-standard-c-major-open"))
        assertTrue(error.message.orEmpty().contains("not permitted"))
    }
}
