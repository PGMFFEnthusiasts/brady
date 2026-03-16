import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure

plugins {
    id("gradleBuild.shadow")
}

dependencies {
    compileOnlyApi(project(":core-pgm"))
}

val versionDetails: Closure<VersionDetails> by extra
val fullVersion: String = versionDetails().gitHash ?: "unknown"

tasks {
    processResources {
        outputs.upToDateWhen { false }
        val properties = mutableMapOf("fullVersion" to fullVersion)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(properties)
        }
    }
}
