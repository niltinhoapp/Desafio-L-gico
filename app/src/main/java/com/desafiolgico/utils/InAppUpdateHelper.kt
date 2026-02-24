package com.desafiolgico.utils

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * In-App Updates (Play Core)
 *
 * Opção A (recomendada): FLEXIBLE
 * - Baixa em background
 * - Quando terminar, você mostra UI (Snack/Toast) pedindo "Reiniciar"
 * - Quando usuário clicar: completeUpdate()
 *
 * IMPORTANTES:
 * - Isso só funciona quando o app foi instalado pela Play Store (produção/teste interno/fechado/aberto).
 * - Em debug local (APK), quase sempre não aparece update.
 */
class InAppUpdateHelper(
    private val activity: Activity
) {

    companion object {
        private const val TAG = "InAppUpdateHelper"
        private const val REQ_CODE_UPDATE = 4721
    }

    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(activity)

    private var listener: InstallStateUpdatedListener? = null
    private var downloadedCallback: (() -> Unit)? = null

    /**
     * Checa update e inicia FLEXIBLE se disponível.
     *
     * onDownloaded: chamado quando o APK já terminou de baixar (mas ainda não aplicou).
     * Aí você mostra UI e chama completeFlexibleUpdate() quando o usuário aceitar.
     */
    fun checkAndStartFlexible(onDownloaded: () -> Unit) {
        downloadedCallback = onDownloaded

        // registra listener (uma vez)
        ensureListenerRegistered()

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (shouldStartFlexible(info)) {
                    startFlexible(info)
                } else {
                    // Mesmo que não inicie agora, se já estiver baixado (raro), dispara callback
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        onDownloaded()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Falha ao checar update: ${e.message}", e)
            }
    }

    /**
     * Se o update ficou em andamento e o usuário saiu/voltou,
     * isso retoma o estado e, se já tiver baixado, dispara o callback.
     */
    fun resumeIfInProgress() {
        ensureListenerRegistered()

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                // Se já baixou, avisa UI de novo
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    downloadedCallback?.invoke()
                    return@addOnSuccessListener
                }

                // Se tem update disponível e permite FLEXIBLE, pode iniciar de novo
                if (shouldStartFlexible(info)) {
                    startFlexible(info)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Falha ao resumir update: ${e.message}", e)
            }
    }

    /**
     * Quando o usuário clicar "Reiniciar", chame isso.
     * O Play Core aplica o update e reinicia o app.
     */
    fun completeFlexibleUpdate() {
        runCatching {
            appUpdateManager.completeUpdate()
        }.onFailure {
            Log.w(TAG, "completeUpdate falhou: ${it.message}", it)
        }
    }

    /**
     * Importante chamar no onDestroy() da Activity para evitar leak.
     */
    fun unregister() {
        runCatching {
            listener?.let { appUpdateManager.unregisterListener(it) }
        }
        listener = null
        downloadedCallback = null
    }

    // ---------------------------------------------------------------------------------------------
    // Internos
    // ---------------------------------------------------------------------------------------------

    private fun ensureListenerRegistered() {
        if (listener != null) return

        listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    // ✅ baixou — agora é com você mostrar UI
                    downloadedCallback?.invoke()
                }
                InstallStatus.INSTALLED -> {
                    // update aplicado — pode limpar listener
                    // (não é obrigatório, mas ajuda)
                    // unregister()
                }
                else -> Unit
            }
        }

        runCatching {
            appUpdateManager.registerListener(listener!!)
        }.onFailure {
            Log.w(TAG, "registerListener falhou: ${it.message}", it)
            listener = null
        }
    }

    private fun shouldStartFlexible(info: AppUpdateInfo): Boolean {
        val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        val allowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        return available && allowed
    }

    private fun startFlexible(info: AppUpdateInfo) {
        try {
            val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            appUpdateManager.startUpdateFlowForResult(
                info,
                activity,
                options,
                REQ_CODE_UPDATE
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.w(TAG, "startUpdateFlowForResult falhou: ${e.message}", e)
        } catch (t: Throwable) {
            Log.w(TAG, "Erro inesperado ao iniciar update: ${t.message}", t)
        }
    }
}
