package com.desafiolgico.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.desafiolgico.BuildConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object CrashlyticsHelper {

    private val didFastInit = AtomicBoolean(false)

    /**
     * ✅ Inicialização LEVE (boa para startup).
     * Chame 1x (idempotente) e de preferência após o 1º frame / em background.
     */
    fun initFast(context: Context, enableInDebug: Boolean = false) {
        if (!didFastInit.compareAndSet(false, true)) return

        val appCtx = context.applicationContext
        FirebaseApp.initializeApp(appCtx)
        val crashlytics = FirebaseCrashlytics.getInstance()

        val isDebuggable = (appCtx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val enabled = (!isDebuggable || enableInDebug)
        crashlytics.setCrashlyticsCollectionEnabled(enabled)

        // Tudo aqui deve ser “barato” (sem PackageManager / UMP / SharedPrefs pesados).
        crashlytics.setCustomKey("build_type", if (isDebuggable) "debug" else "release")
        crashlytics.setCustomKey("crashlytics_enabled", enabled)

        // Use BuildConfig (evita PackageManager.getPackageInfo no startup)
        crashlytics.setCustomKey("app_version_name", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE.toString())

        crashlytics.setCustomKey("android_api", Build.VERSION.SDK_INT)
        crashlytics.setCustomKey("device", "${Build.MANUFACTURER} ${Build.MODEL}")
        crashlytics.setCustomKey("locale", Locale.getDefault().toLanguageTag())
    }

    /**
     * ✅ Atualize contexto de usuário APÓS login/guest.
     * (Evita puxar UserManager no cold start)
     */
    fun setUserContext(
        context: Context,
        userId: String?,
        email: String? = null,
        username: String? = null
    ) {
        val crashlytics = FirebaseCrashlytics.getInstance()

        // UserId: só seta se vier algo válido
        val id = userId?.trim().orEmpty()
        if (id.isNotEmpty()) crashlytics.setUserId(id)

        // Evite PII: não grave nome/email em texto puro
        if (!username.isNullOrBlank()) crashlytics.setCustomKey("user_name_hash", sha256(username))
        crashlytics.setCustomKey("has_email", !email.isNullOrBlank())
    }

    /**
     * ✅ Estado do jogo: chame quando já tiver dados prontos (depois do login / depois que GameDataManager initou).
     */
    fun setGameState(context: Context) {
        val appCtx = context.applicationContext
        val crashlytics = FirebaseCrashlytics.getInstance()

        runCatching { crashlytics.setCustomKey("coins", CoinManager.getCoins(appCtx)) }
        runCatching { crashlytics.setCustomKey("current_level", GameDataManager.getUltimoNivelNormal(appCtx) ?: "UNKNOWN") }
        runCatching { crashlytics.setCustomKey("current_streak", GameDataManager.currentStreak) }
    }

    /**
     * ✅ UMP: chame só quando for usar Ads (não no startup).
     */
    fun enrichWithUmp(context: Context) {
        val appCtx = context.applicationContext
        val crashlytics = FirebaseCrashlytics.getInstance()

        runCatching {
            val ci = UserMessagingPlatform.getConsentInformation(appCtx)
            val status = when (ci.consentStatus) {
                ConsentInformation.ConsentStatus.UNKNOWN -> "UNKNOWN"
                ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
                ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
                ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
                else -> "OTHER"
            }
            crashlytics.setCustomKey("ump_consent_status", status)
            crashlytics.setCustomKey("ump_form_available", ci.isConsentFormAvailable)
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
            level?.let { append(" | level=").append(it) }
            qIndex?.let { append(" | question=").append(it) }
            correct?.let { append(" | correct=").append(it) }
        }
        log(msg)
    }

    fun breadcrumbUser(event: String, detail: String? = null) {
        log(if (detail != null) "[USER] $event | $detail" else "[USER] $event")
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(24) // curtinho
    }
}
