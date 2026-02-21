import java.io.FileInputStream
import java.util.Properties

val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}

fun ks(name: String): String? =
    keystoreProps.getProperty(name)?.takeIf { it.isNotBlank() }

val isReleaseTask = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // ✅ KSP compatível com Kotlin 2.2.0
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"

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
        versionCode = 18
        versionName = "1.1.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksFile = rootProject.file("keystore/desafio_lgico_25.jks")
            storeFile = ksFile

            val storePass = ks("storePassword") ?: ks("KEYSTORE_PASSWORD")
            val alias = ks("keyAlias") ?: ks("KEY_ALIAS") ?: "key0"
            val keyPass = ks("keyPassword") ?: ks("KEY_PASSWORD")

            if (isReleaseTask) {
                require(ksFile.exists()) { "Keystore não encontrado: ${ksFile.absolutePath}" }
                require(!storePass.isNullOrBlank()) { "Faltando storePassword (ou KEYSTORE_PASSWORD)" }
                require(!keyPass.isNullOrBlank()) { "Faltando keyPassword (ou KEY_PASSWORD)" }
            }

            storePassword = storePass
            keyAlias = alias
            keyPassword = keyPass
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            // AdMob TESTE
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
            resValue("string", "banner_ad_unit_id", "ca-app-pub-3940256099942544/9214589741")
            resValue("string", "admob_rewarded_ad_unit_id", "ca-app-pub-3940256099942544/5224354917")
        }

        release {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // AdMob REAL
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-4958622518589705~1887040194"
            resValue("string", "banner_ad_unit_id", "ca-app-pub-4958622518589705/1734854735")
            resValue("string", "admob_rewarded_ad_unit_id", "ca-app-pub-4958622518589705/3051012274")

            signingConfig = signingConfigs.getByName("release")
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

    // 🔧 força versões (mantenha só o necessário)
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
            force("androidx.room:room-runtime:2.6.1")
            force("androidx.room:room-common:2.6.1")
            force("androidx.sqlite:sqlite:2.4.0")
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

    // ✅ Firebase (só 1 BOM: a do TOML)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)

    // ✅ Google Login + Credential Manager
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")

    // ✅ Google Play Services
    implementation(libs.play.services.auth)

    // ✅ Ads: deixe APENAS UM jeito (via TOML)
    implementation(libs.play.services.ads)
    // ❌ NÃO usar play-services-ads-api
    // ❌ NÃO duplicar com "implementation("com.google.android.gms:play-services-ads:24.9.0")"

    // UI e animações
    implementation(libs.lottie)
    implementation(libs.konfetti.xml.v204)
    implementation(libs.glide)
    implementation(libs.coil)

    // Arquitetura
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.security:security-crypto:1.1.0")

    // HTTP e JSON
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Room
    implementation(libs.androidx.room.runtime)


    implementation ("com.google.firebase:firebase-firestore")
    ksp(libs.androidx.room.compiler)

    // Navegação e Browser
    implementation("androidx.browser:browser:1.6.0")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
