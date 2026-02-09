package com.desafiolgico.premium

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.desafiolgico.utils.dp

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.desafiolgico.utils.CoinManager
import com.desafiolgico.utils.GameDataManager
import com.desafiolgico.utils.PremiumItem
import com.desafiolgico.utils.PremiumManager
import com.desafiolgico.utils.PremiumType
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

sealed class PremiumRow {
    data class Header(val id: String, val title: String) : PremiumRow()
    data class Item(
        val id: String,
        val item: PremiumItem,
        val unlocked: Boolean,
        val selected: Boolean,
        val coins: Int
    ) : PremiumRow()
}

class PremiumShopAdapter(
    private val onPurchase: (PremiumItem) -> Unit,
    private val onApply: (PremiumItem) -> Unit,
    private val onUpgradePet: (petId: String, cost: Int) -> Unit
) : ListAdapter<PremiumRow, RecyclerView.ViewHolder>(Diff) {

    companion object {
        private const val TYPE_HEADER = 1
        private const val TYPE_ITEM = 2

        private val Diff = object : DiffUtil.ItemCallback<PremiumRow>() {
            override fun areItemsTheSame(oldItem: PremiumRow, newItem: PremiumRow): Boolean {
                return when {
                    oldItem is PremiumRow.Header && newItem is PremiumRow.Header -> oldItem.id == newItem.id
                    oldItem is PremiumRow.Item && newItem is PremiumRow.Item -> oldItem.id == newItem.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: PremiumRow, newItem: PremiumRow): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun submitData(
        context: Context,
        coins: Int,
        sections: List<Pair<String, List<PremiumItem>>>
    ) {
        val rows = mutableListOf<PremiumRow>()
        for ((title, items) in sections) {
            rows += PremiumRow.Header(id = "hdr_$title", title = title)
            for (it in items) {
                val unlocked = PremiumManager.isUnlocked(context, it)
                val selected = isSelected(context, it)
                rows += PremiumRow.Item(
                    id = "itm_${it.type}_${it.id}",
                    item = it,
                    unlocked = unlocked,
                    selected = selected,
                    coins = coins
                )
            }
        }
        submitList(rows)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PremiumRow.Header -> TYPE_HEADER
            is PremiumRow.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(buildHeaderView(parent.context))
            else -> ItemVH(buildItemView(parent.context))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is PremiumRow.Header -> (holder as HeaderVH).bind(row)
            is PremiumRow.Item -> (holder as ItemVH).bind(row, onPurchase, onApply, onUpgradePet)
        }
    }

    // -------------------------
    // Header
    // -------------------------
    private class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv = (view as TextView)

        fun bind(row: PremiumRow.Header) {
            tv.text = row.title
        }
    }

    private fun buildHeaderView(ctx: Context): View {
        return TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
            setTextColor(Color.WHITE)
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            setBackgroundColor(Color.parseColor("#14FFFFFF"))
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(ctx, 14)
                bottomMargin = dp(ctx, 6)
            }
        }
    }

    // -------------------------
    // Item
    // -------------------------
    private class ItemVH(view: View) : RecyclerView.ViewHolder(view) {

        private val card = view as MaterialCardView
        private val name: TextView
        private val desc: TextView
        private val status: TextView
        private val mainBtn: MaterialButton
        private val petLevelTv: TextView
        private val petUpBtn: MaterialButton

        init {
            val wrap = card.getChildAt(0) as LinearLayout
            val left = wrap.getChildAt(0) as LinearLayout
            val right = wrap.getChildAt(1) as LinearLayout

            name = left.getChildAt(0) as TextView
            desc = left.getChildAt(1) as TextView
            status = left.getChildAt(2) as TextView

            mainBtn = right.getChildAt(0) as MaterialButton
            petLevelTv = right.getChildAt(1) as TextView
            petUpBtn = right.getChildAt(2) as MaterialButton
        }

        fun bind(
            row: PremiumRow.Item,
            onPurchase: (PremiumItem) -> Unit,
            onApply: (PremiumItem) -> Unit,
            onUpgradePet: (String, Int) -> Unit
         ) {
            val ctx = itemView.context
            val item = row.item

            val cStrokeSoft = Color.parseColor("#24FFFFFF")
            val cGold = Color.parseColor("#FBBF24")
            val cGoldDeep = Color.parseColor("#F59E0B")
            val cBlue = Color.parseColor("#2563EB")
            val cMuted = Color.parseColor("#D1D5DB")
            val cMuted2 = Color.parseColor("#94A3B8")

            name.text = item.name
            desc.text = item.desc.takeIf { it.isNotBlank() } ?: defaultDesc(item)

            // Card stroke
            card.strokeWidth = if (row.selected) ctx.dp ( 2) else ctx.dp(1)
            card.strokeColor = if (row.selected) cGold else cStrokeSoft

            // Status
            status.setTextColor(
                when {
                    row.selected -> cGold
                    row.unlocked -> Color.parseColor("#93C5FD")
                    item.priceCoins > 0 -> Color.parseColor("#FCA5A5")
                    else -> cMuted2
                }
            )

            status.text = when {
                row.selected -> "✅ Selecionado"
                row.unlocked -> "🔓 Desbloqueado"
                else -> item.statusTextSafe(ctx)
            }

            // Main button
            mainBtn.setOnClickListener(null)
            mainBtn.isAllCaps = false
            mainBtn.minWidth = 0
            mainBtn.minimumWidth = 0
            mainBtn.minHeight = 0
            mainBtn.minimumHeight = 0

            when {
                !row.unlocked && item.priceCoins > 0 -> {
                    val falta = (item.priceCoins - row.coins).coerceAtLeast(0)
                    val pode = row.coins >= item.priceCoins

                    mainBtn.text = if (pode) "Comprar (${item.priceCoins})" else "Faltam $falta"
                    mainBtn.isEnabled = pode
                    mainBtn.alpha = if (pode) 1f else 0.7f
                    mainBtn.backgroundTintList = ColorStateList.valueOf(cGoldDeep)

                    if (pode) {
                        mainBtn.setOnClickListener {
                            onPurchase(item)
                            Toast.makeText(ctx, "Comprado e aplicado ✅", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                !row.unlocked && item.priceCoins <= 0 -> {
                    mainBtn.text = "Conquista"
                    mainBtn.isEnabled = false
                    mainBtn.alpha = 0.75f
                    mainBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#334155"))
                }

                row.unlocked && row.selected -> {
                    mainBtn.text = "Selecionado"
                    mainBtn.isEnabled = false
                    mainBtn.alpha = 0.85f
                    mainBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1F2937"))
                }

                row.unlocked && !row.selected -> {
                    mainBtn.text = "Aplicar"
                    mainBtn.isEnabled = true
                    mainBtn.alpha = 1f
                    mainBtn.backgroundTintList = ColorStateList.valueOf(cBlue)
                    mainBtn.setOnClickListener {
                        onApply(item)
                        Toast.makeText(ctx, "Aplicado ✅", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // PET controls
            if (item.type == PremiumType.PET && row.unlocked && item.id != "pet_none") {
                val lvl = GameDataManager.getPetLevel(ctx, item.id)
                petLevelTv.visibility = View.VISIBLE
                petUpBtn.visibility = View.VISIBLE

                petLevelTv.text = "Nível: $lvl/3"
                petLevelTv.setTextColor(cMuted)

                if (lvl >= 3) {
                    petUpBtn.visibility = View.GONE
                } else {
                    val next = lvl + 1
                    val cost = petUpgradeCost(next)
                    val coins = CoinManager.getCoins(ctx)
                    val pode = coins >= cost

                    petUpBtn.text = if (pode) "Up $next ($cost)" else "Faltam ${cost - coins}"
                    petUpBtn.isEnabled = pode
                    petUpBtn.alpha = if (pode) 1f else 0.7f
                    petUpBtn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#7C3AED"))

                    petUpBtn.setOnClickListener {
                        onUpgradePet(item.id, cost)
                        Toast.makeText(ctx, "Pet evoluiu! 🐾✨", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                petLevelTv.visibility = View.GONE
                petUpBtn.visibility = View.GONE
            }
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

        private fun PremiumItem.statusTextSafe(ctx: Context): String {
            // se você já tem item.statusText(context), use direto.
            // Se não tiver, cai num texto genérico:
            return runCatching { this.statusText(ctx) }.getOrElse {
                if (priceCoins > 0) "💰 ${priceCoins} moedas" else "Meta especial"
            }
        }
    }

    private fun buildItemView(ctx: Context): View {
        val cGlassCard = Color.parseColor("#141B2D")
        val cStrokeSoft = Color.parseColor("#24FFFFFF")

        val card = MaterialCardView(ctx).apply {
            radius = dp(ctx, 18).toFloat()
            cardElevation = dp(ctx, 2).toFloat()
            useCompatPadding = true
            setCardBackgroundColor(cGlassCard)
            strokeWidth = dp(ctx, 1)
            strokeColor = cStrokeSoft
            setContentPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(ctx, 10)
            }
        }

        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val left = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val name = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Color.WHITE)
        }

        val desc = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(Color.parseColor("#D1D5DB"))
            setPadding(0, dp(ctx, 4), 0, 0)
        }

        val status = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(ctx, 6), 0, 0)
            setTextColor(Color.parseColor("#94A3B8"))
        }

        left.addView(name)
        left.addView(desc)
        left.addView(status)

        val right = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        val mainBtn = MaterialButton(ctx).apply {
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            setTextColor(Color.WHITE)
            isAllCaps = false
        }

        val petLevelTv = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setPadding(0, dp(ctx, 8), 0, dp(ctx, 2))
            gravity = Gravity.END
            visibility = View.GONE
        }

        val petUpBtn = MaterialButton(ctx).apply {
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10))
            setTextColor(Color.WHITE)
            isAllCaps = false
            visibility = View.GONE
        }

        right.addView(mainBtn)
        right.addView(petLevelTv)
        right.addView(petUpBtn)

        wrap.addView(left)
        wrap.addView(right)
        card.addView(wrap)

        return card
    }

    private fun isSelected(ctx: Context, item: PremiumItem): Boolean = when (item.type) {
        PremiumType.THEME -> GameDataManager.getSelectedTheme(ctx) == item.id
        PremiumType.FRAME -> GameDataManager.getSelectedFrame(ctx) == item.id
        PremiumType.TITLE -> GameDataManager.getSelectedTitle(ctx) == item.id
        PremiumType.PET -> GameDataManager.getSelectedPet(ctx) == item.id
        PremiumType.VFX -> GameDataManager.getSelectedVfx(ctx) == item.id
    }

    private fun dp(ctx: Context, value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            ctx.resources.displayMetrics
        ).toInt()
}
