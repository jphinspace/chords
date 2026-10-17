package chords.catalog

import chords.model.*

/** Authored definitions only. The build adds derived notes to produce Catalog. */
data class CatalogSource(
    val catalogVersion: String,
    val instruments: List<Instrument>,
    val tunings: List<Tuning>,
    val shapes: List<FingeringShape>,
    val chordTypes: List<ChordType>,
    val chords: List<NamedChord>,
    val chordFingerings: List<ChordFingering>,
)

/** Author an absolute, nut-anchored pattern for any string count or chord quality. */
internal fun fixedShape(id: String, frets: List<Int?>): FingeringShape = FingeringShape(
    ShapeId(id), frets.size, frets.map {
        when (it) {
            null -> StringInstruction.Muted
            0 -> StringInstruction.Open
            else -> StringInstruction.Offset(it)
        }
    }, PermittedPositions.Fixed(listOf(0)),
)
