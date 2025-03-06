buildscript {
    repositories {
        google()
        mavenCentral()
    }

}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.jetbrains.kotlin) apply false
}