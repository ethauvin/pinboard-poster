# [Pinboard](https://pinboard.in) Poster for Kotlin, Java and Android

[![License (3-Clause BSD)](https://img.shields.io/badge/license-BSD%203--Clause-blue.svg?style=flat-square)](https://opensource.org/licenses/BSD-3-Clause) [![Release](https://img.shields.io/github/release/ethauvin/pinboard-poster.svg)](https://github.com/ethauvin/pinboard-poster/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/net.thauvin.erik/pinboard-poster.svg?color=blue)](https://central.sonatype.com/artifact/net.thauvin.erik/pinboard-poster)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ethauvin_pinboard-poster&metric=alert_status)](https://sonarcloud.io/dashboard?id=ethauvin_pinboard-poster)
[![GitHub CI](https://github.com/ethauvin/pinboard-poster/actions/workflows/gradle.yml/badge.svg)](https://github.com/ethauvin/pinboard-poster/actions/workflows/gradle.yml)
[![CircleCI](https://circleci.com/gh/ethauvin/pinboard-poster/tree/master.svg?style=shield)](https://circleci.com/gh/ethauvin/pinboard-poster/tree/master)

A small library for posting to [Pinboard](https://pinboard.in).

## Examples

### Kotlin

```kotlin

val poster = PinboardPoster("user:TOKEN")

poster.addPin("https://www.example.com/foo", "This is a test")
poster.addPin("https://examples.com", "This is a test", tags = arrayOf("foo", "bar"))
poster.deletePin("https:///www.example.com/bar")

```

[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/kotlin/src/main/kotlin/net/thauvin/erik/pinboard/samples/KotlinExample.kt)

### Java

```java

final PinboardPoster poster = new PinBboardPoster("user:TOKEN");

poster.addPin("https://www.example.com/foo", "This is a test");
poster.addPin(new PinConfig.Builder()
                .url("https://example.com")
                .description("This is a test")
                .tags("foo", "bar")
                .build());
poster.deletePin("https:///www.example.com/bar");
```

[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/java/src/main/java/net/thauvin/erik/pinboard/samples/JavaExample.java)

Your API authentication token is available on the [Pinboard settings page](https://pinboard.in/settings/password).

## Usage with Gradle, Maven, etc.

To install and run from Gradle, add the following to the build.gradle file:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    compile 'net.thauvin.erik:pinboard-poster:1.0.4'
}
```

[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/java/build.gradle)  
[View Kotlin DSL Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/kotlin/build.gradle.kts)

Instructions for using with Maven, Ivy, etc. can be found on [Maven Central](https://central.sonatype.com/artifact/net.thauvin.erik/pinboard-poster).

## Adding

The `addPin` function support all of the [Pinboard API parameters](https://pinboard.in/api/#posts_add):

```kotlin
import java.time.ZonedDateTime

poster.addPin(
    url = "https://www.example.com",
    description = "This is the title",
    extended = "This is the extended description.",
    tags = arrayOf("tag1", "tag2", "tag3"),
    dt = ZonedDateTime.now(),
    replace = true,
    shared = true,
    toRead = false
)
```

`url` and `description` are required.

It returns `true` if the bookmark was added successfully, `false` otherwise.

## Deleting

The `deletePin` function support all of the [Pinboard API parameters](https://pinboard.in/api/#posts_delete):

```kotlin
poster.deletePin(url = "https://www.example.com/")
```

It returns `true` if the bookmark was deleted successfully, `false` otherwise.

## Logging

The library used [`java.util.logging`](https://docs.oracle.com/javase/8/docs/api/java/util/logging/package-summary.html) to log errors. Logging can be configured as follows:

#### Kotlin

```kotlin
with(poster.logger) {
    addHandler(ConsoleHandler().apply { level = Level.FINE })
    level = Level.FINE
}
```

#### Java

```java
final ConsoleHandler consoleHandler = new ConsoleHandler();
consoleHandler.setLevel(Level.FINE);
final Logger logger = poster.getLogger();
logger.addHandler(consoleHandler);
logger.setLevel(Level.FINE);
```

or using a logging properties file.

## API Authentication Token

The token can also be located in a [properties file](https://en.wikipedia.org/wiki/.properties) or environment variable.

### Local Property

For example, using the default `PINBOARD_API_TOKEN` key value from a `local.properties` file:

```ini
# local.properties
PINBOARD_API_TOKEN=user\:TOKEN
```

```kotlin
val poster = PinboardPoster(Paths.get("local.properties"))
```

or by specifying your own key:

```ini
# my.properties
my.api.key=user\:TOKEN
```

```kotlin
val poster = PinboardPoster(Paths.get("my.properties"), "my.api.key")
```

or even specifying your own property:

```kotlin
val p = Properties()
p.setProperty("api.key", "user:TOKEN")

val poster = PinboardPoster(p, "api.key")
```

_In all cases, the value of the `PINBOARD_API_TOKEN` environment variable is used by default if the specified property is invalid or not found._

### Environment Variable

If no arguments are passed to the constructor, the value of the `PINBOARD_API_TOKEN` environment variable will be used, if any.

```sh
export PINBOARD_API_TOKEN="user:TOKEN"
```

```kotlin
val poster = PinboardPoster()
```

## API End Point

The API end point is automatically configured to `https://api.pinboard.in/v1/`. Since Pinboard uses the `del.ico.us` API, the library could potentially be used with another compatible service. To configure the API end point, use:

```kotlin
poster.apiEndPoint = "https://www.example.com/v1"
```
