package com.desafiolgico.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale
import androidx.core.content.edit

/**
 * 🔤 LanguageHelper — Gerencia a persistência e aplicação do idioma do aplicativo.
 */
object LanguageHelper {

    private const val PREFS_NAME = "LanguagePrefs"
    private const val KEY_LANGUAGE = "selected_language_code"

    // Idiomas disponíveis
    const val LANGUAGE_PORTUGUESE = "pt"
    const val LANGUAGE_ENGLISH = "en"

    /**
     * Salva o idioma selecionado (ex: "en", "pt") nas SharedPreferences.
     */
    fun setLanguage(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_LANGUAGE, languageCode)
            }
    }

    /**
     * Retorna o idioma salvo. Padrão = Português 🇧🇷.
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_PORTUGUESE) ?: LANGUAGE_PORTUGUESE
    }

    /**
     * Cria um novo Context configurado com o idioma correto.
     * Deve ser chamado em attachBaseContext().
     */
    fun wrap(context: Context): ContextWrapper {
        val languageCode = getLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        val localeList = LocaleList(locale)
        config.setLocales(localeList)

        val wrapped = context.createConfigurationContext(config)
        return ContextWrapper(wrapped)
    }

    /**
     * Aplica o idioma em tempo de execução (sem recriar toda a app).
     * Use após o usuário trocar o idioma nas configurações.
     */
    fun applyLanguage(context: Context) {
        wrap(context)
    }
    object LanguageHelper {

        private const val PREFS_NAME = "LanguagePrefs"
        private const val KEY_LANGUAGE = "selected_language_code"

        const val LANGUAGE_PORTUGUESE = "pt"
        const val LANGUAGE_ENGLISH = "en"

        fun setLanguage(context: Context, languageCode: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit {
                    putString(KEY_LANGUAGE, languageCode)
                }
        }

        fun getLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, LANGUAGE_PORTUGUESE) ?: LANGUAGE_PORTUGUESE
        }

        fun wrap(context: Context): ContextWrapper {
            val languageCode = getLanguage(context)
            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration)
            val localeList = LocaleList(locale)
            config.setLocales(localeList)

            val wrapped = context.createConfigurationContext(config)
            return ContextWrapper(wrapped)
        }

        fun applyLanguage(context: Context) {
            wrap(context)
        }

        // 🔹 NOVO: saber se o usuário já escolheu idioma pelo menos uma vez
        fun isLanguageSelected(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.contains(KEY_LANGUAGE)
        }
    }

}
