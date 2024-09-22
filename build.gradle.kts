plugins {
    java
    id("com.github.johnrengelman.shadow") version ("7.0.0")
}

group = "at.helpch.placeholderapi.expansion"
version = "2.7.2"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.3")
}

tasks {
    java {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    shadowJar {
        archiveFileName.set("PAPI-Expansion-Server-${project.version}.jar")
    }
}