package com.desafiolgico.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.desafiolgico.R
import java.time.LocalDate

object GameDataManager {

    private const val PREFS_NAME = "DesafioLogicoPrefs"

    // ---- Globais (sessão) ----
    private const val KEY_ACTIVE_USER_ID = "active_user_id"
    private const val KEY_MODO_SECRETO_ATIVO = "modo_secreto_ativo"
    private const val KEY_ULTIMO_NIVEL_NORMAL = "ultimo_nivel_normal"


    private const val KEY_CORRECT_BY_LEVEL = "correct_by_level_"


    private const val KEY_PREFER_AVATAR = "prefer_avatar_over_photo"

    private const val KEY_TOTAL_CORRECT = "total_correct"

    // =====================================================
// 📅 Daily Challenge (por usuário)
// =====================================================
    private const val KEY_DAILY_DATE = "daily_date"
    private const val KEY_DAILY_DONE = "daily_done"
    private const val KEY_DAILY_LAST_DONE_DATE = "daily_last_done_date"

    private const val KEY_DAILY_Q_DATE = "daily_q_date"
    private const val KEY_DAILY_Q1 = "daily_q1"
    private const val KEY_DAILY_Q2 = "daily_q2"
    private const val KEY_DAILY_Q3 = "daily_q3"
    private const val KEY_DAILY_DONE_DATE = "daily_done_date"
    private const val KEY_DAILY_STREAK = "daily_streak"

    private const val KEY_DAILY_XP = "daily_xp"
    // ---- Daily Result ----
    private const val KEY_DAILY_CORRECT = "daily_correct"
    private const val KEY_DAILY_SCORE = "daily_score"
    private const val KEY_DAILY_XP_EARNED = "daily_xp_earned"
    private const val KEY_DAILY_RESULT_DATE = "daily_result_date"


    // ---- Isoladas por usuário (via getUserKey) ----
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHOTO = "user_photo"
    private const val KEY_USER_AVATAR = "user_avatar"
    private const val KEY_TOTAL_SCORE = "total_score"
    private const val KEY_COINS = "coins"
    private const val KEY_XP = "xp"
    private const val KEY_UNLOCKED_LEVELS = "unlocked_levels"
    private const val KEY_HIGHEST_STREAK = "highest_streak"
    private const val KEY_LAST_QUESTION_INDEX = "last_q_index"
    private const val KEY_TOTAL_CORRECT_GLOBAL = "total_correct_global" // ✅ para o Mapa (por usuário)

    private const val KEY_UNLOCKED_AVATARS = "unlocked_avatars"

    private var prefs: SharedPreferences? = null
    private var contextGlobal: Context? = null

    /**
     * UID atual (Firebase UID). Se não logado => "guest".
     * IMPORTANTE: NÃO use username como ID.
     */
    var currentUserId: String = "guest"
        private set

    fun init(context: Context) {
        if (prefs == null) {
            prefs =
                context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            contextGlobal = context.applicationContext

            // restaura usuário ativo (se existir)
            val saved = prefs?.getString(KEY_ACTIVE_USER_ID, null)
            currentUserId = saved?.takeIf { it.isNotBlank() } ?: "guest"

            Log.d("GameDataManager", "✅ Inicializado. activeUser=$currentUserId")
        }
    }

    private fun getPrefs(context: Context): SharedPreferences =
        prefs ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun sanitizeUserId(raw: String?): String {
        val id = raw?.trim().takeUnless { it.isNullOrBlank() } ?: "guest"
        return id.replace("\\W+".toRegex(), "_")
    }

    private fun getUserKey(key: String): String {
        val id = sanitizeUserId(currentUserId)
        return "${id}_$key"
    }

    /** Troca usuário ativo (use no login/logout). */
    fun setActiveUserId(context: Context, uid: String?) {
        val finalId = uid?.trim().takeUnless { it.isNullOrBlank() } ?: "guest"
        currentUserId = finalId
        getPrefs(context).edit().putString(KEY_ACTIVE_USER_ID, finalId).apply()
        Log.d("GameDataManager", "👤 activeUserId = $currentUserId")
    }

    // Estado de sessão (memória)
    var currentStreak: Int = 0
    var totalErrors: Int = 0

    // =====================================================
    // ⚙️ Flags de Sessão
    // =====================================================
    var ultimoNivelNormal: String?
        get() = prefs?.getString(KEY_ULTIMO_NIVEL_NORMAL, null)
        set(value) {
            prefs?.edit()?.putString(KEY_ULTIMO_NIVEL_NORMAL, value)?.apply()
        }

