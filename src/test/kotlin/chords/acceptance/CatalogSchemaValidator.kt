package chords.acceptance


import io.vertx.core.json.JsonObject
import io.vertx.json.schema.Draft
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.JsonSchemaOptions
import io.vertx.json.schema.Validator
internal fun validateSchema(json: String) {
    val schema = requireNotNull(object {}.javaClass.getResourceAsStream("/catalog.schema.json")) {
        "catalog.schema.json resource missing"
    }.bufferedReader(Charsets.UTF_8).use { it.readText() }
    val validator = Validator.create(JsonSchema.of(JsonObject(schema)),
        JsonSchemaOptions().setDraft(Draft.DRAFT202012).setBaseUri("https://chords.invalid/catalog.schema.json"))
    val result = validator.validate(JsonObject(json))
    require(result.valid) { "catalog schema: ${result.toString()}" }
}
