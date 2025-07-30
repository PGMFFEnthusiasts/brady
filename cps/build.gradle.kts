plugins {
    id("gradleBuild.shadow")
}

dependencies {
    compileOnlyApi(project(":core"))
}