    var isModoSecretoAtivo: Boolean
        get() = prefs?.getBoolean(KEY_MODO_SECRETO_ATIVO, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_MODO_SECRETO_ATIVO, value)?.apply()
        }

    fun getUltimoNivelNormal(context: Context): String? =
        getPrefs(context).getString(KEY_ULTIMO_NIVEL_NORMAL, null)

    fun setUltimoNivelNormal(context: Context, level: String) {
        getPrefs(context).edit().putString(KEY_ULTIMO_NIVEL_NORMAL, level).apply()
    }

    fun clearUltimoNivelNormal(context: Context) {
        getPrefs(context).edit().remove(KEY_ULTIMO_NIVEL_NORMAL).apply()
    }

    // =====================================================
    // 👤 Dados do Usuário (por UID)
    // =====================================================
    fun saveUserData(context: Context, username: String?, photoUrl: String?, avatarId: Int?) {
        val p = getPrefs(context)

        p.edit().apply {
            // username por usuário (não global)
            putString(getUserKey(KEY_USER_NAME), username)

            // foto: salva se vier
            if (!photoUrl.isNullOrBlank()) {
                putString(getUserKey(KEY_USER_PHOTO), photoUrl)
            }

            // avatar: opcional (0/null remove)
            // avatar: só salva se o usuário PREFERE avatar
            val preferAvatar = isPreferAvatar(context)

            if (preferAvatar && avatarId != null && avatarId > 0) {
                putInt(getUserKey(KEY_USER_AVATAR), avatarId)
            } else {
                remove(getUserKey(KEY_USER_AVATAR))
            }


            apply()
        }

        Log.d("GameDataManager", "💾 saveUserData OK (uid=$currentUserId)")
    }

    fun loadUserData(context: Context): Triple<String?, String?, Int?> {
        val p = getPrefs(context)

        val username = p.getString(getUserKey(KEY_USER_NAME), null)
        val photoUrl = p.getString(getUserKey(KEY_USER_PHOTO), null)

        val preferAvatar = isPreferAvatar(context)
        val avatarId = if (preferAvatar) {
            p.getInt(getUserKey(KEY_USER_AVATAR), 0).takeIf { it > 0 }
        } else null

        return Triple(username, photoUrl, avatarId)
    }

    /** Remove avatar escolhido (volta a usar a foto do Google) */
    fun clearUserAvatar(context: Context) {
        getPrefs(context).edit().remove(getUserKey(KEY_USER_AVATAR)).apply()
    }

    // =====================================================
    // 🔓 Níveis desbloqueáveis
    // =====================================================
    fun unlockLevel(context: Context, level: String) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_UNLOCKED_LEVELS)
        val set = (p.getStringSet(key, mutableSetOf()) ?: mutableSetOf()).toMutableSet()
        if (set.add(level)) {
            p.edit().putStringSet(key, set).apply()
            Log.d("GameDataManager", "🔓 Nível desbloqueado: $level (uid=$currentUserId)")
        }
    }

    fun isLevelUnlocked(context: Context, level: String): Boolean {
        if (level == Levels.INICIANTE) return true
        val key = getUserKey(KEY_UNLOCKED_LEVELS)
        val set = getPrefs(context).getStringSet(key, setOf(Levels.INICIANTE))
        return set?.contains(level) == true
    }

    fun getUnlockedLevels(context: Context): Set<String> {
        val key = getUserKey(KEY_UNLOCKED_LEVELS)
        return getPrefs(context).getStringSet(key, setOf(Levels.INICIANTE))
            ?: setOf(Levels.INICIANTE)
    }

    // =====================================================
    // 🏆 Recorde (Highest Streak)
    // =====================================================
    fun getHighestStreak(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_HIGHEST_STREAK), 0)

    fun updateHighestStreakIfNeeded(context: Context, newStreak: Int) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_HIGHEST_STREAK)
        val current = p.getInt(key, 0)
        if (newStreak > current) p.edit().putInt(key, newStreak).apply()
    }

    // =====================================================
    // 💰 Moedas e XP
    // =====================================================
    fun addCoins(context: Context, amount: Int) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_COINS)
        val total = p.getInt(key, 0) + amount
        p.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getCoins(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_COINS), 0)

    fun addXP(context: Context, amount: Int) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_XP)
        val total = p.getInt(key, 0) + amount
        p.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getXP(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_XP), 0)

    // =====================================================
    // 📊 Pontuação total
    // =====================================================
    fun addScoreToOverallTotal(context: Context, points: Int) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_TOTAL_SCORE)
        val total = p.getInt(key, 0) + points

        p.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    // =====================================================
