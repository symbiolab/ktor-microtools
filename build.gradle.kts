import com.diffplug.gradle.spotless.SpotlessApply
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Dependencies.Kotlin
    kotlin("plugin.serialization") version Dependencies.Kotlin

    id("com.diffplug.spotless") version Dependencies.Spotless
    id("com.github.johnrengelman.shadow") version Dependencies.Shadow
    id("com.github.ben-manes.versions") version Dependencies.Versions
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-server-core:${Dependencies.Ktor}")
    implementation("io.ktor:ktor-server-auth:${Dependencies.Ktor}")
    implementation("io.ktor:ktor-server-auth-jwt:${Dependencies.Ktor}")

    implementation("ch.qos.logback:logback-classic:${Dependencies.Logback}")
    implementation("io.github.microutils:kotlin-logging:${Dependencies.KotlinLogging}")

    testImplementation("io.ktor:ktor-server-test-host:${Dependencies.Ktor}")

    testImplementation("io.kotest:kotest-runner-junit5:${Dependencies.Kotest}")
    testImplementation("io.kotest:kotest-assertions-core:${Dependencies.Kotest}")
    testImplementation("io.kotest:kotest-assertions-json:${Dependencies.Kotest}")
    testImplementation("io.kotest:kotest-assertions-ktor:${Dependencies.KotestKtor}")
}

defaultTasks("clean", "test")

spotless {
    kotlin {
        ktlint(Dependencies.Ktlint)
        toggleOffOn()
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    kotlinGradle {
        ktlint(Dependencies.Ktlint)
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }

    compileKotlin {
        dependsOn(withType<SpotlessApply>())
    }

    test {
        useJUnitPlatform()
        testLogging {
            events = setOf(PASSED, SKIPPED, FAILED, STANDARD_ERROR, STANDARD_OUT)
        }

        outputs.upToDateWhen { false }
    }
}
