plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    // Referencing the JAR from the app module to avoid moving files around
    implementation(files("../app/libs/web-untis-api-1.2.jar"))
    implementation("org.json:json:20231013")
}

tasks.shadowJar {
    // Relocate org.json to avoid conflict with Android's internal JSON library
    relocate("org.json", "shaded.org.json")
    
    // Ensure the output file has a predictable name if needed, but default is fine
    archiveClassifier.set("") // This makes the shadow jar the default artifact
}

// Configure the module to publish the Shadow JAR as its main output
configurations {
    apiElements {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
    runtimeElements {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
}