// 🗺️ Progresso do Mapa (por usuário)
// - cada acerto soma +1 (persistente)
// - step = total / 10
// =====================================================
    fun incrementTotalCorrectGlobal(context: Context, delta: Int = 1) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_TOTAL_CORRECT_GLOBAL)
        val total = (p.getInt(key, 0) + delta).coerceAtLeast(0)
        p.edit().putInt(key, total).apply()
    }

    fun getTotalCorrectGlobal(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_TOTAL_CORRECT_GLOBAL), 0)

    fun getMapStep(context: Context, stepSize: Int = 10): Int {
        if (stepSize <= 0) return 0
        return getTotalCorrectGlobal(context) / stepSize
    }

    fun resetMapProgress(context: Context) {
        getPrefs(context).edit().remove(getUserKey(KEY_TOTAL_CORRECT_GLOBAL)).apply()
    }


    fun getOverallTotalScore(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_TOTAL_SCORE), 0)

    // =====================================================
    // 💾 Progresso por nível
    // =====================================================
    fun saveLastQuestionIndex(context: Context, levelName: String, index: Int) {
        val baseKey = "${levelName.replace("\\s+".toRegex(), "_")}_$KEY_LAST_QUESTION_INDEX"
        getPrefs(context).edit().putInt(getUserKey(baseKey), index).apply()
    }

    fun loadLastQuestionIndex(context: Context, levelName: String): Int {
        val baseKey = "${levelName.replace("\\s+".toRegex(), "_")}_$KEY_LAST_QUESTION_INDEX"
        return getPrefs(context).getInt(getUserKey(baseKey), 0)
    }

    fun clearLastQuestionIndex(context: Context, levelName: String) {
        val baseKey = "${levelName.replace("\\s+".toRegex(), "_")}_$KEY_LAST_QUESTION_INDEX"
        getPrefs(context).edit().remove(getUserKey(baseKey)).apply()
    }

    // =====================================================
    // 🎨 Avatares desbloqueados (por usuário)
    // =====================================================
    fun unlockAvatar(context: Context, avatarId: Int) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_UNLOCKED_AVATARS)
        val set = (p.getStringSet(key, mutableSetOf()) ?: mutableSetOf()).toMutableSet()
        if (set.add(avatarId.toString())) {
            p.edit().putStringSet(key, set).apply()
        }
    }

    fun isAvatarUnlocked(context: Context, avatarId: Int): Boolean {
        if (avatarId == R.drawable.avatar1) return true

        val p = getPrefs(context)
        val key = getUserKey(KEY_UNLOCKED_AVATARS)
        val set = p.getStringSet(key, emptySet())
        return set?.contains(avatarId.toString()) == true
    }

    // =====================================================
    // 🔁 Logout / Reset
    // =====================================================
    fun clearSession(context: Context) {
        // só “desloga” (não apaga progresso)
        setActiveUserId(context, "guest")
        Log.d("GameDataManager", "🧹 Sessão limpa. activeUser=guest")
    }
    // =====================================================
