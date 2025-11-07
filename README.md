# Cast API

[![Maven Central](https://maven-badges.sml.io/sonatype-central/org.digitalmediaserver/cast-api/badge.svg?style=plastic)](https://maven-badges.sml.io/sonatype-central/org.digitalmediaserver/cast-api)

This is a Java library that lets you connect to and communicate with "cast applications" running on Google cast devices like ChromeCast dongles. It does so without using any of the Google frameworks, which are limited to Android, iOS and the Chrome browser. Since the Google frameworks are mostly closed-source and no documentation exists for the protocol, this library may not always do what the cast device expects. This implementation is the result of discoveries made by other open-source projects, the Chromium source code and trial and error.

This is a fork of [ChromeCast Java API v2](https://github.com/vitalidze/chromecast-java-api-v2) which has been heavily modified and extended for use with the [Digital Media Server](https://github.com/DigitalMediaServer/DigitalMediaServer/) project. Discussions with the author of the original project about implementing the necessary changes lead nowhere, which resulted in the creation of this fork. The initial plan was to address some fundamental problems like the lack of support for overriding classes, the lack of control of the network side of things and the multi-threading handling. After starting the work, it soon became clear that much more needed to be done. Quite a few extra commands and data structures have been implemented, but not all that are likely to exist according to other sources are implemented. In particular, everything related to ads and most of the queue handling has not been implemented because there's currently no need for this for the [Digital Media Server](https://github.com/DigitalMediaServer/DigitalMediaServer/) project. The queue handling might be implemented in the future, otherwise pull requests are always welcome.

No explicit documentation for how to use this library exists, but every piece of code has JavaDocs. Using these, in combination with [Google's documentation for their proprietary frameworks](https://developers.google.com/cast/docs/reference), should make using it pretty straight forward.

## Include

### Maven

Ths library is available at Maven Central, simply add the following in your project's `pom.xml` file:

```xml
<dependencies>
...
  <dependency>
    <groupId>org.digitalmediaserver</groupId>
    <artifactId>cast-api</artifactId>
    <version>RELEASE</version>
  </dependency>
...
</dependencies>
```

### Manual

If you don't use a build automation tool like Maven, you have to provide the library's dependencies manually, please make sure to use compatible versions. The applicable versions are listed in [pom.xml](pom.xml) under `<dependencies>`. Please note the `<scope>`, dependencies scoped with `compile` or `test` are only required to build the library, not to use it.

## Discovery

The library includes [CastDeviceMonitor](src/main/java/org/digitalmediaserver/cast/CastDeviceMonitor.java) for easy discovery of cast devices. The class isn't very sophisticated, and is only meant for very basic projects or to get up and running quickly. If you need more control over the network configuration, or the project already has a mDNS instance running, it's recommended that the project takes care of mDNS and uses the library to create `CastDevice` instances when devices that provide `_googlecast._tcp.local.` are found.
