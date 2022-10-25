import com.google.protobuf.gradle.*

plugins {
    kotlin("jvm")

    id("java")
    id("com.google.protobuf")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))

    api("com.google.protobuf:protobuf-kotlin:${Versions.JVM_PROTOBUF_VERSION}")
    api("com.google.protobuf:protobuf-java-util:${Versions.JVM_PROTOBUF_VERSION}")
    api("io.grpc:grpc-protobuf:${Versions.JVM_GRPC_VERSION}")
    api("io.grpc:grpc-stub:${Versions.JVM_GRPC_VERSION}")
    api("io.grpc:grpc-kotlin-stub:${Versions.JVM_GRPC_KOTLIN_VERSION}")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES_VERSION}")
}

sourceSets {
    main {
        proto {
            srcDirs("../src/commonMain/proto")
        }
        kotlin.srcDir(buildDir.resolve("generated/source/proto/main/grpc"))
        kotlin.srcDir(buildDir.resolve("generated/source/proto/main/grpckt"))
        kotlin.srcDir(buildDir.resolve("generated/source/proto/main/java"))
        kotlin.srcDir(buildDir.resolve("generated/source/proto/main/kotlin"))
    }
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.JVM_PROTOBUF_VERSION}"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.JVM_GRPC_VERSION}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.JVM_GRPC_KOTLIN_VERSION}:jdk7@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") {
                    option("lite")
                }
                id("grpckt") {
                    option("lite")
                }
            }

            it.builtins {
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}