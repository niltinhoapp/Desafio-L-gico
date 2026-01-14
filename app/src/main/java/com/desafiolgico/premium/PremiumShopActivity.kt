package com.desafiolgico.premium

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.desafiolgico.R
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.PremiumCatalog
import com.desafiolgico.utils.PremiumItem
import com.desafiolgico.utils.PremiumManager
import com.desafiolgico.utils.PremiumType
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PremiumShopActivity : AppCompatActivity() {

    private lateinit var coinsText: TextView
    private lateinit var listContainer: LinearLayout

    // Paleta Dark Premium (ajuste fino fácil aqui)
    private val cBgTop = Color.parseColor("#0B1020")
    private val cBgMid = Color.parseColor("#0F172A")
    private val cBgBottom = Color.parseColor("#060A14")

    private val cGlass = Color.parseColor("#14FFFFFF")
    private val cStrokeSoft = Color.parseColor("#24FFFFFF")

    private val cWhite = Color.WHITE
    private val cMuted = Color.parseColor("#D1D5DB")
    private val cMuted2 = Color.parseColor("#94A3B8")

    private val cGold = Color.parseColor("#FBBF24")
    private val cGoldDeep = Color.parseColor("#F59E0B")
    private val cBlue = Color.parseColor("#2563EB")

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEdgeToEdge()

        GameDataManager.init(applicationContext)

        setContentView(buildContentView())

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sys.top, bottom = sys.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // fundo gradiente (recomendado via drawable). Fallback: cor sólida caso não exista.
            try {
                setBackgroundResource(R.drawable.bg_premium_gradient)
            } catch (_: Exception) {
                setBackgroundColor(cBgMid)
            }
        }

        // Header em card "glass"
        val headerCard = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            useCompatPadding = true
            setCardBackgroundColor(cGlass)
            strokeWidth = dp(1)
            strokeColor = cStrokeSoft
            setContentPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val backBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "←"
            minWidth = 0
            minimumHeight = dp(36)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setTextColor(cWhite)
            strokeWidth = dp(1)
            strokeColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }


        val title = TextView(this).apply {
            text = "Loja Premium"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(cWhite)
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        coinsText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTextColor(cGold)
            gravity = Gravity.END
            text = "💰 0"
        }

        header.addView(backBtn)
        header.addView(title)
        header.addView(coinsText)

        headerCard.addView(header)

        root.addView(
            headerCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(14)
                rightMargin = dp(14)
                topMargin = dp(12)
            }
        )

        // Scroll
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(18))
        }

        scroll.addView(
            listContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply { topMargin = dp(6) }
        )

        return root
    }

    private fun refreshUi() {
        // libera automaticamente o que já bateu por conquista
        PremiumCatalog.all().forEach { PremiumManager.unlockByAchievementIfPossible(applicationContext, it) }

        val coins = CoinManager.getCoins(this)
        coinsText.text = "💰 $coins"

        listContainer.removeAllViews()

        addSection("🎨 Temas", PremiumCatalog.themes)
        addSection("🖼️ Molduras", PremiumCatalog.frames)
        addSection("🏷️ Títulos", PremiumCatalog.titles)
        addSection("🐾 Pets", PremiumCatalog.pets)
        addSection("✨ Efeitos (VFX)", PremiumCatalog.vfx)
    }

    private fun addSection(title: String, items: List<PremiumItem>) {
        val tv = TextView(this).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            setTextColor(cWhite)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            // chip de seção
            try {
                background = getDrawable(R.drawable.bg_section_chip)
            } catch (_: Exception) {
                setBackgroundColor(cGlass)
            }
        }

        listContainer.addView(
            tv,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
        )

        items.forEach { item ->
            listContainer.addView(buildItemCard(item))
        }
    }

    private fun buildItemCard(item: PremiumItem): View {
        val unlocked = PremiumManager.isUnlocked(this, item)
        val selected = isSelected(item)

        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            useCompatPadding = true
            setCardBackgroundColor(Color.parseColor("#141B2D"))
            strokeWidth = if (selected) dp(2) else dp(1)
            strokeColor = if (selected) cGold else cStrokeSoft
            setContentPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val name = TextView(this).apply {
            text = item.name
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(cWhite)
        }

        val desc = TextView(this).apply {
            val base = item.desc.takeIf { it.isNotBlank() } ?: defaultDesc(item)
            text = base
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(cMuted)
            setPadding(0, dp(4), 0, 0)
        }

        val status = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(6), 0, 0)
            setTextColor(
                when {
                    selected -> cGold
                    unlocked -> Color.parseColor("#93C5FD")
                    item.priceCoins > 0 -> Color.parseColor("#FCA5A5")
                    else -> cMuted2
                }
            )
        }



        status.text = when {
            selected -> "✅ Selecionado"
            unlocked -> "🔓 Desbloqueado"
            else -> item.statusText(this)
        }


        left.addView(name)
        left.addView(desc)
        left.addView(status)

        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val mainBtn = MaterialButton(this).apply {
            // ✅ remove limites mínimos que criam “folga”
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0

            // ✅ seu padding real
            setPadding(dp(12), dp(10), dp(12), dp(10))

            setTextColor(Color.WHITE)
            isAllCaps = false
        }

        when {
            !unlocked && item.priceCoins > 0 -> {
                mainBtn.text = "Comprar (${item.priceCoins})"
                mainBtn.backgroundTintList = ColorStateList.valueOf(cGoldDeep)
                mainBtn.setOnClickListener {
                    val ok = PremiumManager.purchase(this, item)
                    if (!ok) {
                        Toast.makeText(this, "Moedas insuficientes 😕", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    PremiumManager.applySelected(this, item)
                    Toast.makeText(this, "Comprado e aplicado ✅", Toast.LENGTH_SHORT).show()
                    refreshUi()
                }
            }

            !unlocked && item.priceCoins <= 0 -> {
                mainBtn.text = "Conquista"
                mainBtn.isEnabled = false
                mainBtn.alpha = 0.75f
                mainBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#334155"))
            }

            unlocked && selected -> {
                mainBtn.text = "Selecionado"
                mainBtn.isEnabled = false
                mainBtn.alpha = 0.85f
                mainBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1F2937"))
            }

            unlocked && !selected -> {
                mainBtn.text = "Aplicar"
                mainBtn.backgroundTintList = ColorStateList.valueOf(cBlue)
                mainBtn.setOnClickListener {
                    PremiumManager.applySelected(this, item)
                    Toast.makeText(this, "Aplicado ✅", Toast.LENGTH_SHORT).show()
                    refreshUi()
                }
            }
        }

        right.addView(mainBtn)

        // upgrade pet (mantém sua lógica, só estiliza)
        if (item.type == PremiumType.PET && unlocked && item.id != "pet_none") {
            val lvl = GameDataManager.getPetLevel(this, item.id)

            val lvlTv = TextView(this).apply {
                text = "Nível: $lvl/3"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTextColor(cMuted)
                setPadding(0, dp(8), 0, dp(2))
                gravity = Gravity.END
            }
            right.addView(lvlTv)

            if (lvl < 3) {
                val next = lvl + 1
                val cost = petUpgradeCost(next)

                val upBtn = MaterialButton(this).apply {
                    text = "Up $next ($cost)"

                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0

                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    setTextColor(Color.WHITE)
                    isAllCaps = false

                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7C3AED")) // roxo premium
                }

                upBtn.setOnClickListener {
                    val ok = PremiumManager.upgradePet(this, item.id, cost)
                    if (!ok) {
                        Toast.makeText(this, "Sem moedas / já no máximo 😅", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    Toast.makeText(this, "Pet evoluiu! 🐾✨", Toast.LENGTH_SHORT).show()
                    refreshUi()
                }

                right.addView(upBtn)
            }
        }

        wrap.addView(left)
        wrap.addView(right)
        card.addView(wrap)

        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }

        return card
    }

    private fun isSelected(item: PremiumItem): Boolean = when (item.type) {
        PremiumType.THEME -> GameDataManager.getSelectedTheme(this) == item.id
        PremiumType.FRAME -> GameDataManager.getSelectedFrame(this) == item.id
        PremiumType.TITLE -> GameDataManager.getSelectedTitle(this) == item.id
        PremiumType.PET -> GameDataManager.getSelectedPet(this) == item.id
        PremiumType.VFX -> GameDataManager.getSelectedVfx(this) == item.id
    }

    private fun requirementText(item: PremiumItem): String {
        val parts = mutableListOf<String>()
        if (item.minDailyStreak > 0) {
            val cur = GameDataManager.getDailyStreak(this)
            parts += "Daily ${item.minDailyStreak} (atual $cur)"
        }
        if (item.minHighestStreak > 0) {
            val best = GameDataManager.getHighestStreak(this)
            parts += "Streak ${item.minHighestStreak} (melhor $best)"
        }
        return if (parts.isEmpty()) "Meta especial" else parts.joinToString(" • ")
    }

    private fun defaultDesc(item: PremiumItem): String = when (item.type) {
        PremiumType.THEME -> "Muda o visual geral do app"
        PremiumType.FRAME -> "Borda premium no seu avatar"
        PremiumType.TITLE -> "Título exibido no perfil"
        PremiumType.PET -> "Companheiro que evolui (lvl 1..3)"
        PremiumType.VFX -> "Efeito especial ao vencer"
    }

    private fun petUpgradeCost(nextLevel: Int): Int = when (nextLevel) {
        2 -> 250
        3 -> 450
        else -> 999
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
