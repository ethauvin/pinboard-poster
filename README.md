# [Pinboard](https://pinboard.in) Poster for Kotlin/Java

[![License (3-Clause BSD)](https://img.shields.io/badge/license-BSD%203--Clause-blue.svg?style=flat-square)](http://opensource.org/licenses/BSD-3-Clause) [![Build Status](https://travis-ci.org/ethauvin/pinboard-poster.svg?branch=master)](https://travis-ci.org/ethauvin/pinboard-poster) [![Dependency Status](https://www.versioneye.com/user/projects/591c0293b81f680038a784b3/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/591c0293b81f680038a784b3) [![Download](https://api.bintray.com/packages/ethauvin/maven/pinboard-poster/images/download.svg) ](https://bintray.com/ethauvin/maven/pinboard-poster/_latestVersion)

A small Kotlin/Java library for posting to [Pinboard](https://pinboard.in).

## Examples

### Kotlin

```kotlin

val poster = PinboardPoster("user:TOKEN")

poster.addPin("http://www.example.com/foo", "This is a test")
poster.deletePin("http:///www.example.com/bar")

```
[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/src/main/kotlin/net/thauvin/erik/pinboard/PinboardPoster.kt#L199)

### Java
```java

final PinboardPoster poster = new PinBboardPoster("user:TOKEN");

poster.addPin("http://www.example.com/foo", "This is a test");
poster.deletePin("http:///www.example.com/bar");
```
[View Example](https://github.com/ethauvin/pinboard-poster/blob/master/src/main/java/net/thauvin/erik/pinboard/JavaExample.java)

Your API authentication token is available on the [Pinboard settings page](https://pinboard.in/settings/password).

## Usage with Maven, Gradle and Kobalt

### Maven

To install and run from Maven, configure an artifact as follows:

```xml
<dependency>
    <groupId>net.thauvin.erik</groupId>
    <artifactId>pinboard-poster</artifactId>
    <version>0.9.0</version>
</dependency>
```

### Gradle

To install and run from Gradle, add the following to the build.gradle file:

```gradle
dependencies {
    compileOnly 'net.thauvin.erik:pinboard-poster:0.9.0'
}
```

### Kobalt

To install and run from Kobalt, add the following to the Build.kt file:

```gradle
dependencies {
    compile("net.thauvin.erik:pinboard-poster:0.9.0")
}
```

## Adding

The `addPin` function support all of the [Pinboard API parameters](https://pinboard.in/api/#posts_add):

```kotlin
poster.addPin(url = "http://www.example.com",
              description = "This is the title.",
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

```kotlin
with(poster.logger) {
    addHandler(ConsoleHandler().apply { level = Level.FINE })
    level = Level.FINE
}
```

or using a property file.

## API End Point

The API end point is automatically configured to `https://api.pinboard.in/v1/`. Since Pinboard uses the `del.ico.us` API, the library could potentially be used with another compatible service. To configure the API end point, use:

```kotlin
poster.apiEndPoint = "https://www.example.com/v1"
```