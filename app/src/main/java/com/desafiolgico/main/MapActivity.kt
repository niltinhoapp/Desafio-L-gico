package com.desafiolgico.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.View.MeasureSpec
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.desafiolgico.R
import com.desafiolgico.utils.GameDataManager
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.hypot
import android.animation.ValueAnimator
import android.content.Intent
import android.widget.Toast
import com.desafiolgico.utils.ScoreManager
import kotlin.math.floor
import com.desafiolgico.utils.EnigmaPortalGate


class MapActivity : AppCompatActivity() {

    private data class Pt(val x: Float, val y: Float)
    private data class Anchor(val fx: Float, val fy: Float)
    private val milestoneStars = mutableListOf<ImageView>()
    private var travelAnimator: ValueAnimator? = null
    private var firstOpen = true



    private val levelOrder = listOf(
        GameDataManager.Levels.INICIANTE,
        GameDataManager.Levels.INTERMEDIARIO,
        GameDataManager.Levels.AVANCADO,
        GameDataManager.Levels.EXPERIENTE
    )

    /**
     * ✅ Anchors (fração 0..1) EXTRAÍDOS do centro da estrada do seu mapa (mapa_pa),
     * com MAIS pontos pra estrela não “cortar caminho” nas curvas.
     */
    private val anchors = listOf(
        Anchor(0.1363f, 0.6021f),
        Anchor(0.1860f, 0.5907f),
        Anchor(0.2642f, 0.5787f),
        Anchor(0.3405f, 0.5701f),
        Anchor(0.4186f, 0.5636f),
        Anchor(0.4962f, 0.5602f),
        Anchor(0.5744f, 0.5588f),
        Anchor(0.6516f, 0.5564f),
        Anchor(0.7306f, 0.5517f),
        Anchor(0.8077f, 0.5462f),
        Anchor(0.8862f, 0.5397f),
        Anchor(0.8390f, 0.5150f),
        Anchor(0.7607f, 0.4981f),
        Anchor(0.6873f, 0.4791f),
        Anchor(0.6090f, 0.4653f),
        Anchor(0.5355f, 0.4527f),
        Anchor(0.4759f, 0.4395f),
        Anchor(0.5493f, 0.4220f),
        Anchor(0.6262f, 0.4052f),
        Anchor(0.7049f, 0.3878f),
        Anchor(0.7816f, 0.3784f),
        Anchor(0.8922f, 0.3725f),
        Anchor(0.8190f, 0.3074f),
        Anchor(0.7071f, 0.2701f),
        Anchor(0.8286f, 0.2437f),
    )

    // Views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var imgMap: ImageView
    private lateinit var overlay: FrameLayout
    private lateinit var star: ImageView
    private lateinit var glow: View
    private lateinit var ping: View
    private lateinit var txtProgress: TextView
    private lateinit var scoreManager: ScoreManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        GameDataManager.init(this)

        scoreManager = ScoreManager(this)

        scoreManager.overallScoreLive.observe(this) { score ->
            maybeOpenEnigmaPortal(score)
        }


