package chords.processing

import chords.schema.*

internal class CalculateChordNotes(private val source: CatalogSource) {
    /** Resolve a placement after definition validation. */
    fun resolve(id: ChordShapeId): ResolvedChordShape {
        val f = source.chordShapesById.getValue(id)
        val tuning = source.tuningsById.getValue(f.tuningId)
        val instrument = source.instrumentsById.getValue(tuning.instrumentId)
        val shape = source.shapesById.getValue(f.shapeId)
        val chord = source.chordsById.getValue(f.chordId)
        val structure = source.chordStructuresById.getValue(chord.structureId)
        val frets = absoluteFrets(shape, f.position)
        val midi = frets.mapIndexed { i, fret ->
            fret?.let {
                Math.addExact(tuning.openStrings[i].midi, it)
            }
        }
        val spellings = chordSpellings(chord, structure)
        val bassMidi = midi.filterNotNull().minOrNull()
        val notes = midi.map { value -> value?.let {
            val spelling = if (it == bassMidi && chord.bass != null) chord.bass else spellings.getValue(it % 12)
            pitchFor(it, spelling)
        } }
        return ResolvedChordShape(f, instrument, tuning, shape, chord, structure, DerivedNotes(frets, notes))
    }
}

internal fun absoluteFrets(shape: Shape, position: Int): List<Int?> {
    return shape.strings.map { instruction ->
        when (instruction) {
            StringInstruction.Muted -> null
            StringInstruction.Open -> 0
            is StringInstruction.Offset -> Math.addExact(position, instruction.offset)
        }
    }
}

private val letters = "CDEFGAB"
private val naturals = listOf(0, 2, 4, 5, 7, 9, 11)

internal fun spelledValue(spelling: String): Int {
    return naturals[letters.indexOf(spelling[0])] + spelling.count { it == '♯' } - spelling.count { it == '♭' }
}

internal fun pitchClass(spelling: String) = Math.floorMod(spelledValue(spelling), 12)

/** Degree selects the letter; semitones select its accidental. Never use a global sharp/flat table. */
internal fun chordSpellings(chord: Chord, structure: ChordStructure): Map<Int, String> {
    val root = pitchClass(chord.root)
    val rootLetter = letters.indexOf(chord.root[0])
    val result = linkedMapOf<Int, String>()
    for (interval in structure.intervals.sortedBy { it.degree }) {
        val letter = (rootLetter.toLong() + interval.degree - 1).mod(7)
        val pc = (root.toLong() + interval.semitones).mod(12)
        val delta = Math.floorMod(pc - naturals[letter] + 6, 12) - 6
        val spelling = letters[letter].toString() + when {
            delta > 0 -> "♯".repeat(delta)
            delta < 0 -> "♭".repeat(-delta)
            else -> ""
        }
        // If two degrees sound alike, prefer the lower degree deterministically.
        result.putIfAbsent(pc, spelling)
    }
    return result
}

internal fun pitchFor(midi: Int, spelling: String): Pitch {
    val value = spelledValue(spelling)
    return Pitch(spelling, (midi - value) / 12 - 1, midi)
}
