# [Pinboard](https://pinboard.in) Poster for Kotlin/Java

[![License (3-Clause BSD)](https://img.shields.io/badge/license-BSD%203--Clause-blue.svg?style=flat-square)](http://opensource.org/licenses/BSD-3-Clause) [![release](https://img.shields.io/github/release/ethauvin/pinboard-poster.svg)](https://github.com/ethauvin/pinboard-poster/releases/latest) [![Download](https://api.bintray.com/packages/ethauvin/maven/pinboard-poster/images/download.svg)](https://bintray.com/ethauvin/maven/pinboard-poster/_latestVersion)  
[![Known Vulnerabilities](https://snyk.io/test/github/ethauvin/pinboard-poster/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/ethauvin/pinboard-poster?targetFile=pom.xml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ethauvin_pinboard-poster&metric=alert_status)](https://sonarcloud.io/dashboard?id=ethauvin_pinboard-poster) [![Build Status](https://travis-ci.com/ethauvin/pinboard-poster.svg?branch=master)](https://travis-ci.com/ethauvin/pinboard-poster) [![CircleCI](https://circleci.com/gh/ethauvin/pinboard-poster/tree/master.svg?style=shield)](https://circleci.com/gh/ethauvin/pinboard-poster/tree/master)

A small Kotlin/Java/Android library for posting to [Pinboard](https://pinboard.in).

## Examples

### Kotlin

```kotlin

val poster = PinboardPoster("user:TOKEN")

poster.addPin("http://www.example.com/foo", "This is a test")
poster.deletePin("http:///www.example.com/bar")

```
[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/kotlin/src/main/kotlin/net/thauvin/erik/pinboard/samples/KotlinExample.kt)

### Java
```java

final PinboardPoster poster = new PinBboardPoster("user:TOKEN");

poster.addPin("http://www.example.com/foo", "This is a test");
poster.deletePin("http:///www.example.com/bar");
```
[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/java/src/main/java/net/thauvin/erik/pinboard/samples/JavaExample.java)

Your API authentication token is available on the [Pinboard settings page](https://pinboard.in/settings/password).

## Usage with Gradle, Maven, etc.

To install and run from Gradle, add the following to the build.gradle file:

```gradle
dependencies {
    compile 'net.thauvin.erik:pinboard-poster:1.0.2'
}
```
[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/java/build.gradle)  
[View Kotlin DSL Example](https://github.com/ethauvin/pinboard-poster/blob/master/samples/kotlin/build.gradle.kts)

Instructions for using with Maven, Ivy, etc. can be found on [Maven Central](https://search.maven.org/artifact/net.thauvin.erik/pinboard-poster/1.0.2/jar).

## Adding

The `addPin` function support all of the [Pinboard API parameters](https://pinboard.in/api/#posts_add):

```kotlin
poster.addPin(url = "http://www.example.com",
              description = "This is the title",
              extended = "This is the extended description.",
              tags = "tag1 tag2 tag3",
              dt = "2010-12-11T19:48:02Z",
              replace = true,
              shared = true,
              toRead = false)
```

`url` and `description` are required.

It returns `true` if the bookmark was added successfully, `false` otherwise.

## Deleting

The `deletePin` function support all of the [Pinboard API parameters](https://pinboard.in/api/#posts_delete):

```kotlin
poster.deletePin(url = "http://www.example.com/")
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