// 📅 Daily Challenge (por usuário) - FONTE ÚNICA
// =====================================================


    private fun todayKey(): String = java.time.LocalDate.now().toString() // ex: 2026-01-02

    fun isDailyDone(context: Context): Boolean {
        val p = getPrefs(context)
        val last = p.getString(getUserKey(KEY_DAILY_DONE_DATE), null)
        return last == todayKey()
    }

    fun getDailyStreak(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_DAILY_STREAK), 0)

    fun markDailyDone(context: Context) {
        val p = getPrefs(context)
        val today = todayKey()

        val last = p.getString(getUserKey(KEY_DAILY_DONE_DATE), null)
        val currentStreak = p.getInt(getUserKey(KEY_DAILY_STREAK), 0)

        val yesterday = java.time.LocalDate.now().minusDays(1).toString()

        val newStreak = when {
            last == today -> currentStreak          // já marcado hoje
            last == yesterday -> currentStreak + 1  // continua streak
            else -> 1                               // reseta
        }

        p.edit()
            .putString(getUserKey(KEY_DAILY_DONE_DATE), today)
            .putInt(getUserKey(KEY_DAILY_STREAK), newStreak)
            .apply()
    }

    fun saveDailyResult(context: Context, correct: Int, score: Int, xpEarned: Int) {
        val p = getPrefs(context)
        p.edit()
            .putString(getUserKey(KEY_DAILY_RESULT_DATE), todayKey())
            .putInt(getUserKey(KEY_DAILY_CORRECT), correct)
            .putInt(getUserKey(KEY_DAILY_SCORE), score)
            .putInt(getUserKey(KEY_DAILY_XP_EARNED), xpEarned)
            .apply()
    }

    fun addTotalCorrect(context: Context, amount: Int = 1) {
        val p = getPrefs(context)
        val key = getUserKey(KEY_TOTAL_CORRECT)
        val total = p.getInt(key, 0) + amount
        p.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getTotalCorrect(context: Context): Int =
        getPrefs(context).getInt(getUserKey(KEY_TOTAL_CORRECT), 0)


    fun getDailyLastResult(context: Context): Triple<Int, Int, Int> {
        val p = getPrefs(context)
        val correct = p.getInt(getUserKey(KEY_DAILY_CORRECT), 0)
        val score = p.getInt(getUserKey(KEY_DAILY_SCORE), 0)
        val xp = p.getInt(getUserKey(KEY_DAILY_XP_EARNED), 0)
        return Triple(correct, score, xp)
    }

    /**
     * ✅ Gera/recupera as 3 perguntas do dia (mesmas o dia todo)
     * - estável por DIA + USUÁRIO
     * - salva pelo questionText (não mexe no TestActivity)
     */
    fun getDailyQuestions(
        context: Context,
        questionManager: com.desafiolgico.model.QuestionManager,
        unlockedLevels: Set<String>
    ): List<com.desafiolgico.model.Question> {

        val p = getPrefs(context)
        val today = todayKey()

        // Pool
        val pool = unlockedLevels
            .flatMap { lvl -> questionManager.getQuestionsByLevel(lvl) }
            .distinctBy { it.questionText.trim() }

        if (pool.size < 3) return pool

        // Se já existe hoje, tenta restaurar
        val savedDate = p.getString(getUserKey(KEY_DAILY_Q_DATE), null)
        if (savedDate == today) {
            val t1 = p.getString(getUserKey(KEY_DAILY_Q1), null)
            val t2 = p.getString(getUserKey(KEY_DAILY_Q2), null)
            val t3 = p.getString(getUserKey(KEY_DAILY_Q3), null)

            val restored = listOfNotNull(
                pool.firstOrNull { it.questionText == t1 },
                pool.firstOrNull { it.questionText == t2 },
                pool.firstOrNull { it.questionText == t3 }
            )
            if (restored.size == 3) return restored
        }

        // Gera determinístico por dia + usuário
        val seed = today.hashCode() + (currentUserId.hashCode() * 31)
        val rnd = kotlin.random.Random(seed)
        val selected = pool.shuffled(rnd).take(3)

        // Salva para ficar igual o dia todo
        p.edit()
            .putString(getUserKey(KEY_DAILY_Q_DATE), today)
            .putString(getUserKey(KEY_DAILY_Q1), selected[0].questionText)
            .putString(getUserKey(KEY_DAILY_Q2), selected[1].questionText)
            .putString(getUserKey(KEY_DAILY_Q3), selected[2].questionText)
            .apply()

        return selected
    }
    // =====================================================
// 🧹 Reset local (por usuário)
// =====================================================

    /**
     * Apaga SOMENTE os dados locais do usuário atual (UID atual).
     * Não apaga o app todo e não mexe em outros usuários.
     */
    fun resetUserLocalData(context: Context) {
        val p = getPrefs(context)
        val id = sanitizeUserId(currentUserId)

        val keysToRemove = listOf(
            "${id}_$KEY_USER_NAME",
            "${id}_$KEY_USER_PHOTO",
            "${id}_$KEY_USER_AVATAR",
            "${id}_$KEY_TOTAL_SCORE",
            "${id}_$KEY_COINS",
            "${id}_$KEY_XP",
            "${id}_$KEY_UNLOCKED_LEVELS",
            "${id}_$KEY_HIGHEST_STREAK",
            "${id}_$KEY_UNLOCKED_AVATARS",
            "${id}_$KEY_TOTAL_CORRECT_GLOBAL",


            // Daily (se você adicionou essas keys)
            "${id}_$KEY_DAILY_DONE_DATE",
            "${id}_$KEY_DAILY_STREAK",
            "${id}_$KEY_DAILY_RESULT_DATE",
            "${id}_$KEY_DAILY_CORRECT",
            "${id}_$KEY_DAILY_SCORE",
            "${id}_$KEY_DAILY_XP_EARNED",
            "${id}_$KEY_DAILY_Q_DATE",
            "${id}_$KEY_DAILY_Q1",
            "${id}_$KEY_DAILY_Q2",
            "${id}_$KEY_DAILY_Q3"
        )

        p.edit().apply {
            keysToRemove.forEach { remove(it) }
            apply()
        }

        // Reseta variáveis de sessão em memória
        currentStreak = 0
        totalErrors = 0
        // ✅ zera progresso do mapa por nível
        resetCorrectForAllLevels(context)
    }

    // --- Map progress: correct answers per level (per user) ---



    // =====================================================
// 🗺️ MAPA: Acertos por nível (por usuário) - 30 por nível
// =====================================================

    private fun sanitizeLevelKey(level: String): String {
        // remove acentos/símbolos de forma simples (Intermediário -> Intermedi_rio)
        return level.trim().replace("\\W+".toRegex(), "_")
    }

    private fun correctKeyForLevel(level: String): String {
        // KEY_CORRECT_BY_LEVEL já tem "_" no final no seu arquivo
        return getUserKey(KEY_CORRECT_BY_LEVEL + sanitizeLevelKey(level))
    }

    fun getCorrectForLevel(context: Context, level: String): Int {
        return getPrefs(context).getInt(correctKeyForLevel(level), 0)
    }

    fun setCorrectForLevel(context: Context, level: String, value: Int) {
        getPrefs(context).edit()
            .putInt(correctKeyForLevel(level), value.coerceAtLeast(0))
            .apply()
    }

    fun addCorrectForLevel(context: Context, level: String, delta: Int = 1) {
        if (delta == 0) return
        val p = getPrefs(context)
        val key = correctKeyForLevel(level)
        val updated = (p.getInt(key, 0) + delta).coerceAtLeast(0)
        p.edit().putInt(key, updated).apply()
    }

    fun resetCorrectForLevel(context: Context, level: String) {
        getPrefs(context).edit().remove(correctKeyForLevel(level)).apply()
    }

    fun resetCorrectForAllLevels(context: Context) {
        resetCorrectForLevel(context, Levels.INICIANTE)
        resetCorrectForLevel(context, Levels.INTERMEDIARIO)
        resetCorrectForLevel(context, Levels.AVANCADO)
        resetCorrectForLevel(context, Levels.EXPERIENTE)
    }

    fun getTotalCorrectAllLevels(context: Context): Int {
        return listOf(Levels.INICIANTE, Levels.INTERMEDIARIO, Levels.AVANCADO, Levels.EXPERIENTE)
            .sumOf { getCorrectForLevel(context, it) }
    }






    fun setPreferAvatar(context: Context, prefer: Boolean) {
        getPrefs(context).edit()
            .putBoolean(getUserKey(KEY_PREFER_AVATAR), prefer)
            .apply()
    }

    fun isPreferAvatar(context: Context): Boolean {
        return getPrefs(context).getBoolean(getUserKey(KEY_PREFER_AVATAR), false)
    }

    /**
     * Mantém compatibilidade com chamadas antigas: GameDataManager.resetAll(context)
     * Faz reset do usuário atual.
     */
    fun resetAll(context: Context) {
        resetUserLocalData(context)
    }

    // =====================================================
// 💎 Cosmetics Premium (Temas / Molduras / Títulos / Pets / VFX) - por usuário
// =====================================================
    private const val KEY_SELECTED_THEME = "selected_theme"
    private const val KEY_SELECTED_FRAME = "selected_frame"
    private const val KEY_SELECTED_TITLE = "selected_title"
    private const val KEY_SELECTED_PET   = "selected_pet"
    private const val KEY_SELECTED_VFX   = "selected_vfx"

    private const val KEY_UNLOCKED_THEMES = "unlocked_themes"
    private const val KEY_UNLOCKED_FRAMES = "unlocked_frames"
    private const val KEY_UNLOCKED_TITLES = "unlocked_titles"
    private const val KEY_UNLOCKED_PETS   = "unlocked_pets"
    private const val KEY_UNLOCKED_VFX    = "unlocked_vfx"

    private const val KEY_PET_LEVEL_PREFIX = "pet_level_" // ex: pet_level_pet_owl

    private fun getUserStringSet(context: Context, key: String, def: Set<String> = emptySet()): MutableSet<String> {
        val p = getPrefs(context)
        val set = p.getStringSet(getUserKey(key), def) ?: def
        return set.toMutableSet()
    }

    private fun putUserStringSet(context: Context, key: String, value: Set<String>) {
        getPrefs(context).edit().putStringSet(getUserKey(key), value).apply()
    }

    fun trySpendCoins(context: Context, amount: Int): Boolean {
        if (amount <= 0) return true
        val current = getCoins(context)
        if (current < amount) return false
        addCoins(context, -amount)
        return true
    }

    // ---- Unlock helpers ----
    private fun unlockString(context: Context, setKey: String, id: String) {
        val s = getUserStringSet(context, setKey)
        if (s.add(id)) putUserStringSet(context, setKey, s)
    }

    private fun isStringUnlocked(context: Context, setKey: String, id: String): Boolean {
        val s = getPrefs(context).getStringSet(getUserKey(setKey), emptySet()) ?: emptySet()
        return s.contains(id)
    }

    // ---- Themes ----
    fun unlockTheme(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_THEMES, id)
    fun isThemeUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_THEMES, id)
    fun setSelectedTheme(context: Context, id: String) = getPrefs(context).edit().putString(getUserKey(KEY_SELECTED_THEME), id).apply()
    fun getSelectedTheme(context: Context): String = getPrefs(context).getString(getUserKey(KEY_SELECTED_THEME), "theme_default") ?: "theme_default"

    // ---- Frames ----
    fun unlockFrame(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_FRAMES, id)
    fun isFrameUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_FRAMES, id)
    fun setSelectedFrame(context: Context, id: String) = getPrefs(context).edit().putString(getUserKey(KEY_SELECTED_FRAME), id).apply()
    fun getSelectedFrame(context: Context): String = getPrefs(context).getString(getUserKey(KEY_SELECTED_FRAME), "frame_none") ?: "frame_none"

    // ---- Titles ----
    fun unlockTitle(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_TITLES, id)
    fun isTitleUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_TITLES, id)
    fun setSelectedTitle(context: Context, id: String) = getPrefs(context).edit().putString(getUserKey(KEY_SELECTED_TITLE), id).apply()
    fun getSelectedTitle(context: Context): String = getPrefs(context).getString(getUserKey(KEY_SELECTED_TITLE), "title_none") ?: "title_none"

    // ---- Pets ----
    fun unlockPet(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_PETS, id)
    fun isPetUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_PETS, id)
    fun setSelectedPet(context: Context, id: String) = getPrefs(context).edit().putString(getUserKey(KEY_SELECTED_PET), id).apply()
    fun getSelectedPet(context: Context): String = getPrefs(context).getString(getUserKey(KEY_SELECTED_PET), "pet_none") ?: "pet_none"

    fun getPetLevel(context: Context, petId: String): Int {
        val k = getUserKey(KEY_PET_LEVEL_PREFIX + petId)
        return getPrefs(context).getInt(k, 1).coerceIn(1, 3)
    }
    fun setPetLevel(context: Context, petId: String, lvl: Int) {
        val k = getUserKey(KEY_PET_LEVEL_PREFIX + petId)
        getPrefs(context).edit().putInt(k, lvl.coerceIn(1, 3)).apply()
    }

    // ---- VFX ----
    fun unlockVfx(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_VFX, id)
    fun isVfxUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_VFX, id)
    fun setSelectedVfx(context: Context, id: String) = getPrefs(context).edit().putString(getUserKey(KEY_SELECTED_VFX), id).apply()
    fun getSelectedVfx(context: Context): String = getPrefs(context).getString(getUserKey(KEY_SELECTED_VFX), "vfx_basic") ?: "vfx_basic"



    object Levels {
        const val INICIANTE = "Iniciante"
        const val INTERMEDIARIO = "Intermediário"
        const val AVANCADO = "Avançado"
        const val EXPERIENTE = "Experiente"
    }


    object SecretLevels {
        const val RELAMPAGO = "Relâmpago"
        const val PERFEICAO = "Perfeição"
        const val ENIGMA = "Enigma"
    }
}
