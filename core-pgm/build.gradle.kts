plugins {
    id("gradleBuild.shadow")
}

dependencies {
    compileOnlyApi(project(":core"))
    compileOnlyApi(libs.tc.oc.pgm.core)
}
