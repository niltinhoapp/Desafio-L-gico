import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.libs

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
    alias(libs.plugins.google.gms.google.services) // Plugin KSP
}

android {
    namespace = "com.desafiologico"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.desafiolgico"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildToolsVersion = "34.0.0"

    buildFeatures {
        viewBinding = true
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }
}


dependencies {
    // Bibliotecas essenciais do Android
    implementation(libs.androidx.core.ktx)         // Extensões Kotlin para Android
    implementation(libs.androidx.appcompat)       // Compatibilidade de recursos Android
    // implementation(libs.androidx.constraintlayout) // Layouts de tela mais avançados
    implementation(libs.androidx.activity)        // API para gerenciar Activities
    implementation(libs.androidx.recyclerview)    // Versão mais recente disponível
    implementation(libs.androidx.core.ktx.v1160) // Extensões Kotlin para Android

    // Ou a versão mais recente
    implementation(libs.material.v1110)


    implementation(libs.glide)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database.ktx)
    annotationProcessor(libs.compiler)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.lottie)
    implementation(libs.konfetti)
    implementation(libs.androidx.constraintlayout)

    // Material Design (Escolha uma versão - Material 2 ou Material 3)
    //implementation(libs.material)                 // Material Design 2
    implementation(libs.androidx.material3.android) // Material Design 3 (Descomente caso necessário)
    // Bibliotecas Firebase e serviços
    //  implementation(libs.firebase.inappmessaging)  // Mensagens no aplicativo do Firebase
    implementation(libs.play.services.ads) // Google Play Services para Ads
    // Gerenciamento de ciclo de vida e corrotinas
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)


    // Networking e serialização
    implementation(libs.retrofit)                 // Retrofit para chamadas de API
    implementation(libs.converter.gson)           // Conversor Gson para Retrofit
    // Manipulação de imagens e banco de dados
    implementation(libs.coil)                     // Carregamento de imagens leve e eficiente
    implementation(libs.androidx.room.runtime)    // Biblioteca Room para persistência de dados
    ksp(libs.androidx.room.compiler)              // Compilador Room com KSP (verifique a necessidade)
    // Testes unitários e instrumentados
    testImplementation(libs.junit)                // Testes unitários
    androidTestImplementation(libs.androidx.junit)  // Testes instrumentados JUnit
    androidTestImplementation(libs.androidx.espresso.core) // Testes de UI com Espresso

//        implementation(libs.leonidslib)

}

