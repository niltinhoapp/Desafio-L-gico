package com.desafiolgico.main.ui

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.desafiolgico.R
import com.desafiolgico.databinding.ActivityTestBinding

class OverlayController(
    private val activity: AppCompatActivity,
    private val rootLayout: ViewGroup,
    private val overlayContainer: ViewGroup,
    binding: ActivityTestBinding
) {
    private var curiosityOverlayView: View? = null
    private var autoCloseRunnable: Runnable? = null
    private var closing = false

    private val curiosities = listOf(
        "🌊 Sabia que o coração de um camarão fica na cabeça?",
        "🐘 O elefante é o único animal com quatro joelhos.",
        "🦋 As borboletas sentem o gosto com os pés!",
        "🔥 O Sol representa 99,86% da massa do Sistema Solar.",
        "💡 O cérebro humano gera eletricidade suficiente para acender uma lâmpada pequena.",
        "⚡ O relâmpago é mais quente que a superfície do Sol.",
        "🌎 A Terra não é perfeitamente redonda — é ligeiramente achatada nos polos.",
        "💓 Seu coração bate cerca de 100 mil vezes por dia.",
        "👀 Os olhos conseguem distinguir mais de 10 milhões de cores.",
        "🦵 O fêmur humano é mais forte que concreto.",
        "🧬 Cada célula do seu corpo contém cerca de 2 metros de DNA.",
        "🌌 Existem mais estrelas no universo do que grãos de areia na Terra.",
        "🐝 Abelhas reconhecem rostos humanos.",
        "🧠 Seu cérebro pesa cerca de 1,4 kg.",
        "🪶 O pinguim tem joelhos escondidos sob as penas.",
        "🦈 Tubarões existem antes dos dinossauros.",
        "🌧️ A chuva tem cheiro — chamado de petrichor.",
        "🌙 A Lua se afasta da Terra cerca de 3,8 cm por ano.",
        "🚀 Um foguete pode ultrapassar 28.000 km/h ao deixar a atmosfera.",
        "🐢 As tartarugas podem respirar pela cloaca (parte traseira do corpo)."
    )

    private val curiosityBag = ArrayDeque<String>()
    private var lastCuriosity: String? = null

    fun resetCuriosityBag() {
        curiosityBag.clear()
        curiosityBag.addAll(curiosities.shuffled())

        // evita repetir a mesma curiosidade em sequência
        if (curiosityBag.size > 1 && curiosityBag.first() == lastCuriosity) {
            val first = curiosityBag.removeFirst()
            val second = curiosityBag.removeFirst()
            curiosityBag.addFirst(first)
            curiosityBag.addFirst(second)
        }
    }

    fun nextCuriosity(): String {
        if (curiosityBag.isEmpty()) resetCuriosityBag()
        return curiosityBag.removeFirst().also { lastCuriosity = it }
    }

    fun showFloatingChip(text: String, iconRes: Int, positive: Boolean) {
        if (activity.isFinishing || activity.isDestroyed) return

        val chip = activity.layoutInflater.inflate(R.layout.view_floating_chip, overlayContainer, false)

        val icon = chip.findViewById<ImageView>(R.id.chipIcon)
        val tv = chip.findViewById<TextView>(R.id.chipText)

        icon.setImageResource(iconRes)
        tv.text = text

        tv.setTextColor(if (positive) 0xFFE6FFFFFF.toInt() else 0xFFFFE3E3.toInt())
        icon.imageTintList = android.content.res.ColorStateList.valueOf(
            if (positive) 0xFFA5FFB1.toInt() else 0xFFFFA5A5.toInt()
        )

        chip.alpha = 0f
        chip.translationY = -12f
        overlayContainer.addView(chip)

        chip.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(140)
            .withEndAction {
                chip.animate()
                    .alpha(0f)
                    .translationY(-18f)
                    .setStartDelay(650)
                    .setDuration(180)
                    .withEndAction {
                        try { overlayContainer.removeView(chip) } catch (_: Exception) {}
                    }
                    .start()
            }
            .start()
    }

    fun showCuriosityOverlay(
        text: String,
        durationMs: Long = 3000L,
        onDone: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        overlayContainer.post {
            if (activity.isFinishing || activity.isDestroyed) return@post

            // fecha qualquer overlay anterior com segurança
            dismissCuriosityOverlay(immediate = true)

            closing = false
            overlayContainer.visibility = View.VISIBLE
            rootLayout.bringToFront()
            overlayContainer.bringToFront()

            val overlay = activity.layoutInflater.inflate(
                R.layout.view_curiosity_overlay,
                overlayContainer,
                false
            ).apply {
                isClickable = true
                isFocusable = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                elevation = 9999f
                translationZ = 9999f
                alpha = 0f
            }

            curiosityOverlayView = overlay
            overlay.findViewById<TextView>(R.id.curiosityText).text = text

            overlayContainer.addView(overlay)
            overlay.bringToFront()

            val finishOnce = Let@{
                if (closing) return@Let
            }

            fun finish() {
                if (closing) return
                closing = true

                // cancela timer pendente
                autoCloseRunnable?.let { overlay.removeCallbacks(it) }
                autoCloseRunnable = null

                // se já foi removido, só finaliza fluxo
                if (overlay.parent == null) {
                    if (curiosityOverlayView === overlay) curiosityOverlayView = null
                    if (overlayContainer.childCount == 0) overlayContainer.visibility = View.GONE
                    onDone()
                    return
                }

                overlay.animate().cancel()
                overlay.animate()
                    .alpha(0f)
                    .setDuration(160)
                    .withEndAction {
                        try { overlayContainer.removeView(overlay) } catch (_: Exception) {}
                        if (curiosityOverlayView === overlay) curiosityOverlayView = null
                        if (overlayContainer.childCount == 0) overlayContainer.visibility = View.GONE
                        onDone()
                    }
                    .start()
            }

            overlay.setOnClickListener { finish() }

            overlay.animate()
                .alpha(1f)
                .setDuration(160)
                .withEndAction {
                    val r = Runnable { finish() }
                    autoCloseRunnable = r
                    overlay.postDelayed(r, durationMs)
                }
                .start()
        }
    }

    /**
     * Fecha overlay atual. Use quando mudar de tela, pausar jogo, etc.
     */
    fun dismissCuriosityOverlay(immediate: Boolean = false) {
        val overlay = curiosityOverlayView ?: run {
            if (overlayContainer.childCount == 0) overlayContainer.visibility = View.GONE
            return
        }

        // cancela timer pendente
        autoCloseRunnable?.let { overlay.removeCallbacks(it) }
        autoCloseRunnable = null

        closing = false

        if (immediate) {
            try { overlay.animate().cancel() } catch (_: Exception) {}
            try { overlayContainer.removeView(overlay) } catch (_: Exception) {}
            if (curiosityOverlayView === overlay) curiosityOverlayView = null
            if (overlayContainer.childCount == 0) overlayContainer.visibility = View.GONE
            return
        }

        overlay.animate().cancel()
        overlay.animate()
            .alpha(0f)
            .setDuration(120)
            .withEndAction {
                try { overlayContainer.removeView(overlay) } catch (_: Exception) {}
                if (curiosityOverlayView === overlay) curiosityOverlayView = null
                if (overlayContainer.childCount == 0) overlayContainer.visibility = View.GONE
            }
            .start()
    }
}
