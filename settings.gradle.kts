rootProject.name = "brady"

pluginManagement {
    includeBuild("gradleBuild")
    repositories {
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include("core")
include("core-pgm")
include("deps")
include("bot")
include("tools")
include("share")
include("broxy")
include("cps")
