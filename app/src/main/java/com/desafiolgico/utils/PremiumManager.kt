package com.desafiolgico.utils

import android.content.Context
import android.util.Log

object PremiumManager {

    private const val TAG = "PremiumManager"

    const val DEFAULT_THEME = "theme_default"
    const val DEFAULT_FRAME = "frame_none"
    const val DEFAULT_TITLE = "title_none"
    const val DEFAULT_PET = "pet_none"
    const val DEFAULT_VFX = "vfx_basic"

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
     * Conquista (sem moedas). Itens pagos NÃO destravam aqui.
     */
    fun unlockByAchievementIfPossible(ctx: Context, item: PremiumItem): Boolean {
        if (isUnlocked(ctx, item)) return true
        if (item.isPaid()) return false // ✅ regra: pago só compra

        return try {
            if (!item.canUnlockByAchievement(ctx)) return false
            unlock(ctx, item)
            true
        } catch (e: Exception) {
            Log.w(TAG, "unlockByAchievement failed: ${item.type}/${item.id}", e)
            false
        }
    }

    /**
     * Compra com moedas (apenas se priceCoins > 0)
     */
    fun purchase(ctx: Context, item: PremiumItem): Boolean {
        val price = item.priceCoins.coerceAtLeast(0)
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

    /**
     * Aplica (seleciona). Se for conquista e estiver apto, destrava e aplica.
     */
    fun applySelected(ctx: Context, item: PremiumItem) {
        try {
            val unlocked = isUnlocked(ctx, item) || unlockByAchievementIfPossible(ctx, item)
            if (!unlocked) return

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
        if (petId.isBlank() || petId == DEFAULT_PET) return false
        if (cost <= 0) return false

        return try {
            if (!GameDataManager.isPetUnlocked(ctx, petId)) return false

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

    // ✅ opcional: fallback rápido se algum id aplicado não existir mais no catálogo
    fun ensureDefaultsIfMissing(ctx: Context) {
        runCatching {
            if (PremiumCatalog.find(GameDataManager.getSelectedTheme(ctx)) == null)
                GameDataManager.setSelectedTheme(ctx, DEFAULT_THEME)
            if (PremiumCatalog.find(GameDataManager.getSelectedFrame(ctx)) == null)
                GameDataManager.setSelectedFrame(ctx, DEFAULT_FRAME)
            if (PremiumCatalog.find(GameDataManager.getSelectedTitle(ctx)) == null)
                GameDataManager.setSelectedTitle(ctx, DEFAULT_TITLE)
            if (PremiumCatalog.find(GameDataManager.getSelectedPet(ctx)) == null)
                GameDataManager.setSelectedPet(ctx, DEFAULT_PET)
            if (PremiumCatalog.find(GameDataManager.getSelectedVfx(ctx)) == null)
                GameDataManager.setSelectedVfx(ctx, DEFAULT_VFX)
        }
    }
}
