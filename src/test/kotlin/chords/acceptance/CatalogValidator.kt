package chords.acceptance

import chords.schema.*
import chords.processing.*

private fun label(value: String, context: String) = require(value.isNotBlank()) { "${context}: label must not be blank" }
private fun labels(values: List<String>, context: String) {
    values.forEach { label(it, context) }
    require(values.distinct().size == values.size) { "${context}: duplicate labels" }
}
internal class CatalogValidator(private val source: CatalogSource) {
    fun validateDefinitions() {
        uniqueIds(source.instruments.map { it.id.value }, "instruments")
        uniqueIds(source.tunings.map { it.id.value }, "tunings")
        uniqueIds(source.shapes.map { it.id.value }, "shapes")
        uniqueIds(source.chordStructures.map { it.id.value }, "chordStructures")
        uniqueIds(source.chords.map { it.id.value }, "chords")
        uniqueIds(source.chordShapes.map { it.id.value }, "chordShapes")
        require(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(source.catalogVersion)) { "catalog: catalogVersion must be major.minor.patch" }
        source.instrumentsById.values.forEach {
            val c = "instrument ${it.id.value}"
            label(it.name, c); label(it.family, c)
            require(it.stringCount > 0 && (it.fretLimit == null || it.fretLimit >= 0)) { "${c}: invalid string count or fret limit" }
        }
        source.tuningsById.values.forEach {
            val c = "tuning ${it.id.value}"
            label(it.name, c)
            require(it.instrumentId in source.instrumentsById) { "$c: missing reference ${it.instrumentId}" }
            val instrument = source.instrumentsById.getValue(it.instrumentId)
            require(it.openStrings.size == instrument.stringCount) { "${c}: open-string count must match instrument" }
            it.openStrings.forEachIndexed { i, pitch -> validatePitch(pitch, "$c string ${i + 1}") }
        }
        val shapeIdentities = mutableMapOf<Triple<Int, List<StringInstruction>, PermittedPositions>, ShapeId>()
        source.shapesById.values.forEach {
            val c = "shape ${it.id.value}"
            require(it.stringCount > 0 && it.strings.size == it.stringCount) { "${c}: string count mismatch" }
            require(it.strings.any { instruction -> instruction != StringInstruction.Muted }) { "${c}: shape is entirely muted" }
            val identity = Triple(it.stringCount, it.strings, it.positions)
            val previous = shapeIdentities.put(identity, it.id)
            require(previous == null) { "${c}: duplicate geometry of $previous" }
            val offsets = it.strings.filterIsInstance<StringInstruction.Offset>()
            require(offsets.all { offset -> offset.offset >= 0 }) { "$c: negative offset" }
            when (val positions = it.positions) {
                is PermittedPositions.Fixed -> require(positions.frets.isNotEmpty() &&
                    positions.frets.all { fret -> fret >= 0 } && positions.frets == positions.frets.distinct().sorted()) {
                    "$c: fixed positions must be nonempty, nonnegative, sorted and unique"
                }
                is PermittedPositions.Range -> require(positions.min >= 0 && (positions.max == null || positions.max >= positions.min)) {
                    "$c: invalid position range"
                }
            }
            if (StringInstruction.Open !in it.strings && it.positions != PermittedPositions.Fixed(listOf(0))) {
                require(offsets.minOfOrNull { offset -> offset.offset } == 0) { "${c}: normalize movable offsets to minimum 0" }
            }

        }
        val formulas = mutableMapOf<List<ChordInterval>, ChordStructureId>()
        source.chordStructuresById.values.forEach {
            val c = "chord structure ${it.id.value}"
            label(it.name, c)
            require(it.intervals.all { interval -> interval.degree >= 1 && interval.semitones >= 0 }) { "${c}: invalid interval bounds" }
            require(ChordInterval(1, 0) in it.intervals) { "${c}: formula must contain root (1, 0)" }
            require(it.intervals.map { interval -> interval.degree }.distinct().size == it.intervals.size) { "${c}: duplicate interval degree" }
            // Reject incoherent degree/semitone pairs without imposing a closed vocabulary of chord names.
            val major = listOf(0, 2, 4, 5, 7, 9, 11)
            it.intervals.forEach { interval ->
                val degree = interval.degree.toLong() - 1
                val natural = degree / 7 * 12 + major[(degree % 7).toInt()]
                require(interval.semitones.toLong() - natural in -2L..2L) { "${c}: degree ${interval.degree} disagrees with semitones ${interval.semitones}" }
            }
            val previous = formulas.put(it.intervals.sortedBy { interval -> interval.degree }, it.id)
            require(previous == null) { "${c}: duplicate formula of $previous" }
        }
        val identities = mutableMapOf<Triple<String, ChordStructureId, String?>, ChordId>()
        source.chordsById.values.forEach {
            val c = "chord ${it.id.value}"
            label(it.name, c); labels(it.aliases, "$c aliases")
            require(it.structureId in source.chordStructuresById) { "$c: missing reference ${it.structureId}" }
            val structure = source.chordStructuresById.getValue(it.structureId)
            validateSpelling(it.root, c)
            it.bass?.let { bass -> validateSpelling(bass, c) }
            val tones = chordSpellings(it, structure)
            // Check every degree before enharmonic pitch classes are collapsed.
            structure.intervals.forEach { interval ->
                val spelling = chordSpellings(it, structure.copy(intervals = listOf(interval))).values.single()
                validateSpelling(spelling, "$c degree ${interval.degree}")
            }
            it.bass?.let { bass -> require(pitchClass(bass) in tones) { "${c}: slash bass must be a chord tone" } }
            val previous = identities.put(Triple(it.root, it.structureId, it.bass), it.id)
            require(previous == null) { "${c}: duplicate identity of $previous" }
        }
        val combinations = mutableMapOf<List<Any>, ChordShapeId>()
        source.chordShapesById.values.forEach {
            val c = "chordShape ${it.id.value}"
            val previous = combinations.put(listOf(it.tuningId, it.shapeId, it.position, it.chordId), it.id)
            require(previous == null) { "${c}: duplicate placement of $previous" }
            labels(it.tags, "$c tags")

        }
    }

}

