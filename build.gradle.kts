@file:Suppress("DEPRECATION")

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    // Plugins globais (não aplicados diretamente aqui)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false

    // Crashlytics (para upload automático de mapping.txt e relatórios de falha)
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // 🔥 Compatível com Crashlytics + Play Services
        classpath("com.google.gms:google-services:4.4.2")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.6")
    }
}



tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
