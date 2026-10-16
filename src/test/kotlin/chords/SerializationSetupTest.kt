package chords

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationSetupTest {
    @Serializable
    private data class Fixture(val name: String, val frets: List<Int?>)

    @Test
    fun `serialization plugin and JSON runtime preserve muted strings and fret order`() {
        val fixture = Fixture("G major", listOf(3, 0, 0, 0, null, 3))
        val encoded = Json.encodeToString(fixture)
        assertEquals("""{"name":"G major","frets":[3,0,0,0,null,3]}""", encoded)
        assertEquals(fixture, Json.decodeFromString<Fixture>(encoded))
    }
}
