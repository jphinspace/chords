package chords.catalog

import chords.schema.*

/** Explicit fixed-shape declarations; chord identity is independent of geometry. */
internal val openShapes = listOf(
    fixedShape("guitar-6-c-open", listOf(0, 1, 0, 2, 3, null)),
    fixedShape("guitar-6-a-open", listOf(0, 2, 2, 2, 0, null)),
    fixedShape("guitar-6-g-open", listOf(3, 0, 0, 0, 2, 3)),
    fixedShape("guitar-6-g-open-muted-a", listOf(3, 0, 0, 0, null, 3)),
    fixedShape("guitar-6-e-open", listOf(0, 0, 1, 2, 2, 0)),
    fixedShape("guitar-6-d-open", listOf(2, 3, 2, 0, null, null)),
)

/** Movable patterns shared by placements at different frets. */
internal val movableShapes = listOf(
    Shape(ShapeId("guitar-6-e-major-movable"), 6,
        listOf(0, 0, 1, 2, 2, 0).map { StringInstruction.Offset(it) },
        PermittedPositions.Range()),
)

// Geometry is registered once, independently of the tunings using it.
internal val powerShapes = listOf(6, 4, 5).flatMap { count ->
    listOf(count, count - 1).flatMap { rootString ->
        listOf(2, 3).flatMap { noteCount ->
            listOf(true, false).map { openRoot ->
                val strings = MutableList<StringInstruction>(count) { StringInstruction.Muted }
                strings[rootString - 1] = if (openRoot) StringInstruction.Open else StringInstruction.Offset(0)
                strings[rootString - 2] = StringInstruction.Offset(2)
                if (noteCount == 3) strings[rootString - 3] = StringInstruction.Offset(2)
                Shape(
                    ShapeId("strings-$count-power-root-$rootString-notes-$noteCount-${if (openRoot) "open" else "movable"}"),
                    count, strings.toList(),
                    if (openRoot) PermittedPositions.Fixed(listOf(0)) else PermittedPositions.Range())
            }
        }
    }
}

/** Author an absolute, nut-anchored pattern for any string count or chord quality. */
internal fun fixedShape(id: String, frets: List<Int?>): Shape = Shape(
    ShapeId(id), frets.size, frets.map {
        when (it) {
            null -> StringInstruction.Muted
            0 -> StringInstruction.Open
            else -> StringInstruction.Offset(it)
        }
    }, PermittedPositions.Fixed(listOf(0)),
)
