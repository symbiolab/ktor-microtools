object Dependencies {
    // application dependencies
    const val Kotlin = "1.7.0"
    const val Ktor = "2.0.3"
    const val KotlinLogging = "2.1.23"
    const val Logback = "1.2.11"
    const val Kotest = "5.3.2"
    const val KotestKtor = "4.4.3"

    // gradle plugins
    const val Spotless = "6.7.2"
    const val Versions = "0.42.0"
    const val Shadow = "7.1.2"
    const val DockerCompose = "0.16.5"

    // configurations

    // dependency update doesn't catch this one.
    // See https://github.com/pinterest/ktlint/releases/latest for updates
    const val Ktlint = "0.45.2"
}
