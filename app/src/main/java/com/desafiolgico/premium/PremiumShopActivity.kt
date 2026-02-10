package com.desafiolgico.premium

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.PremiumCatalog
import com.desafiolgico.utils.PremiumManager
import com.desafiolgico.utils.applyEdgeToEdge
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class PremiumShopActivity : AppCompatActivity() {

    private lateinit var coinsText: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PremiumShopAdapter

    // Paleta (mantive seu estilo)
    private val cBgMid = Color.parseColor("#0F172A")
    private val cGlass = Color.parseColor("#14FFFFFF")
    private val cStrokeSoft = Color.parseColor("#24FFFFFF")
    private val cWhite = Color.WHITE
    private val cGold = Color.parseColor("#FBBF24")

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

        adapter = PremiumShopAdapter(
            onPurchase = { item ->
                runCatching {
                    val ok = PremiumManager.purchase(this, item)
                    if (ok) PremiumManager.applySelected(this, item)
                }
                refreshUi()
            },
            onApply = { item ->
                runCatching { PremiumManager.applySelected(this, item) }
                refreshUi()
            },
            onUpgradePet = { petId, cost ->
                runCatching {
                    val ok = PremiumManager.upgradePet(this, petId, cost)
                    if (ok) refreshUi()
                }
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        recycler.itemAnimator = null // menos “flicker” em updates rápidos
        recycler.setHasFixedSize(false)
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        // auto-unlock por conquista (não quebra a loja se algo falhar)
        runCatching {
            PremiumCatalog.all().forEach {
                PremiumManager.unlockByAchievementIfPossible(applicationContext, it)
            }
        }

        val coins = runCatching { CoinManager.getCoins(this) }.getOrElse { 0 }
        coinsText.text = "💰 $coins"

        adapter.submitData(
            context = this,
            coins = coins,
            sections = listOf(
                "🎨 Temas" to PremiumCatalog.themes,
                "🖼️ Molduras" to PremiumCatalog.frames,
                "🏷️ Títulos" to PremiumCatalog.titles,
                "🐾 Pets" to PremiumCatalog.pets,
                "✨ Efeitos (VFX)" to PremiumCatalog.vfx
            )
        )
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cBgMid)
        }

        // Header (glass)
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

        val backBtn = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "←"
            minWidth = 0
            minimumWidth = 0
            minimumHeight = dp(36)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setTextColor(cWhite)
            strokeWidth = dp(1)
            backgroundTintList = null
            setOnClickListener { finish() }
        }

        val title = TextView(this).apply {
            text = "Loja Premium"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(cWhite)
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
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

        recycler = RecyclerView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(dp(14), dp(10), dp(14), dp(18))
            clipToPadding = false
        }

        root.addView(
            recycler,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply { weight = 1f }
        )

        return root
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
