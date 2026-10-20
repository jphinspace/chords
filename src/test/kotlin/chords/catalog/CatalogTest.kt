package chords.catalog

import chords.processing.catalogSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogTest {
    @Test
    fun `power chord names include symbolic and enharmonic aliases`() {
        val sharp = catalogSource().chords.single { it.id.value == "c-sharp-power" }
        assertEquals("C♯ power chord", sharp.name)
        assertTrue(sharp.aliases.containsAll(listOf("C♯5", "C#5", "D♭5", "Db5")))
    }
}
