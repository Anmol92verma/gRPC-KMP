buildscript {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
        gradlePluginPortal()
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.8.10"))
        classpath(kotlin("serialization", version = "1.8.10"))

        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    }
}

allprojects {
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}