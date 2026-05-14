import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
}

group = "com.stemlab"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Ktor (HTTP client for OpenAI mode)
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")

    // Tests
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

compose.desktop {
    application {
        mainClass = "com.stemlab.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "StemAgentLab"
            packageVersion = "1.0.0"
            description = "Stem Agent Lab — controlled agent specialization loop"
            copyright = "© 2026 JetBrains Internship Project"

            macOS {
                bundleID = "com.stemlab.StemAgentLab"
                dockName = "Stem Agent Lab"
            }
            windows {
                menuGroup = "Stem Agent Lab"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