        toolbar = findViewById(R.id.toolbarMap)
        imgMap = findViewById(R.id.imgMap)
        overlay = findViewById(R.id.overlay)
        star = findViewById(R.id.imgStar)
        glow = findViewById(R.id.starGlow)
        ping = findViewById(R.id.starPing)
        txtProgress = findViewById(R.id.txtMapProgress)

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Primeiro desenho
        }

    override fun onResume() {
        super.onResume()

        val scoreGlobal = GameDataManager.getOverallTotalScore(this)
        maybeOpenEnigmaPortal(scoreGlobal)

        imgMap.post {
            refreshMap(animated = true, startFromZero = true)
        }


    }


    // ---------------------------
    // Atualiza tudo (texto + estrela)
    // ---------------------------
    private fun refreshMap(animated: Boolean, startFromZero: Boolean) {

        travelAnimator?.cancel()

        val (curLevel, curCorrect) = currentLevelAndCorrect()
        val stageInLevel = stageFromCorrect(curCorrect) // 0..3

        val totalCorrectAll = levelOrder.sumOf { GameDataManager.getCorrectForLevel(this, it) }
        val inLevel = curCorrect.coerceIn(0, 30)

        val nextIn = when {
            inLevel >= 30 -> 0
            inLevel % 10 == 0 -> 10
            else -> 10 - (inLevel % 10)
        }

        txtProgress.text =
            "Total: $totalCorrectAll • Nível: $curLevel ($inLevel/30) • Próximo passo: $nextIn"

        val milestoneCount = 1 + levelOrder.size * 3 // 13 (0/10/20/30 por nível)
        val currentIndex = computeGlobalMilestoneIndex().coerceIn(0, milestoneCount - 1)

        // Anchors (fração -> px da imagem -> px na tela via matrix)
        val screenPoly = anchors.map { a ->
            val pImg = fractionToImagePx(imgMap, a.fx, a.fy)
            drawableToViewPoint(imgMap, pImg.x, pImg.y)
        }

        // t do marco atual
        val tNow = currentIndex.toFloat() / (milestoneCount - 1).toFloat()

        // t do marco anterior (pra voltar de fase)
        val prevIndex = (currentIndex - 1).coerceAtLeast(0)
        val tPrev = prevIndex.toFloat() / (milestoneCount - 1).toFloat()

        // ✅ cria/garante as estrelas apagadas
        ensureMilestoneStars(milestoneCount)

        // ✅ posiciona todas as estrelas nos pontos fixos (marcos)
        for (i in 0 until milestoneCount) {
            val t = i.toFloat() / (milestoneCount - 1).toFloat()
            val p = pointAtT(screenPoly, t)
            placeMilestoneAt(i, p)
        }

        // ✅ estrela móvel sempre visível e na frente
        star.visibility = View.VISIBLE
        star.alpha = 1f
        star.bringToFront()
        glow.bringToFront()
        ping.bringToFront()

        // Visual premium da estrela móvel (tamanho + brilho por estágio)
        applyPremiumStarStyle(star, glow, stageInLevel)

        // Medir tudo antes de posicionar (evita width/height = 0)
        ensureMeasured(star)
        ensureMeasured(glow)
        ensureMeasured(ping)

        // ✅ ponto inicial: 0 se abriu agora; senão começa do marco anterior
        val tStart = if (startFromZero) 0f else tPrev
        val pStart = pointAtT(screenPoly, tStart)
        placeOn(star, glow, ping, pStart)

        // ✅ se não animar: só posiciona e acende até o índice atual
        if (!animated) {
            val pEnd = pointAtT(screenPoly, tNow)
            placeOn(star, glow, ping, pEnd)

            for (i in 0..currentIndex) setMilestoneLit(i, lit = true)
            return
        }

        // ✅ animado: percorre o caminho e vai acendendo as estrelas
        animateAlongPath(
            poly = screenPoly,
            tStart = tStart,
            tEnd = tNow,
            duration = if (startFromZero) 1200L else 720L,
            currentIndex = currentIndex,
            milestoneCount = milestoneCount,
            stageInLevel = stageInLevel
        )
    }


    private fun maybeOpenEnigmaPortal(score: Int) {
        if (!EnigmaPortalGate.shouldAutoOpen(this, score)) return

        EnigmaPortalGate.markAutoOpened(this)

        startActivity(
            Intent(this, EnigmaPortalActivity::class.java)
                .putExtra(EnigmaPortalActivity.EXTRA_SCORE, score)
        )
    }




    // ---------------------------
    // Progresso: 0..30 por nível, marcos 0/10/20/30
    // ---------------------------
    private fun stageFromCorrect(c: Int): Int = when {
        c >= 30 -> 3
        c >= 20 -> 2
        c >= 10 -> 1
        else -> 0
    }

    private fun computeGlobalMilestoneIndex(): Int {
        var idx = 0
        for (lvl in levelOrder) {
            val c = GameDataManager.getCorrectForLevel(this, lvl)
            val stage = stageFromCorrect(c) // 0..3

            if (c >= 30) idx += 3
            else {
                idx += stage
                return idx
            }
        }
        return (1 + levelOrder.size * 3 - 1)
    }

    private fun currentLevelAndCorrect(): Pair<String, Int> {
        for (lvl in levelOrder) {
            val c = GameDataManager.getCorrectForLevel(this, lvl)
            if (c < 30) return lvl to c
        }
        return levelOrder.last() to 30
    }

    // ---------------------------
    // Coordenadas: fração -> px da imagem -> px na tela (imageMatrix)
    // ---------------------------
    private fun fractionToImagePx(img: ImageView, fx: Float, fy: Float): Pt {
        val d = img.drawable ?: return Pt(0f, 0f)
        val xPx = fx.coerceIn(0f, 1f) * d.intrinsicWidth.toFloat()
        val yPx = fy.coerceIn(0f, 1f) * d.intrinsicHeight.toFloat()
        return Pt(xPx, yPx)
    }

    private fun drawableToViewPoint(img: ImageView, xPx: Float, yPx: Float): Pt {
        val pts = floatArrayOf(xPx, yPx)
        img.imageMatrix.mapPoints(pts)

        // padding (se tiver)
        return Pt(
            pts[0] + img.paddingLeft,
            pts[1] + img.paddingTop
        )
    }

    // ---------------------------
    // Polyline: ponto em t (0..1) por comprimento
    // ---------------------------
    private fun pointAtT(poly: List<Pt>, t: Float): Pt {
        if (poly.isEmpty()) return Pt(0f, 0f)
        if (t <= 0f) return poly.first()
        if (t >= 1f) return poly.last()

        val segLens = FloatArray(poly.size - 1)
        var total = 0f
        for (i in 0 until poly.size - 1) {
            val dx = poly[i + 1].x - poly[i].x
            val dy = poly[i + 1].y - poly[i].y
            val len = hypot(dx, dy)
            segLens[i] = len
            total += len
        }
        if (total <= 0f) return poly.first()

        val target = total * t
        var acc = 0f
        for (i in segLens.indices) {
            val len = segLens[i]
            if (acc + len >= target) {
                val local = if (len == 0f) 0f else (target - acc) / len
                return Pt(
                    poly[i].x + (poly[i + 1].x - poly[i].x) * local,
                    poly[i].y + (poly[i + 1].y - poly[i].y) * local
                )
            }
            acc += len
        }
        return poly.last()
    }

    // ---------------------------
    // UI: posicionamento / animação
    // ---------------------------
    private fun ensureMeasured(v: View) {
        if (v.measuredWidth > 0 && v.measuredHeight > 0) return
        v.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
    }

    private fun placeOn(star: View, glow: View, ping: View, p: Pt) {
        star.x = p.x - star.measuredWidth / 2f
        star.y = p.y - star.measuredHeight / 2f

        glow.x = p.x - glow.measuredWidth / 2f
        glow.y = p.y - glow.measuredHeight / 2f

        ping.x = p.x - ping.measuredWidth / 2f
        ping.y = p.y - ping.measuredHeight / 2f
    }

    private fun animateTo(star: View, glow: View, ping: View, p: Pt) {
        val sx = p.x - star.measuredWidth / 2f
        val sy = p.y - star.measuredHeight / 2f

        star.animate().cancel()
        star.animate()
            .x(sx).y(sy)
            .setDuration(720)
            .setInterpolator(DecelerateInterpolator())
            .start()

        glow.animate().cancel()
        glow.animate()
            .x(p.x - glow.measuredWidth / 2f)
            .y(p.y - glow.measuredHeight / 2f)
            .setDuration(720)
            .setInterpolator(DecelerateInterpolator())
            .start()

        ping.animate().cancel()
        ping.animate()
            .x(p.x - ping.measuredWidth / 2f)
            .y(p.y - ping.measuredHeight / 2f)
            .setDuration(720)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    // ---------------------------
    // PREMIUM MAX: visual por estágio (0..3)
    // ---------------------------
    private fun applyPremiumStarStyle(star: ImageView, glow: View, stage: Int) {
        val sizeDp = when (stage) {
            0 -> 22
            1 -> 26
            2 -> 30
            else -> 34
        }

        val useFilled = stage >= 2
        star.setImageResource(
            if (useFilled) R.drawable.star_filled_layer
            else R.drawable.star_outline_layer
        )

        // ⭐ sempre dourado
        val tint = Color.argb(245, 255, 226, 0)
        star.setColorFilter(tint)
        star.imageTintList = ColorStateList.valueOf(tint)

        star.updateLayoutParams<FrameLayout.LayoutParams> {
            width = dp(sizeDp)
            height = dp(sizeDp)
        }

        when (stage) {
            0 -> glow.visibility = View.GONE
            1 -> {
                glow.visibility = View.VISIBLE
                glow.setBackgroundResource(R.drawable.bg_glow_soft)
                glow.alpha = 0.55f
            }
            2 -> {
                glow.visibility = View.VISIBLE
                glow.setBackgroundResource(R.drawable.bg_glow_gold)
                glow.alpha = 0.70f
            }
            else -> {
                glow.visibility = View.VISIBLE
                glow.setBackgroundResource(R.drawable.bg_glow_green)
                glow.alpha = 0.82f
            }
        }

        // breathing
        star.animate().cancel()
        star.scaleX = 1f
        star.scaleY = 1f
        star.animate()
            .scaleX(1.05f).scaleY(1.05f)
            .setDuration(900)
            .withEndAction {
                star.animate().scaleX(1f).scaleY(1f).setDuration(900).start()
            }
            .start()
    }

    private fun playPing(ping: View) {
        ping.visibility = View.VISIBLE
        ping.alpha = 0f
        ping.scaleX = 0.7f
        ping.scaleY = 0.7f

        ping.animate().cancel()
        ping.animate()
            .alpha(0.9f)
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(520)
            .withEndAction {
                ping.animate()
                    .alpha(0f)
                    .setDuration(220)
                    .withEndAction { ping.visibility = View.GONE }
                    .start()
            }
            .start()
    }
    private fun ensureMilestoneStars(count: Int) {
        if (milestoneStars.size == count) return

        // remove antigas
        milestoneStars.forEach { overlay.removeView(it) }
        milestoneStars.clear()

        repeat(count) {
            val iv = ImageView(this).apply {
                setImageResource(R.drawable.star_outline_layer)
                // apagada (cinza)
                setColorFilter(Color.argb(150, 255, 255, 255))
                imageTintList = ColorStateList.valueOf(Color.argb(150, 255, 255, 255))
                alpha = 0.75f
                layoutParams = FrameLayout.LayoutParams(dp(18), dp(18))
            }
            overlay.addView(iv)          // entra atrás
            milestoneStars.add(iv)
        }

        // garante ordem: marcos atrás, efeitos e estrela móvel na frente
        glow.bringToFront()
        ping.bringToFront()
        star.bringToFront()
    }

    private fun setMilestoneLit(i: Int, lit: Boolean) {
        val v = milestoneStars.getOrNull(i) ?: return
        if (lit) {
            v.setImageResource(R.drawable.star_filled_layer)
            val tint = Color.argb(245, 255, 226, 0) // amarelo
            v.setColorFilter(tint)
            v.imageTintList = ColorStateList.valueOf(tint)
            v.alpha = 1f
        } else {
            v.setImageResource(R.drawable.star_outline_layer)
            val tint = Color.argb(140, 255, 255, 255) // apagada
            v.setColorFilter(tint)
            v.imageTintList = ColorStateList.valueOf(tint)
            v.alpha = 0.7f
        }
    }

    private fun placeMilestoneAt(i: Int, p: Pt) {
        val v = milestoneStars.getOrNull(i) ?: return
        ensureMeasured(v)
        v.x = p.x - v.measuredWidth / 2f
        v.y = p.y - v.measuredHeight / 2f
    }
    private fun animateAlongPath(
        poly: List<Pt>,
        tStart: Float,
        tEnd: Float,
        duration: Long,
        currentIndex: Int,
        milestoneCount: Int,
        stageInLevel: Int
    ) {
        travelAnimator?.cancel()

        // tudo apagado no começo, e vai acendendo conforme passa
        for (i in 0 until milestoneCount) setMilestoneLit(i, lit = false)
        setMilestoneLit(0, lit = true)

        var lastLit = 0

        travelAnimator = ValueAnimator.ofFloat(tStart, tEnd).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()

            addUpdateListener { va ->
                val t = va.animatedValue as Float
                val p = pointAtT(poly, t)
                placeOn(star, glow, ping, p)

                // acende marcos conforme t avança
                val idxByT = floor(t * (milestoneCount - 1)).toInt().coerceIn(0, milestoneCount - 1)
                val shouldLitUpTo = minOf(idxByT, currentIndex)

                if (shouldLitUpTo > lastLit) {
                    for (i in (lastLit + 1)..shouldLitUpTo) setMilestoneLit(i, lit = true)
                    lastLit = shouldLitUpTo
                }
            }

            doOnEnd {
                // garante final
                for (i in 0..currentIndex) setMilestoneLit(i, lit = true)
                if (stageInLevel >= 1) playPing(ping)
            }

            start()
        }
    }

    // helper simples
    private fun ValueAnimator.doOnEnd(block: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = block()
        })
    }



    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
