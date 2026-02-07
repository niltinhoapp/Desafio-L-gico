package com.desafiolgico.utils

import android.content.Context

object PremiumManager {

    fun isUnlocked(ctx: Context, item: PremiumItem): Boolean {
        return when (item.type) {
            PremiumType.THEME -> item.id == "theme_default" || GameDataManager.isThemeUnlocked(ctx, item.id)
            PremiumType.FRAME -> item.id == "frame_none"    || GameDataManager.isFrameUnlocked(ctx, item.id)
            PremiumType.TITLE -> item.id == "title_none"    || GameDataManager.isTitleUnlocked(ctx, item.id)
            PremiumType.PET   -> item.id == "pet_none"      || GameDataManager.isPetUnlocked(ctx, item.id)
            PremiumType.VFX   -> item.id == "vfx_basic"     || GameDataManager.isVfxUnlocked(ctx, item.id)
        }
    }

    fun unlockByAchievementIfPossible(ctx: Context, item: PremiumItem): Boolean {
        if (isUnlocked(ctx, item)) return true
        if (!item.canUnlockByAchievement(ctx)) return false

        when (item.type) {
            PremiumType.THEME -> GameDataManager.unlockTheme(ctx, item.id)
            PremiumType.FRAME -> GameDataManager.unlockFrame(ctx, item.id)
            PremiumType.TITLE -> GameDataManager.unlockTitle(ctx, item.id)
            PremiumType.PET   -> GameDataManager.unlockPet(ctx, item.id)
            PremiumType.VFX   -> GameDataManager.unlockVfx(ctx, item.id)
        }
        return true
    }

    fun purchase(context: Context, item: PremiumItem): Boolean {
        if (item.priceCoins <= 0) return false
        if (isUnlocked(context, item)) return true

        val ok = CoinManager.spendCoins(context, item.priceCoins)
        if (!ok) return false

        setUnlocked(context, item, true)
        return true
    }

    fun applySelected(ctx: Context, item: PremiumItem) {
        if (!isUnlocked(ctx, item) && !unlockByAchievementIfPossible(ctx, item)) return

        when (item.type) {
            PremiumType.THEME -> GameDataManager.setSelectedTheme(ctx, item.id)
            PremiumType.FRAME -> GameDataManager.setSelectedFrame(ctx, item.id)
            PremiumType.TITLE -> GameDataManager.setSelectedTitle(ctx, item.id)
            PremiumType.PET   -> GameDataManager.setSelectedPet(ctx, item.id)
            PremiumType.VFX   -> GameDataManager.setSelectedVfx(ctx, item.id)
        }
    }

    // Upgrade do pet (lvl 1..3)
    fun upgradePet(context: Context, petId: String, cost: Int): Boolean {
        if (cost <= 0) return false


        val lvl = GameDataManager.getPetLevel(context, petId)
        if (lvl >= 3) return false

        val ok = CoinManager.spendCoins(context, cost)
        if (!ok) return false


        GameDataManager.setPetLevel(context, petId, lvl + 1)
        return true
    }

    // Novo método para resolver o erro "Unresolved reference: setUnlocked"
    private fun setUnlocked(ctx: Context, item: PremiumItem, unlocked: Boolean) {
        if (!unlocked) return  // só tratamos desbloqueio

        when (item.type) {
            PremiumType.THEME -> GameDataManager.unlockTheme(ctx, item.id)
            PremiumType.FRAME -> GameDataManager.unlockFrame(ctx, item.id)
            PremiumType.TITLE -> GameDataManager.unlockTitle(ctx, item.id)
            PremiumType.PET   -> GameDataManager.unlockPet(ctx, item.id)
            PremiumType.VFX   -> GameDataManager.unlockVfx(ctx, item.id)
        }
    }
}
