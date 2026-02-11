import org.gradle.kotlin.dsl.libs
import java.io.FileInputStream
import java.util.Properties

val keystoreProps = Properties()
val keystorePropsFile = rootProject.file("keystore.properties")
if (keystorePropsFile.exists()) {
    keystoreProps.load(FileInputStream(keystorePropsFile))
}

fun ks(name: String): String? =
    keystoreProps.getProperty(name)?.takeIf { it.isNotBlank() }

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
        versionCode = 14
        versionName = "1.1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }
    signingConfigs {
        create("release") {
            val storeFilePath = ks("storeFile") ?: "../keystore/desafio_lgico_25.jks"
            val storePass = ks("storePassword") ?: ks("KEYSTORE_PASSWORD")
            val alias = ks("keyAlias") ?: ks("KEY_ALIAS")
            val keyPass = ks("keyPassword") ?: ks("KEY_PASSWORD")

            require(!storePass.isNullOrBlank()) { "keystore.properties: faltou storePassword" }
            require(!alias.isNullOrBlank()) { "keystore.properties: faltou keyAlias" }
            require(!keyPass.isNullOrBlank()) { "keystore.properties: faltou keyPassword" }

            storeFile = file(storeFilePath)     // ✅ seu caminho atual funciona
            storePassword = storePass
            keyAlias = alias
            keyPassword = keyPass
        }
    }


    buildTypes {
        debug {
          //  applicationIdSuffix = ".debug"
           // versionNameSuffix = "-debug"

            // (opcional) trocar nome do app no launcher
          //  resValue("string", "app_name", "Desafio Lógico (Debug)")

            isMinifyEnabled = false
            isShrinkResources = false

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

            // ✅ App ID REAL (release) — o seu:
            manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-4958622518589705~1887040194"

            // (Opcional) IDs em strings por buildType (produção)
            resValue("string", "banner_ad_unit_id", "ca-app-pub-4958622518589705/1734854735")

            resValue("string", "admob_rewarded_ad_unit_id", "ca-app-pub-4958622518589705/3051012274")

            signingConfig = signingConfigs.getByName("release") // só se existir!
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

    implementation("androidx.security:security-crypto:1.1.0")
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
