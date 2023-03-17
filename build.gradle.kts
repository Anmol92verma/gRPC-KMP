allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

buildscript {
    dependencies {
        classpath("org.antlr:antlr4-runtime:4.11.1")
    }
}

//https://youtrack.jetbrains.com/issue/KT-46200
plugins {
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    id("com.android.library") apply false
}

//Called by jitpack
tasks.register("publishToMavenLocal") {
    //No need to publish the plugin itself
    dependsOn(":grpc-multiplatform-lib:publishToMavenLocal")
}

// On Apple Silicon we need Node.js 16.0.0
// https://youtrack.jetbrains.com/issue/KT-49109
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class) {
    rootProject.the(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class).nodeVersion = "16.0.0"
}
