<div align="center">

# Ktor microtools

[![CI](https://github.com/symbiolab/ktor-microtools/actions/workflows/ci.yaml/badge.svg)](https://github.com/symbiolab/ktor-microtools/actions/workflows/ci.yaml)

A collection of useful tools accompanying [Ktor](https://ktor.io).

[Gradle](#gradle) •
[Maven](#maven)

</div>

# Installation

## Gradle

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/symbiolab/ktor-microtools")
    }
}
```

```kotlin
dependencies {
    implementation("de.symbiolab.ktor-microtools:<version>")
}
```

## Maven

```xml
<dependency>
  <groupId>de.symbiolab</groupId>
  <artifactId>ktor-microtools</artifactId>
  <version><!-- version --></version>
</dependency>
```

```shell
mvn install
```
