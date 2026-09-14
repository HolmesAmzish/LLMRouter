plugins {
    kotlin("jvm") version "2.3.21"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