internal fun validateCompletedCatalog(catalog: Catalog) {
    require(catalog.formatVersion == CURRENT_FORMAT_VERSION) { "Unsupported catalog format: ${catalog.formatVersion}" }
    val source = CatalogSource(catalog.catalogVersion, catalog.instruments, catalog.tunings,
        catalog.shapes, catalog.chordStructures, catalog.chords, catalog.chordShapes)
    val expected = acceptCatalog(source)
    require(catalog.derivedNotes.keys == expected.derivedNotes.keys) { "derivedNotes: keys must exactly match chordShape IDs" }
    expected.derivedNotes.forEach { (id, notes) ->
        require(catalog.derivedNotes[id] == notes) { "chordShape ${id.value}: derived notes/frets disagree with source" }
    }
}

internal fun validatePitch(pitch: Pitch, context: String) {
    validateSpelling(pitch.spelling, context)
    val value = spelledValue(pitch.spelling)
    require(pitch.midi in 0..127 && pitch.octave in -1..9) { "${context}: pitch outside format 1 bounds: $pitch" }
    require((pitch.octave.toLong() + 1) * 12 + value == pitch.midi.toLong()) { "${context}: spelling/octave disagree with MIDI: $pitch" }
}


/** Acceptance boundary used by tests, never by the JSON generator. */
internal fun acceptCatalog(source: CatalogSource): Catalog {

    CatalogValidator(source).validateDefinitions()
    for (f in source.chordShapes) {
        val c = "chordShape ${f.id.value}"
        require(f.tuningId in source.tuningsById) { "$c: missing reference ${f.tuningId}" }
        val tuning = source.tuningsById.getValue(f.tuningId)
        require(tuning.instrumentId in source.instrumentsById) { "$c: missing reference ${tuning.instrumentId}" }
        val instrument = source.instrumentsById.getValue(tuning.instrumentId)
        require(f.shapeId in source.shapesById) { "$c: missing reference ${f.shapeId}" }
        val shape = source.shapesById.getValue(f.shapeId)
        require(f.chordId in source.chordsById) { "$c: missing reference ${f.chordId}" }
        val chord = source.chordsById.getValue(f.chordId)
        require(chord.structureId in source.chordStructuresById) { "$c: missing reference ${chord.structureId}" }
        val structure = source.chordStructuresById.getValue(chord.structureId)
        require(shape.stringCount == instrument.stringCount && tuning.openStrings.size == shape.stringCount) { "${c}: shape, tuning, and instrument string counts must match" }
        val allowed = when (val rules = shape.positions) {
            is PermittedPositions.Fixed -> f.position in rules.frets
            is PermittedPositions.Range -> f.position >= rules.min && (rules.max == null || f.position <= rules.max)
        }
        require(f.position >= 0 && allowed) { "$c: position ${f.position} not permitted by shape ${shape.id.value}" }
        shape.strings.filterIsInstance<StringInstruction.Offset>().forEach {
            require(f.position.toLong() + it.offset in 0..Int.MAX_VALUE.toLong()) { "$c: resolved fret overflows format 1" }
        }
        val frets = absoluteFrets(shape, f.position)
        val midi = frets.mapIndexedNotNull { i, fret -> fret?.let {
            require(instrument.fretLimit == null || it <= instrument.fretLimit) { "${c}: string ${i + 1} exceeds instrument fret limit" }
            val value = tuning.openStrings[i].midi.toLong() + it
            require(value in 0..127) { "${c}: MIDI outside 0..127" }
            value.toInt()
        } }
        val tones = chordSpellings(chord, structure)
        require(midi.map { it % 12 }.toSet() == tones.keys) { "${c}: sounding pitch classes do not match chord ${chord.id.value}" }
        chord.bass?.let {
            val pc = pitchClass(it)
            require(pc in tones) { "${c}: slash bass must be a chord tone" }
            require(midi.minOrNull()?.rem(12) == pc) { "${c}: slash bass does not match lowest sounding note" }
        }
    }
    val catalog = generateCatalog(source)
    catalog.derivedNotes.forEach { (id, derived) ->
        derived.notes.filterNotNull().forEach { validatePitch(it, "chordShape ${id.value}") }
    }
    return catalog
}

private fun uniqueIds(ids: List<String>, collection: String) {
    val seen = mutableSetOf<String>()
    ids.forEach { id ->
        require(Regex("[a-z0-9]+(?:-[a-z0-9]+)*").matches(id)) { "$collection: invalid ID $id" }
        require(seen.add(id)) { "$collection: duplicate ID $id" }
    }
}

private fun validateSpelling(spelling: String, context: String) {
    require(Regex("[A-G](?:♯{1,2}|♭{1,2})?").matches(spelling)) { "$context: invalid pitch spelling '$spelling'" }
}
