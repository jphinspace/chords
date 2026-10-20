package chords.schema

/** Authored definitions only. The build adds derived notes to produce Catalog. */
data class CatalogSource(
    val catalogVersion: String,
    val instruments: List<Instrument>,
    val tunings: List<Tuning>,
    val shapes: List<Shape>,
    val chordStructures: List<ChordStructure>,
    val chords: List<Chord>,
    val chordShapes: List<ChordShape>,
) {
    val instrumentsById = instruments.associateBy { it.id }
    val tuningsById = tunings.associateBy { it.id }
    val shapesById = shapes.associateBy { it.id }
    val chordStructuresById = chordStructures.associateBy { it.id }
    val chordsById = chords.associateBy { it.id }
    val chordShapesById = chordShapes.associateBy { it.id }
}
