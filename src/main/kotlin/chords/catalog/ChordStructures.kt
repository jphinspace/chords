package chords.catalog

import chords.schema.*

internal val chordStructures = listOf(
    ChordStructure(ChordStructureId("major"), "Major", listOf(
        ChordInterval(1, 0), ChordInterval(3, 4), ChordInterval(5, 7),
    )),
    ChordStructure(ChordStructureId("power"), "Power chord", listOf(ChordInterval(1, 0), ChordInterval(5, 7))),
)
