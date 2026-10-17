package chords.catalog

import chords.model.*
import kotlin.test.*

class InitialCatalogTest {
    private val source = extendedCatalogFixture()
    private val natural = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

    private fun pitchClass(spelling: String): Int = Math.floorMod(
        natural.getValue(spelling.first()) + spelling.count { it == '♯' } - spelling.count { it == '♭' }, 12)

    private fun frets(fingering: ChordFingering): List<Int?> =
        source.shapes.single { it.id == fingering.shapeId }.strings.map {
            when (it) {
                StringInstruction.Muted -> null
                StringInstruction.Open -> 0
                is StringInstruction.Offset -> fingering.position + it.offset
            }
        }

    private fun pitches(fingering: ChordFingering): List<Int> {
        val tuning = source.tunings.single { it.id == fingering.tuningId }
        return frets(fingering).mapIndexedNotNull { index, fret -> fret?.plus(tuning.openStrings[index].midi) }
    }

    @Test
    fun `catalog retains initial coverage and unique references`() {
        assertTrue(source.instruments.size >= 3)
        assertTrue(source.tunings.size >= 3)
        assertTrue(source.chordTypes.size >= 2)
        assertTrue(source.chords.size >= 17)
        assertTrue(source.shapes.size >= 30)
        assertTrue(source.chordFingerings.size >= 162)
        val idLists = listOf(source.instruments.map { it.id.value }, source.tunings.map { it.id.value },
            source.chordTypes.map { it.id.value }, source.chords.map { it.id.value },
            source.shapes.map { it.id.value }, source.chordFingerings.map { it.id.value })
        idLists.forEach { assertEquals(it.size, it.distinct().size) }
        assertEquals(source.chordFingerings.size, source.chordFingerings.map {
            listOf(it.tuningId, it.shapeId, it.position, it.chordId)
        }.distinct().size)
        assertEquals(source.shapes.size, source.shapes.map { Triple(it.stringCount, it.strings, it.positions) }.distinct().size)
        for (f in source.chordFingerings) {
            val tuning = source.tunings.single { it.id == f.tuningId }
            val instrument = source.instruments.single { it.id == tuning.instrumentId }
            val shape = source.shapes.single { it.id == f.shapeId }
            assertEquals(instrument.stringCount, tuning.openStrings.size)
            assertEquals(instrument.stringCount, shape.stringCount)
            assertEquals(shape.stringCount, shape.strings.size)
            when (val positions = shape.positions) {
                is PermittedPositions.Fixed -> assertTrue(f.position in positions.frets)
                is PermittedPositions.Range -> {
                    assertTrue(f.position >= positions.min)
                    assertTrue(positions.max == null || f.position <= positions.max)
                }
            }
            assertTrue(f.provenance.isNotEmpty())
        }
        assertTrue(source.chordFingerings.all { f -> source.shapes.any { it.id == f.shapeId } })
        assertTrue(source.chordFingerings.all { f -> source.chords.any { it.id == f.chordId } })
    }

    @Test
    fun `all sounding pitches match declared chords`() {
        for (tuning in source.tunings) for (pitch in tuning.openStrings) {
            assertEquals(pitchClass(pitch.spelling), pitch.midi % 12)
            assertEquals((pitch.octave + 1) * 12 + natural.getValue(pitch.spelling.first()) +
                pitch.spelling.count { it == '♯' } - pitch.spelling.count { it == '♭' }, pitch.midi)
        }
        for (f in source.chordFingerings) {
            val chord = source.chords.single { it.id == f.chordId }
            val type = source.chordTypes.single { it.id == chord.typeId }
            val expected = type.intervals.map { (pitchClass(chord.root) + it.semitones) % 12 }.toSet()
            assertEquals(expected, pitches(f).map { it % 12 }.toSet(), f.id.value)
        }
    }

    @Test
    fun `power chords cover both root strings and every position in both sizes`() {
        for (tuning in source.tunings.filter { it.id.value in setOf(
            "guitar-6-e-standard", "bass-4-e-standard", "bass-5-b-standard") }) {
            val expectedIds = (listOf(tuning.openStrings.size, tuning.openStrings.size - 1)).flatMap { rootString ->
                listOf(2, 3).flatMap { size -> (0..12).map { fret ->
                    ChordFingeringId("${tuning.id.value}-power-root-$rootString-notes-$size-fret-$fret")
                } }
            }.toSet()
            val entries = source.chordFingerings.filter { it.id in expectedIds }
            assertEquals(52, entries.size)
            entries.forEach { assertEquals(tuning.id, it.tuningId, it.id.value) }
            for (rootIndex in listOf(tuning.openStrings.size - 1, tuning.openStrings.size - 2)) {
                for (noteCount in listOf(2, 3)) {
                    val group = entries.filter { f ->
                        val pattern = frets(f)
                        pattern.count { it != null } == noteCount && pattern.indexOfLast { it != null } == rootIndex
                    }
                    assertEquals((0..12).toSet(), group.map { it.position }.toSet())
                    assertEquals(12, group.map { it.chordId }.toSet().size)
                    assertEquals(1, group.filter { it.position > 0 }.map { it.shapeId }.toSet().size)
                    group.forEach { f ->
                        val sounding = pitches(f).sorted()
                        assertEquals(7, sounding[1] - sounding[0])
                        if (noteCount == 3) assertEquals(12, sounding[2] - sounding[0])
                        val chord = source.chords.single { it.id == f.chordId }
                        assertEquals(pitchClass(chord.root), sounding.first() % 12)
                    }
                }
            }
        }
    }

    @Test
    fun `open CAGED patterns preserve string order and both G variants`() {
        val expected = mapOf(
            "c-major" to setOf(listOf(0, 1, 0, 2, 3, null)),
            "a-major" to setOf(listOf(0, 2, 2, 2, 0, null)),
            "g-major" to setOf(listOf(3, 0, 0, 0, 2, 3), listOf(3, 0, 0, 0, null, 3)),
            "e-major" to setOf(listOf(0, 0, 1, 2, 2, 0)),
            "d-major" to setOf(listOf(2, 3, 2, 0, null, null)),
        )
        val expectedIds = expected.keys.map { ChordFingeringId("guitar-6-e-standard-$it-open") }.toSet() +
            ChordFingeringId("guitar-6-e-standard-g-major-open-muted-a")
        val entries = source.chordFingerings.filter { it.id in expectedIds }
        entries.forEach { assertEquals(TuningId("guitar-6-e-standard"), it.tuningId, it.id.value) }
        val actual = entries.groupBy { it.chordId.value }.mapValues { (_, fs) -> fs.map(::frets).toSet() }
        assertEquals(expected, actual)
        val c = source.chordFingerings.single { it.id.value == "guitar-6-e-standard-c-major-open" }
        assertEquals(listOf(64, 60, 55, 52, 48), pitches(c))
        val lowB = source.chordFingerings.single {
            it.id.value == "bass-5-b-standard-power-root-5-notes-3-fret-0"
        }
        assertEquals(listOf(35, 30, 23), pitches(lowB))
    }

    @Test
    fun `aliases and expansion are stable`() {
        assertEquals(initialCatalog(), initialCatalog())
        val sharp = source.chords.single { it.id.value == "c-sharp-power" }
        assertEquals("C♯ power chord", sharp.name)
        assertTrue(sharp.aliases.containsAll(listOf("C♯5", "C#5", "D♭5", "Db5")))
        source.chords.forEach { assertEquals(it.aliases.size, it.aliases.distinct().size) }
    }
}
