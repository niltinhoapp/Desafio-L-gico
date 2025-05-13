@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        // ESSENCIAL: Permite que o Gradle encontre plugins do Android e Google Services.
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // ESSENCIAL: Garante que as dependências (como Firebase BOM) sejam resolvidas.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Se você usa o Jitpack
    }
}

rootProject.name = "Desafio Lógico"
include(":app")
