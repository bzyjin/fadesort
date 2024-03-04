plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    api(libs.commons.math3)
}

group = "com.github.bzyjin"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(19)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    testLogging {
        showStandardStreams = true
    }
}
