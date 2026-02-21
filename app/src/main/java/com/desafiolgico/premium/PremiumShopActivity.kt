// PremiumShopActivity.kt
package com.desafiolgico.premium

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.desafiolgico.databinding.ActivityPremiumShopBinding
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.LanguageHelper
import com.desafiolgico.utils.PremiumCatalog
import com.desafiolgico.utils.PremiumManager
import com.desafiolgico.utils.PremiumThemes
import com.desafiolgico.utils.applyEdgeToEdge
import com.desafiolgico.utils.applySystemBarsPadding

class PremiumShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumShopBinding
    private lateinit var adapter: PremiumShopAdapter

    // 🔤 garante idioma salvo nessa tela
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyEdgeToEdge(lightSystemBarIcons = false)

        binding = ActivityPremiumShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarsPadding(applyTop = true, applyBottom = true)

        // ✅ init do GameDataManager (se você usa isso no app)
        GameDataManager.init(this)

        // ✅ aplica tema premium (se existir no seu projeto)
        runCatching {
            PremiumThemes.apply(
                activity = this,
                root = binding.root,
                cardViews = listOf(binding.topBar, binding.coinCard)
            )
        }

        adapter = PremiumShopAdapter(
            onPurchase = { item ->
                PremiumManager.purchase(this, item)
                refresh()
            },
            onApply = { item ->
                PremiumManager.applySelected(this, item)
                refresh()
            },
            onUpgradePet = { petId, cost ->
                PremiumManager.upgradePet(this, petId, cost)
                refresh()
            }
        )

        binding.recyclerPremium.layoutManager = LinearLayoutManager(this)
        binding.recyclerPremium.adapter = adapter
        binding.recyclerPremium.itemAnimator = null // ✅ evita “piscadas” quando atualiza

        binding.btnBack.setOnClickListener { finish() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        // ✅ se o usuário ganha moedas em outra tela e volta, atualiza
        refresh()
    }

    private fun refresh() {
        val coins = CoinManager.getCoins(this)
        binding.txtCoins.text = coins.toString()

        val sections = listOf(
            getStringSafe("Temas") to PremiumCatalog.themes,
            getStringSafe("Molduras") to PremiumCatalog.frames,
            getStringSafe("Títulos") to PremiumCatalog.titles,
            getStringSafe("Pets") to PremiumCatalog.pets,
            getStringSafe("Efeitos") to PremiumCatalog.vfx
        )

        adapter.submitData(this, coins, sections)
    }

    private fun getStringSafe(fallback: String): String = fallback
}
