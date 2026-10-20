plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    testImplementation(kotlin("test-junit5"))
    testImplementation("io.vertx:vertx-json-schema:5.2.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

tasks.test {
    useJUnitPlatform()
}

sourceSets.test {
    resources.srcDir("schema")
}

val validateCatalog by tasks.registering {
    group = "verification"
    description = "Run tests, including catalog acceptance and JSON Schema checks."
    dependsOn(tasks.test)
}

tasks.register<JavaExec>("buildCatalog") {
    dependsOn(tasks.test)
    group = "build"
    description = "Validate and write the deterministic shared JSON catalog."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("chords.processing.BuildCatalogKt")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    args(layout.buildDirectory.file("catalog/catalog.json").get().asFile.absolutePath)
}

tasks.check { dependsOn(validateCatalog) }
