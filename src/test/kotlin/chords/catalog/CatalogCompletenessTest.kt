package chords.catalog

import chords.schema.*
import chords.processing.*
import kotlin.test.*

/** Required minimum coverage, independent of the source templates. Extra records are welcome. */
internal fun assertCatalogComplete(catalog: Catalog) {
    val expectedTunings = listOf(
        Triple("guitar-6-e-standard", "guitar-6", listOf(64, 59, 55, 50, 45, 40)),
        Triple("bass-4-e-standard", "bass-4", listOf(43, 38, 33, 28)),
        Triple("bass-5-b-standard", "bass-5", listOf(43, 38, 33, 28, 23)),
    )
    val chords = catalog.chords.associateBy { it.id }
    val structures = catalog.chordStructures.associateBy { it.id }
    fun formula(f: ChordShape) = structures.getValue(chords.getValue(f.chordId).structureId).intervals.toSet()
    val power = setOf(ChordInterval(1, 0), ChordInterval(5, 7))
    val major = power + ChordInterval(3, 4)
    expectedTunings.forEach { (tuningId, instrumentId, pitches) ->
        val context = "coverage tuning $tuningId"
        val tuning = catalog.tunings.singleOrNull { it.id.value == tuningId }
        require(tuning != null) { "${context}: required tuning missing" }
        require(tuning.instrumentId.value == instrumentId && tuning.openStrings.map { it.midi } == pitches) { "${context}: required instrument or open pitches changed" }
        val entries = catalog.chordShapes.filter { it.tuningId == tuning.id && formula(it) == power }
        for (rootIndex in listOf(pitches.lastIndex, pitches.lastIndex - 1)) {
            for (count in listOf(2, 3)) for (position in 0..12) {
                val expectedFrets = List<Int?>(pitches.size) { index ->
                    when (index) {
                        rootIndex -> position
                        rootIndex - 1 -> position + 2
                        rootIndex - 2 -> if (count == 3) position + 2 else null
                        else -> null
                    }
                }
                val found = entries.any { f ->
                    val chord = chords.getValue(f.chordId)
                    catalog.derivedNotes.getValue(f.id).frets == expectedFrets &&
                        pitchClass(chord.root) == (pitches[rootIndex] + position) % 12
                }
                require(found) { "${context}: missing $count-note power chord, root string ${rootIndex + 1}, fret $position" }
            }
        }
    }
    val open = listOf(
        "C" to listOf(0, 1, 0, 2, 3, null), "A" to listOf(0, 2, 2, 2, 0, null),
        "G" to listOf(3, 0, 0, 0, 2, 3), "G" to listOf(3, 0, 0, 0, null, 3),
        "E" to listOf(0, 0, 1, 2, 2, 0), "D" to listOf(2, 3, 2, 0, null, null),
    )
    val matches = open.map { (root, frets) ->
        val match = catalog.chordShapes.firstOrNull {
            val chord = chords.getValue(it.chordId)
            it.tuningId.value == "guitar-6-e-standard" && chord.root == root && chord.bass == null &&
                formula(it) == major && catalog.derivedNotes.getValue(it.id).frets == frets
        }
        require(match != null) { "coverage guitar-6-e-standard: missing open $root major pattern $frets" }
        match
    }
    require(matches[2].chordId == matches[3].chordId) { "coverage G major: both open variants must share a chord ID" }
}

class CatalogCompletenessTest {
    @Test
    fun `initial catalog contains promised records`() {
        assertCatalogComplete(generateCatalog(catalogSource()))
    }
    @Test
    fun `coverage cannot be replaced by equivalent notes in another tuning`() {
        val extended = extendedCatalogFixture()
        val reassigned = extended.copy(chordShapes = extended.chordShapes.map {
            if (it.id.value == "guitar-6-e-standard-c-major-open") it.copy(tuningId = TuningId("guitar-6-drop-d")) else it
        })
        val catalog = generateCatalog(reassigned)
        val error = assertFailsWith<IllegalArgumentException> { assertCatalogComplete(catalog) }
        assertTrue(error.message.orEmpty().contains("missing open C"))
        val omitted = catalogSource().copy(chordShapes = catalogSource().chordShapes.filterNot {
            it.id.value == "bass-4-e-standard-power-root-4-notes-2-fret-1"
        })
        assertFailsWith<IllegalArgumentException> { assertCatalogComplete(generateCatalog(omitted)) }
    }

}
