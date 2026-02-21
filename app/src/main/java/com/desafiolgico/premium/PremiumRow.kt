// PremiumRow.kt
package com.desafiolgico.premium

import com.desafiolgico.utils.PremiumItem

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
