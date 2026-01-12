import org.gradle.kotlin.dsl.libs

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Room (KSP)
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"

    // Firebase
    alias(libs.plugins.google.gms.google.services)
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.desafiolgico"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.desafiolgico"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }

        release {
            // (como você deixou) sem R8/shrink
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true

        // ✅ garante geração do BuildConfig (para BuildConfig.DEBUG existir)
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }

    // 🔧 (mantive como você tinha) força versões para evitar conflitos
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
            force("androidx.room:room-runtime:2.6.1")
            force("androidx.room:room-common:2.6.1")
            force("androidx.sqlite:sqlite:2.4.0")
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        }
    }
}

dependencies {
    // Base Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material.v1130)
    implementation(libs.androidx.material3.android)

    // 🔥 Firebase (BOM + módulos)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-crashlytics")

    // Login Google + Credential Manager
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")

    // UI e animações
    implementation(libs.lottie)
    implementation(libs.konfetti.xml.v204)
    implementation(libs.glide)

    // Arquitetura
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // HTTP e JSON
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Google Play Services
    implementation(libs.play.services.ads)
    implementation(libs.play.services.auth)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Navegação e Browser
    implementation("androidx.browser:browser:1.6.0")

    // Imagens
    implementation(libs.coil)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
