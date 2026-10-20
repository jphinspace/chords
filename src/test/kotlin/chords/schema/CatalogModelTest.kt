package chords.schema

import io.vertx.core.json.JsonObject
import io.vertx.json.schema.Draft
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.JsonSchemaOptions
import io.vertx.json.schema.Validator
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogModelTest {
    private val schema = Validator.create(
        JsonSchema.of(JsonObject(requireNotNull(javaClass.getResourceAsStream("/catalog.schema.json"))
            .bufferedReader().use { it.readText() })),
        JsonSchemaOptions().setDraft(Draft.DRAFT202012).setBaseUri("https://chords.invalid/catalog.schema.json"),
    )

    private fun fixture(): Catalog {
        val guitar = Instrument(InstrumentId("guitar-6"), "Six-string guitar", "guitar", 6)
        val tuning = Tuning(TuningId("guitar-e"), guitar.id, "E standard", listOf(
            Pitch("E", 4, 64), Pitch("B", 3, 59), Pitch("G", 3, 55),
            Pitch("D", 3, 50), Pitch("A", 2, 45), Pitch("E", 2, 40),
        ))
        val shape = Shape(
            ShapeId("open-g"), 6,
            listOf(StringInstruction.Offset(3), StringInstruction.Open, StringInstruction.Open,
                StringInstruction.Open, StringInstruction.Offset(2), StringInstruction.Offset(3)),
            PermittedPositions.Fixed(listOf(0)),
        )
        val alternative = shape.copy(
            id = ShapeId("open-g-muted"),
            strings = shape.strings.toMutableList().also { it[4] = StringInstruction.Muted }.toList(),
        )
        val barre = Shape(
            ShapeId("major-barre"), 6,
            listOf(0, 0, 1, 2, 2, 0).map { StringInstruction.Offset(it) },
            PermittedPositions.Range(),

        )
        val type = ChordStructure(ChordStructureId("major"), "Major", listOf(
            ChordInterval(1, 0), ChordInterval(3, 4), ChordInterval(5, 7),
        ))
        val chord = Chord(ChordId("g-major"), "G", type.id, "G major", listOf("G"))
        val chordShape = ChordShape(
            ChordShapeId("g-open"), tuning.id, shape.id, 0, chord.id,
            tags = listOf("open"),
        )
        val other = chordShape.copy(id = ChordShapeId("g-open-muted"), shapeId = alternative.id)
        val notes = DerivedNotes(listOf(3, 0, 0, 0, 2, 3), listOf(
            Pitch("G", 4, 67), Pitch("B", 3, 59), Pitch("G", 3, 55),
            Pitch("D", 3, 50), Pitch("B", 2, 47), Pitch("G", 2, 43),
        ))
        return Catalog(1, "0.1.0", listOf(guitar), listOf(tuning), listOf(shape, alternative, barre),
            listOf(type), listOf(chord), listOf(chordShape, other),
            mapOf(chordShape.id to notes, other.id to DerivedNotes(
                listOf(3, 0, 0, 0, null, 3),
                notes.notes.toMutableList().also { it[4] = null }.toList(),
            )))
    }

    @Test
    fun `complete model round trips and satisfies schema`() {
        val original = fixture()
        val encoded = catalogJson.encodeToString(original)
        assertEquals(original, catalogJson.decodeFromString<Catalog>(encoded))
        val result = schema.validate(JsonObject(encoded))
        assertTrue(result.valid, result.toString())
        val tree = catalogJson.parseToJsonElement(encoded).jsonObject
        assertEquals("0.1.0", tree.getValue("catalogVersion").jsonPrimitive.content)
        assertEquals(setOf("g-open", "g-open-muted"), tree.getValue("derivedNotes").jsonObject.keys)
        assertFalse(encoded.contains("chords.schema"))
        assertEquals(original.chordShapes[0].chordId, original.chordShapes[1].chordId)
    }

    @Test
    fun `instructions have stable exclusive tags`() {
        for (instruction in listOf(StringInstruction.Muted, StringInstruction.Open, StringInstruction.Offset(0))) {
            val text = catalogJson.encodeToString<StringInstruction>(instruction)
            assertEquals(instruction, catalogJson.decodeFromString<StringInstruction>(text))
        }
        assertFailsWith<SerializationException> {
            catalogJson.decodeFromString<StringInstruction>("""{"kind":"open","offset":2}""")
        }
        assertFailsWith<SerializationException> {
            catalogJson.decodeFromString<StringInstruction>("""{"kind":"unknown"}""")
        }
    }

    @Test
    fun `IDs serialize as plain strings`() {
        assertEquals("\"g-major\"", catalogJson.encodeToString(ChordId("g-major")))
        assertEquals(ChordId("g-major"), catalogJson.decodeFromString<ChordId>("\"g-major\""))
    }

    @Test
    fun `range defaults decode`() {
        assertEquals(PermittedPositions.Range(), catalogJson.decodeFromString<PermittedPositions>("""{"kind":"range"}"""))
    }

    @Test
    fun `schema rejects malformed wire records`() {
        val valid = JsonObject(catalogJson.encodeToString(fixture()))
        val mutations: List<(JsonObject) -> Unit> = listOf(
            { it.put("formatVersion", 2) },
            { it.put("catalogVersion", "latest") },
            { it.put("unknown", true) },
            { it.remove("tunings") },
            { it.getJsonArray("instruments").getJsonObject(0).put("stringCount", 0) },
            { it.getJsonArray("tunings").getJsonObject(0).getJsonArray("openStrings").getJsonObject(0).put("midi", 128) },
            { it.getJsonArray("shapes").getJsonObject(0).getJsonArray("strings").getJsonObject(0).put("offset", -1) },
            { it.getJsonArray("shapes").getJsonObject(0).getJsonArray("strings").getJsonObject(1).put("offset", 1) },
            { it.getJsonArray("chordShapes").getJsonObject(0).put("id", "Bad ID") },
            { it.getJsonArray("chordShapes").getJsonObject(0).put("position", 2147483648L) },
            { it.getJsonArray("chordShapes").getJsonObject(0).put("id", "g-open\n") },
            { it.put("catalogVersion", "0.1.0\n") },
        )
        mutations.forEach { mutate ->
            val broken = valid.copy()
            mutate(broken)
            assertFalse(schema.validate(broken).valid, broken.toString())
        }
    }
}
