plugins {
    id("io.freefair.lombok") version "9.1.0" apply false
    id("com.gradleup.shadow") version "9.0.0-rc1" apply false
    id("java-library")
}

allprojects {
    group = "com.craftlyworks.votely"
    version = "1.0-RELEASE"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}