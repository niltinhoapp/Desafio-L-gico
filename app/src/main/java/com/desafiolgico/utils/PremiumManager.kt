package com.desafiolgico.utils

import android.content.Context
import android.util.Log

object PremiumManager {

    private const val TAG = "PremiumManager"

    private const val DEFAULT_THEME = "theme_default"
    private const val DEFAULT_FRAME = "frame_none"
    private const val DEFAULT_TITLE = "title_none"
    private const val DEFAULT_PET = "pet_none"
    private const val DEFAULT_VFX = "vfx_basic"

    fun isUnlocked(ctx: Context, item: PremiumItem): Boolean {
        val id = item.id
        return when (item.type) {
            PremiumType.THEME -> id == DEFAULT_THEME || GameDataManager.isThemeUnlocked(ctx, id)
            PremiumType.FRAME -> id == DEFAULT_FRAME || GameDataManager.isFrameUnlocked(ctx, id)
            PremiumType.TITLE -> id == DEFAULT_TITLE || GameDataManager.isTitleUnlocked(ctx, id)
            PremiumType.PET -> id == DEFAULT_PET || GameDataManager.isPetUnlocked(ctx, id)
            PremiumType.VFX -> id == DEFAULT_VFX || GameDataManager.isVfxUnlocked(ctx, id)
        }
    }

    /**
     * Tenta destravar via conquista (sem gastar moedas).
     * Retorna true se estiver (ou ficar) desbloqueado.
     */
    fun unlockByAchievementIfPossible(ctx: Context, item: PremiumItem): Boolean {
        if (isUnlocked(ctx, item)) return true

        // ✅ FIX CRÍTICO: nunca destrava item pago via conquista (a não ser que você queira híbrido)
        if (item.isPaid()) return false

        return try {
            if (!item.canUnlockByAchievement(ctx)) return false
            unlock(ctx, item)
            true
        } catch (e: Exception) {
            Log.w(TAG, "unlockByAchievementIfPossible failed: ${item.type}/${item.id}", e)
            false
        }
    }

    fun purchase(ctx: Context, item: PremiumItem): Boolean {
        val price = item.priceCoins
        if (price <= 0) return false
        if (isUnlocked(ctx, item)) return true

        return try {
            val ok = CoinManager.spendCoins(ctx, price)
            if (!ok) return false

            unlock(ctx, item)
            true
        } catch (e: Exception) {
            Log.w(TAG, "purchase failed: ${item.type}/${item.id}", e)
            false
        }
    }

    fun applySelected(ctx: Context, item: PremiumItem) {
        val unlocked = isUnlocked(ctx, item) || unlockByAchievementIfPossible(ctx, item)
        if (!unlocked) return

        try {
            when (item.type) {
                PremiumType.THEME -> GameDataManager.setSelectedTheme(ctx, item.id)
                PremiumType.FRAME -> GameDataManager.setSelectedFrame(ctx, item.id)
                PremiumType.TITLE -> GameDataManager.setSelectedTitle(ctx, item.id)
                PremiumType.PET -> GameDataManager.setSelectedPet(ctx, item.id)
                PremiumType.VFX -> GameDataManager.setSelectedVfx(ctx, item.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "applySelected failed: ${item.type}/${item.id}", e)
        }
    }

    fun upgradePet(ctx: Context, petId: String, cost: Int): Boolean {
        if (petId.isBlank()) return false
        if (petId == DEFAULT_PET) return false
        if (cost <= 0) return false

        return try {
            val unlocked = GameDataManager.isPetUnlocked(ctx, petId)
            if (!unlocked) return false

            val lvl = GameDataManager.getPetLevel(ctx, petId).coerceIn(1, 3)
            if (lvl >= 3) return false

            val ok = CoinManager.spendCoins(ctx, cost)
            if (!ok) return false

            GameDataManager.setPetLevel(ctx, petId, (lvl + 1).coerceIn(1, 3))
            true
        } catch (e: Exception) {
            Log.w(TAG, "upgradePet failed: $petId", e)
            false
        }
    }

    private fun unlock(ctx: Context, item: PremiumItem) {
        when (item.type) {
            PremiumType.THEME -> GameDataManager.unlockTheme(ctx, item.id)
            PremiumType.FRAME -> GameDataManager.unlockFrame(ctx, item.id)
            PremiumType.TITLE -> GameDataManager.unlockTitle(ctx, item.id)
            PremiumType.PET -> GameDataManager.unlockPet(ctx, item.id)
            PremiumType.VFX -> GameDataManager.unlockVfx(ctx, item.id)
        }
    }
}
