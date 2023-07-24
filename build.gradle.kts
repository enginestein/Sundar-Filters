plugins {
    kotlin("jvm") version "1.8.21"
}

group = "com.aryavart.sundar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    classpath 'com.android.tools.build:gradle:7.0.4'
    classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10'
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
    classpath("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}