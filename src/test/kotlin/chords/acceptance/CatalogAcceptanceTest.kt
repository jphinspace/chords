package chords.acceptance

import chords.catalog.*
import chords.processing.*
import chords.schema.*
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogAcceptanceTest {
    @Test
    fun `authored catalog and exported JSON satisfy acceptance rules`() {
        val source = catalogSource()
        val accepted = acceptCatalog(source)
        assertCatalogComplete(accepted)
        val json = buildCatalogJson(source)
        validateSchema(json)
        assertEquals(accepted, catalogJson.decodeFromString<Catalog>(json))
    }

    @Test
    fun `extension fixtures satisfy acceptance rules`() {
        val source = extendedCatalogFixture()
        acceptCatalog(source)
        validateSchema(buildCatalogJson(source))
    }
}
