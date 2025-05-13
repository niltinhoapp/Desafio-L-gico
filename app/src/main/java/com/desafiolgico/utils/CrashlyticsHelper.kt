package com.desafiolgico.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import com.google.firebase.crashlytics.FirebaseCrashlytics
// UMP (opcional)
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import java.util.Locale

object CrashlyticsHelper {

    /** Chame no Application.onCreate() */

    fun setupCrashlytics(context: Context, enableInDebug: Boolean = false) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val enabled = (!isDebuggable || enableInDebug)

        crashlytics.setCrashlyticsCollectionEnabled(enabled)

        try {
            // ---------- User ----------
            val name  = UserManager.getNomeUsuario(context) ?: "Desconhecido"
            val email = UserManager.getEmailUsuario(context) ?: "sem-email"

            // ✅ Evita PII: use um id interno estável
            crashlytics.setUserId(GameDataManager.currentUserId)

            crashlytics.setCustomKey("user_name", name)
            crashlytics.setCustomKey("has_email", email != "sem-email")

            // ---------- App ----------
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName ?: "N/A"
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode.toString()
                else @Suppress("DEPRECATION") pInfo.versionCode.toString()

            crashlytics.setCustomKey("app_version_name", versionName)
            crashlytics.setCustomKey("app_version_code", versionCode)

            // ✅ Correto
            crashlytics.setCustomKey("build_type", if (isDebuggable) "debug" else "release")
            crashlytics.setCustomKey("crashlytics_enabled", enabled)

            // ---------- Device ----------
            crashlytics.setCustomKey("android_api", Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            crashlytics.setCustomKey("locale", Locale.getDefault().toLanguageTag())

            // ---------- Consent (UMP) opcional ----------
            runCatching {
                val ci = UserMessagingPlatform.getConsentInformation(context)
                val status = when (ci.consentStatus) {
                    ConsentInformation.ConsentStatus.UNKNOWN      -> "UNKNOWN"
                    ConsentInformation.ConsentStatus.REQUIRED     -> "REQUIRED"
                    ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
                    ConsentInformation.ConsentStatus.OBTAINED     -> "OBTAINED"
                    else -> "OTHER"
                }
                crashlytics.setCustomKey("ump_consent_status", status)
                crashlytics.setCustomKey("ump_form_available", ci.isConsentFormAvailable)
            }

            // ---------- Estado de jogo ----------
            runCatching { crashlytics.setCustomKey("coins", CoinManager.getCoins(context)) }
            runCatching { crashlytics.setCustomKey("current_level", GameDataManager.getUltimoNivelNormal(context) ?: "UNKNOWN") }
            runCatching { crashlytics.setCustomKey("current_streak", GameDataManager.currentStreak) }

        } catch (e: Exception) {
            crashlytics.log("setupCrashlytics error: ${e.localizedMessage}")
        }
    }


    // --------- Utilidades ---------
    fun log(message: String) {
        FirebaseCrashlytics.getInstance().log(message)
    }

    fun recordException(e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
    }

    // --------- Breadcrumbs úteis ---------
    fun breadcrumbAd(event: String, placement: String? = null, extra: String? = null) {
        val msg = buildString {
            append("[AD] ").append(event)
            placement?.let { append(" | placement=").append(it) }
            extra?.let { append(" | ").append(it) }
        }
        log(msg)
    }

    fun breadcrumbGame(event: String, level: String? = null, qIndex: Int? = null, correct: Boolean? = null) {
        val msg = buildString {
            append("[GAME] ").append(event)
            level?.let   { append(" | level=").append(it) }
            qIndex?.let  { append(" | question=").append(it) }
            correct?.let { append(" | correct=").append(it) }
        }
        log(msg)
    }

    fun breadcrumbUser(event: String, detail: String? = null) {
        log(if (detail != null) "[USER] $event | $detail" else "[USER] $event")
    }
}
