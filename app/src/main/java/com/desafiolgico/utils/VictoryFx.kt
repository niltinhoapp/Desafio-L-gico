package com.desafiolgico.utils

import android.content.Context
import android.media.MediaPlayer
import android.view.View
import com.desafiolgico.R
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

object VictoryFx {

    // Reuso (menos garbage / mais liso)
    private val SHAPES = listOf(Shape.Square, Shape.Circle)
    private val SIZES = listOf(Size(8, 3f), Size(10, 4f), Size(12, 5f))

    private data class Preset(
        val colors: IntArray,
        val soundRes: Int,
        val speed: Float,
        val durationMs: Long,
        val perSecond: Int
    )

    // Mantém 1 player ativo (evita tocar 2 sons por cima e evita leak)
    private var currentMp: MediaPlayer? = null

    fun play(context: Context, konfetti: KonfettiView) {
        val appCtx = context.applicationContext

        // garante visível e cancela qualquer animação anterior (sem crash)
        konfetti.visibility = View.VISIBLE
        stopKonfettiSafe(konfetti)

        val preset = when (GameDataManager.getSelectedVfx(appCtx)) {
            "vfx_gold" -> Preset(
                colors = intArrayOf(0xFFFFD700.toInt(), 0xFFFFF8E1.toInt(), 0xFFFFC107.toInt()),
                soundRes = R.raw.victory_gold,
                speed = 12f,
                durationMs = 1200L,
                perSecond = 220
            )
            "vfx_neon" -> Preset(
                colors = intArrayOf(0xFF00E5FF.toInt(), 0xFFFF00E5.toInt(), 0xFFB2FF59.toInt()),
                soundRes = R.raw.victory_neon,
                speed = 14f,
                durationMs = 1200L,
                perSecond = 230
            )
            "vfx_fire" -> Preset(
                colors = intArrayOf(0xFFFF3D00.toInt(), 0xFFFF9100.toInt(), 0xFFFF6D00.toInt()),
                soundRes = R.raw.victory_fire,
                speed = 16f,
                durationMs = 1400L,
                perSecond = 260
            )
            else -> Preset(
                colors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFBBDEFB.toInt()),
                soundRes = R.raw.victory_basic,
                speed = 10f,
                durationMs = 900L,
                perSecond = 180
            )
        }

        val party = Party(
            speed = preset.speed,
            maxSpeed = preset.speed + 8f,
            damping = 0.90f,
            spread = 360,
            colors = preset.colors.toList(),
            size = SIZES,
            shapes = SHAPES,
            timeToLive = preset.durationMs,
            emitter = Emitter(duration = preset.durationMs, TimeUnit.MILLISECONDS)
                .perSecond(preset.perSecond),
            position = Position.Relative(0.5, 0.20)
        )

        // ✅ Compatível com Konfetti 2.0.4
        konfetti.start(listOf(party))
        playSoundSafe(appCtx, preset.soundRes)

        // some depois do efeito (sem “ficar sujo” na UI)
        konfetti.postDelayed({
            stopKonfettiSafe(konfetti)
            konfetti.visibility = View.GONE
        }, preset.durationMs + 250L)
    }

    private fun stopKonfettiSafe(konfetti: KonfettiView) {
        runCatching {
            val m = konfetti.javaClass.methods.firstOrNull {
                it.name == "stopGracefully" && it.parameterTypes.isEmpty()
            }
            if (m != null) m.invoke(konfetti)
        }
    }

    private fun playSoundSafe(context: Context, res: Int) {
        runCatching {
            try { currentMp?.stop() } catch (_: Exception) {}
            try { currentMp?.release() } catch (_: Exception) {}
            currentMp = null

            val mp = MediaPlayer.create(context, res) ?: return
            currentMp = mp
            mp.setOnCompletionListener {
                try { it.release() } catch (_: Exception) {}
                if (currentMp === it) currentMp = null
            }
            mp.start()
        }
    }

    fun release() {
        try { currentMp?.stop() } catch (_: Exception) {}
        try { currentMp?.release() } catch (_: Exception) {}
        currentMp = null
    }
}
