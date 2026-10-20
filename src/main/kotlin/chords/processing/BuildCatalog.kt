package chords.processing

import chords.catalog.*
import chords.schema.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.encodeToString

/** Gather authored records for generation or acceptance tests. */
internal fun catalogSource(catalogTunings: List<Tuning> = tunings): CatalogSource =
    CatalogSource("0.1.0", instruments.toList(), catalogTunings.toList(), openShapes + powerShapes,
        chordStructures.toList(), chords.toList(), chordShapes)

/** Calculate notes and assemble the export in stable ID order. Acceptance tests check the catalog rules. */
internal fun generateCatalog(source: CatalogSource): Catalog {

    val notes = CalculateChordNotes(source)
    val derived = source.chordShapesById.keys.sortedBy { it.value }.associateWith { notes.resolve(it).derived }
    return Catalog(CURRENT_FORMAT_VERSION, source.catalogVersion,
        source.instruments.sortedBy { it.id.value }, source.tunings.sortedBy { it.id.value },
        source.shapes.sortedBy { it.id.value }, source.chordStructures.sortedBy { it.id.value },
        source.chords.sortedBy { it.id.value }, source.chordShapes.sortedBy { it.id.value }, derived)
}

internal fun buildCatalogJson(source: CatalogSource): String =
    catalogJson.encodeToString(generateCatalog(source)) + "\n"

/** Generate completely before touching the destination; publish by atomic replacement. */
internal fun writeCatalog(source: CatalogSource, destination: Path) {
    val json = buildCatalogJson(source)
    val output = destination.toAbsolutePath()
    Files.createDirectories(output.parent)
    val temporary = Files.createTempFile(output.parent, ".catalog-", ".json")
    try {
        Files.writeString(temporary, json, Charsets.UTF_8)
        Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(temporary)
    }
}

fun main(args: Array<String>) {
    require(args.size == 1) { "Usage: <output.json>" }
    writeCatalog(catalogSource(), Path.of(args[0]))
    println("Wrote catalog to ${args[0]}")
}
