package com.desafiolgico.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.desafiolgico.R
import java.time.LocalDate
import java.util.HashSet
import java.util.Locale

object GameDataManager {

    private const val TAG = "GameDataManager"
    private const val PREFS_NAME = "DesafioLogicoPrefs"

    // ---- Globais (sessão) ----
    private const val KEY_ACTIVE_USER_ID = "active_user_id"
    private const val KEY_MODO_SECRETO_ATIVO = "modo_secreto_ativo"
    private const val KEY_ULTIMO_NIVEL_NORMAL = "ultimo_nivel_normal"

    // ---- Progresso / mapa ----
    private const val KEY_CORRECT_BY_LEVEL = "correct_by_level_" // + levelKey
    private const val KEY_TOTAL_CORRECT = "total_correct"
    private const val KEY_TOTAL_CORRECT_GLOBAL = "total_correct_global" // ✅ Mapa (por usuário)

    // ---- Perfil ----
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_PHOTO = "user_photo"
    private const val KEY_USER_AVATAR = "user_avatar"
    private const val KEY_PREFER_AVATAR = "prefer_avatar_over_photo"

    // ---- Economia / progresso ----
    private const val KEY_TOTAL_SCORE = "total_score"
    private const val KEY_COINS = "coins"
    private const val KEY_XP = "xp"
    private const val KEY_UNLOCKED_LEVELS = "unlocked_levels"
    private const val KEY_HIGHEST_STREAK = "highest_streak"
    private const val KEY_LAST_QUESTION_INDEX = "last_q_index"
    private const val KEY_UNLOCKED_AVATARS = "unlocked_avatars"

    // =====================================================
    // 📅 Daily Challenge (por usuário) - fonte única (SECURE)
    // =====================================================
    private const val KEY_DAILY_Q_DATE = "daily_q_date"
    private const val KEY_DAILY_Q1 = "daily_q1"
    private const val KEY_DAILY_Q2 = "daily_q2"
    private const val KEY_DAILY_Q3 = "daily_q3"

    private const val KEY_DAILY_DONE_DATE = "daily_done_date"
    private const val KEY_DAILY_STREAK = "daily_streak"

    private const val KEY_DAILY_CORRECT = "daily_correct"
    private const val KEY_DAILY_SCORE = "daily_score"
    private const val KEY_DAILY_XP_EARNED = "daily_xp_earned"
    private const val KEY_DAILY_RESULT_DATE = "daily_result_date"

    // =====================================================
    // 💎 Cosmetics Premium (por usuário) (SECURE)
    // =====================================================
    private const val KEY_SELECTED_THEME = "selected_theme"
    private const val KEY_SELECTED_FRAME = "selected_frame"
    private const val KEY_SELECTED_TITLE = "selected_title"
    private const val KEY_SELECTED_PET = "selected_pet"
    private const val KEY_SELECTED_VFX = "selected_vfx"

    private const val KEY_UNLOCKED_THEMES = "unlocked_themes"
    private const val KEY_UNLOCKED_FRAMES = "unlocked_frames"
    private const val KEY_UNLOCKED_TITLES = "unlocked_titles"
    private const val KEY_UNLOCKED_PETS = "unlocked_pets"
    private const val KEY_UNLOCKED_VFX = "unlocked_vfx"

    private const val KEY_PET_LEVEL_PREFIX = "pet_level_" // ex: pet_level_pet_owl

    // =====================================================
    // 🔁 Anti-farm (seen/scored) - por usuário + por nível (SECURE)
    // (Compatível com seu TestActivity atual)
    // =====================================================
    private const val KEY_SEEN_PREFIX = "seen_"
    private const val KEY_SCORED_PREFIX = "scored_"

    // =====================================================
    // MIGRAÇÃO -> seguro
    // =====================================================
    private const val KEY_SECURE_MIGRATION_DONE = "secure_migration_done_v1"

    private var prefs: SharedPreferences? = null
    private var contextGlobal: Context? = null

    /**
     * UID atual (Firebase UID). Se não logado => "guest".
     * IMPORTANTE: NÃO use username como ID.
     */
    var currentUserId: String = "guest"
        private set

    // Estado de sessão (memória) - compat
    var currentStreak: Int = 0
    var totalErrors: Int = 0

    // =====================================================================
    // PREFS
    // =====================================================================

    private fun normalPrefs(context: Context): SharedPreferences {
        val appCtx = context.applicationContext
        val p = prefs
        if (p != null) return p
        val created = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = created
        contextGlobal = appCtx
        return created
    }

    private fun securePrefs(context: Context): SharedPreferences =
        SecurePrefs.get(context.applicationContext)

    fun init(context: Context) {
        val appCtx = context.applicationContext
        if (prefs == null) {
            prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            contextGlobal = appCtx
        }

        // restaura usuário ativo (se existir)
        val saved = prefs?.getString(KEY_ACTIVE_USER_ID, null)
        currentUserId = sanitizeUserId(saved)

        // ✅ migra dados antigos p/ seguro (1x por usuário)
        migrateUserDataToSecureIfNeeded(appCtx)

        Log.d(TAG, "✅ Inicializado. activeUser=$currentUserId")
    }
    // ========================================
// 🐾 PET: map selected_pet -> drawable res
// ========================================

    /**
     * Retorna o drawable do pet selecionado (0 = não mostrar).
     * Usa o nível do pet (1..3) para escolher pet_xxx_1/2/3.
     */
    fun getSelectedPetResId(context: Context): Int {
        val petId = getSelectedPet(context)
        return when (petId) {
            "pet_none" -> 0

            // ✅ Ajuste os IDs exatamente iguais aos do seu catalog (PremiumItem.id)
            "pet_owl" -> R.drawable.pet_owl_1
            "pet_owl_1" -> R.drawable.pet_owl_1
            "pet_owl_2" -> R.drawable.pet_owl_2
            "pet_owl_3" -> R.drawable.pet_owl_3

        //    "pet_cat" -> R.drawable.pet_cat_1
          //  "pet_cat_1" -> R.drawable.pet_cat_1
           // "pet_cat_2" -> R.drawable.pet_cat_2
            //"pet_cat_3" -> R.drawable.pet_cat_3

            else -> 0
        }
    }
    private fun sanitizeUserId(raw: String?): String {
        val id = raw?.trim().takeUnless { it.isNullOrBlank() } ?: "guest"
        return id.replace("\\W+".toRegex(), "_")
    }

    private fun getUserKey(key: String): String {
        val id = sanitizeUserId(currentUserId)
        return "${id}_$key"
    }

    // =====================================================================
    // Sessão / Flags globais (NORMAL PREFS)
    // =====================================================================

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
        normalPrefs(context).getString(KEY_ULTIMO_NIVEL_NORMAL, null)

    fun setUltimoNivelNormal(context: Context, level: String) {
        normalPrefs(context).edit().putString(KEY_ULTIMO_NIVEL_NORMAL, level).apply()
    }

    fun clearUltimoNivelNormal(context: Context) {
        normalPrefs(context).edit().remove(KEY_ULTIMO_NIVEL_NORMAL).apply()
    }

    /** Troca usuário ativo (use no login/logout). */
    fun setActiveUserId(context: Context, uid: String?) {
        val appCtx = context.applicationContext
        init(appCtx)

        val finalId = uid?.trim().takeUnless { it.isNullOrBlank() } ?: "guest"
        currentUserId = sanitizeUserId(finalId)

        normalPrefs(appCtx).edit().putString(KEY_ACTIVE_USER_ID, finalId).apply()

        // ✅ garante migração também ao trocar de usuário
        migrateUserDataToSecureIfNeeded(appCtx)

        Log.d(TAG, "👤 activeUserId = $currentUserId")
    }

    // =====================================================================
    // Canonicalização de nível (chave estável)
    // =====================================================================

    fun canonicalLevel(level: String): String {
        val t = level.trim().lowercase(Locale.getDefault())
        return when (t) {
            "iniciante" -> Levels.INICIANTE
            "intermediario", "intermediário" -> Levels.INTERMEDIARIO
            "avancado", "avançado" -> Levels.AVANCADO
            "experiente" -> Levels.EXPERIENTE
            else -> level.trim()
        }
    }

    private fun sanitizeLevelKey(level: String): String =
        canonicalLevel(level).trim().replace("\\W+".toRegex(), "_")

    // =====================================================================
    // 👤 Dados do Usuário (por UID) - SECURE PREFS
    // =====================================================================

    fun saveUserData(context: Context, username: String?, photoUrl: String?, avatarId: Int?) {
        init(context)

        val sp = securePrefs(context)
        sp.edit().apply {
            putString(getUserKey(KEY_USER_NAME), username)

            if (!photoUrl.isNullOrBlank()) {
                putString(getUserKey(KEY_USER_PHOTO), photoUrl)
            }

            val preferAvatar = isPreferAvatar(context)
            if (preferAvatar && avatarId != null && avatarId > 0) {
                putInt(getUserKey(KEY_USER_AVATAR), avatarId)
            } else {
                remove(getUserKey(KEY_USER_AVATAR))
            }

            apply()
        }

        Log.d(TAG, "💾 saveUserData OK (uid=$currentUserId) [SECURE]")
    }

    fun loadUserData(context: Context): Triple<String?, String?, Int?> {
        init(context)

        val sp = securePrefs(context)

        val username = sp.getString(getUserKey(KEY_USER_NAME), null)
        val photoUrl = sp.getString(getUserKey(KEY_USER_PHOTO), null)

        val preferAvatar = isPreferAvatar(context)
        val avatarId = if (preferAvatar) {
            sp.getInt(getUserKey(KEY_USER_AVATAR), 0).takeIf { it > 0 }
        } else null

        return Triple(username, photoUrl, avatarId)
    }

    fun clearUserAvatar(context: Context) {
        init(context)
        securePrefs(context).edit().remove(getUserKey(KEY_USER_AVATAR)).apply()
    }

    // =====================================================================
    // 🔓 Níveis desbloqueáveis (SECURE)
    // =====================================================================

    fun unlockLevel(context: Context, level: String) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_UNLOCKED_LEVELS)
        val set = (sp.getStringSet(key, mutableSetOf()) ?: mutableSetOf()).toMutableSet()
        if (set.add(canonicalLevel(level))) {
            sp.edit().putStringSet(key, set).apply()
            Log.d(TAG, "🔓 Nível desbloqueado: $level (uid=$currentUserId) [SECURE]")
        }
    }

    fun isLevelUnlocked(context: Context, level: String): Boolean {
        init(context)

        val lv = canonicalLevel(level)
        if (lv == Levels.INICIANTE) return true

        val key = getUserKey(KEY_UNLOCKED_LEVELS)
        val set = securePrefs(context).getStringSet(key, setOf(Levels.INICIANTE)) ?: setOf(Levels.INICIANTE)
        return set.contains(lv)
    }

    fun getUnlockedLevels(context: Context): Set<String> {
        init(context)

        val key = getUserKey(KEY_UNLOCKED_LEVELS)
        return securePrefs(context).getStringSet(key, setOf(Levels.INICIANTE)) ?: setOf(Levels.INICIANTE)
    }

    // =====================================================================
    // 🏆 Recorde (Highest Streak) (SECURE)
    // =====================================================================

    fun getHighestStreak(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_HIGHEST_STREAK), 0)
    }

    fun updateHighestStreakIfNeeded(context: Context, newStreak: Int) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_HIGHEST_STREAK)
        val current = sp.getInt(key, 0)
        if (newStreak > current) sp.edit().putInt(key, newStreak).apply()
    }

    // =====================================================================
    // 💰 Moedas e XP (SECURE)
    // =====================================================================

    fun addCoins(context: Context, amount: Int) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_COINS)
        val total = sp.getInt(key, 0) + amount
        sp.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getCoins(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_COINS), 0)
    }

    fun addXP(context: Context, amount: Int) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_XP)
        val total = sp.getInt(key, 0) + amount
        sp.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getXP(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_XP), 0)
    }

    // =====================================================================
    // 📊 Pontuação total (SECURE)
    // =====================================================================

    fun addScoreToOverallTotal(context: Context, points: Int) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_TOTAL_SCORE)
        val total = sp.getInt(key, 0) + points
        sp.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getOverallTotalScore(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_TOTAL_SCORE), 0)
    }

    // =====================================================================
    // 🗺️ Progresso do Mapa (por usuário) (SECURE)
    // =====================================================================

    fun incrementTotalCorrectGlobal(context: Context, delta: Int = 1) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_TOTAL_CORRECT_GLOBAL)
        val total = (sp.getInt(key, 0) + delta).coerceAtLeast(0)
        sp.edit().putInt(key, total).apply()
    }

    fun getTotalCorrectGlobal(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_TOTAL_CORRECT_GLOBAL), 0)
    }

    fun getMapStep(context: Context, stepSize: Int = 10): Int {
        if (stepSize <= 0) return 0
        return getTotalCorrectGlobal(context) / stepSize
    }

    fun resetMapProgress(context: Context) {
        init(context)
        securePrefs(context).edit().remove(getUserKey(KEY_TOTAL_CORRECT_GLOBAL)).apply()
    }

    // =====================================================================
    // 💾 Progresso por nível (SECURE)
    // =====================================================================

    fun saveLastQuestionIndex(context: Context, levelName: String, index: Int) {
        init(context)

        val baseKey = "${sanitizeLevelKey(levelName)}_$KEY_LAST_QUESTION_INDEX"
        securePrefs(context).edit().putInt(getUserKey(baseKey), index.coerceAtLeast(0)).apply()
    }

    fun loadLastQuestionIndex(context: Context, levelName: String): Int {
        init(context)

        val baseKey = "${sanitizeLevelKey(levelName)}_$KEY_LAST_QUESTION_INDEX"
        return securePrefs(context).getInt(getUserKey(baseKey), 0)
    }

    fun clearLastQuestionIndex(context: Context, levelName: String) {
        init(context)

        val baseKey = "${sanitizeLevelKey(levelName)}_$KEY_LAST_QUESTION_INDEX"
        securePrefs(context).edit().remove(getUserKey(baseKey)).apply()
    }

    // =====================================================================
    // 🎨 Avatares desbloqueados (por usuário) (SECURE)
    // =====================================================================

    fun unlockAvatar(context: Context, avatarId: Int) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_UNLOCKED_AVATARS)
        val set = (sp.getStringSet(key, mutableSetOf()) ?: mutableSetOf()).toMutableSet()
        if (set.add(avatarId.toString())) {
            sp.edit().putStringSet(key, set).apply()
        }
    }

    fun isAvatarUnlocked(context: Context, avatarId: Int): Boolean {
        init(context)

        if (avatarId == R.drawable.avatar1) return true

        val sp = securePrefs(context)
        val key = getUserKey(KEY_UNLOCKED_AVATARS)
        val set = sp.getStringSet(key, emptySet()) ?: emptySet()
        return set.contains(avatarId.toString())
    }

    // =====================================================================
    // ✅ Prefer Avatar (por usuário) (SECURE)
    // =====================================================================

    fun setPreferAvatar(context: Context, prefer: Boolean) {
        init(context)
        securePrefs(context).edit().putBoolean(getUserKey(KEY_PREFER_AVATAR), prefer).apply()
    }

    fun isPreferAvatar(context: Context): Boolean {
        init(context)
        return securePrefs(context).getBoolean(getUserKey(KEY_PREFER_AVATAR), false)
    }

    // =====================================================================
    // 📅 Daily Challenge (por usuário) (SECURE)
    // =====================================================================

    private fun todayKey(): String = LocalDate.now().toString() // ex: 2026-01-02

    fun isDailyDone(context: Context): Boolean {
        init(context)
        val sp = securePrefs(context)
        val last = sp.getString(getUserKey(KEY_DAILY_DONE_DATE), null)
        return last == todayKey()
    }

    fun getDailyStreak(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_DAILY_STREAK), 0)
    }

    fun markDailyDone(context: Context) {
        init(context)

        val sp = securePrefs(context)
        val today = todayKey()

        val last = sp.getString(getUserKey(KEY_DAILY_DONE_DATE), null)
        val currentStreak = sp.getInt(getUserKey(KEY_DAILY_STREAK), 0)

        val yesterday = LocalDate.now().minusDays(1).toString()

        val newStreak = when {
            last == today -> currentStreak
            last == yesterday -> currentStreak + 1
            else -> 1
        }

        sp.edit()
            .putString(getUserKey(KEY_DAILY_DONE_DATE), today)
            .putInt(getUserKey(KEY_DAILY_STREAK), newStreak)
            .apply()
    }

    fun saveDailyResult(context: Context, correct: Int, score: Int, xpEarned: Int) {
        init(context)

        val sp = securePrefs(context)
        sp.edit()
            .putString(getUserKey(KEY_DAILY_RESULT_DATE), todayKey())
            .putInt(getUserKey(KEY_DAILY_CORRECT), correct.coerceAtLeast(0))
            .putInt(getUserKey(KEY_DAILY_SCORE), score.coerceAtLeast(0))
            .putInt(getUserKey(KEY_DAILY_XP_EARNED), xpEarned.coerceAtLeast(0))
            .apply()
    }

    fun getDailyLastResult(context: Context): Triple<Int, Int, Int> {
        init(context)

        val sp = securePrefs(context)
        val correct = sp.getInt(getUserKey(KEY_DAILY_CORRECT), 0)
        val score = sp.getInt(getUserKey(KEY_DAILY_SCORE), 0)
        val xp = sp.getInt(getUserKey(KEY_DAILY_XP_EARNED), 0)
        return Triple(correct, score, xp)
    }

    fun addTotalCorrect(context: Context, amount: Int = 1) {
        init(context)

        val sp = securePrefs(context)
        val key = getUserKey(KEY_TOTAL_CORRECT)
        val total = sp.getInt(key, 0) + amount
        sp.edit().putInt(key, total.coerceAtLeast(0)).apply()
    }

    fun getTotalCorrect(context: Context): Int {
        init(context)
        return securePrefs(context).getInt(getUserKey(KEY_TOTAL_CORRECT), 0)
    }

    /**
     * ✅ Gera/recupera as 3 perguntas do dia (mesmas o dia todo)
     * - estável por DIA + USUÁRIO
     * - salva pelo questionText
     */
    fun getDailyQuestions(
        context: Context,
        questionManager: com.desafiolgico.model.QuestionManager,
        unlockedLevels: Set<String>
    ): List<com.desafiolgico.model.Question> {
        init(context)

        val sp = securePrefs(context)
        val today = todayKey()

        val pool = unlockedLevels
            .flatMap { lvl -> questionManager.getQuestionsByLevel(lvl) }
            .distinctBy { it.questionText.trim() }

        if (pool.size < 3) return pool

        val savedDate = sp.getString(getUserKey(KEY_DAILY_Q_DATE), null)
        if (savedDate == today) {
            val t1 = sp.getString(getUserKey(KEY_DAILY_Q1), null)
            val t2 = sp.getString(getUserKey(KEY_DAILY_Q2), null)
            val t3 = sp.getString(getUserKey(KEY_DAILY_Q3), null)

            val restored = listOfNotNull(
                pool.firstOrNull { it.questionText == t1 },
                pool.firstOrNull { it.questionText == t2 },
                pool.firstOrNull { it.questionText == t3 }
            )
            if (restored.size == 3) return restored
        }

        val seed = today.hashCode() + (currentUserId.hashCode() * 31)
        val rnd = kotlin.random.Random(seed)
        val selected = pool.shuffled(rnd).take(3)

        sp.edit()
            .putString(getUserKey(KEY_DAILY_Q_DATE), today)
            .putString(getUserKey(KEY_DAILY_Q1), selected[0].questionText)
            .putString(getUserKey(KEY_DAILY_Q2), selected[1].questionText)
            .putString(getUserKey(KEY_DAILY_Q3), selected[2].questionText)
            .apply()

        return selected
    }

    // =====================================================================
    // 🗺️ MAPA: Acertos por nível (por usuário) (SECURE)
    // =====================================================================

    private fun correctKeyForLevel(level: String): String =
        getUserKey(KEY_CORRECT_BY_LEVEL + sanitizeLevelKey(level))

    fun getCorrectForLevel(context: Context, level: String): Int {
        init(context)
        return securePrefs(context).getInt(correctKeyForLevel(level), 0)
    }

    fun setCorrectForLevel(context: Context, level: String, value: Int) {
        init(context)
        securePrefs(context).edit().putInt(correctKeyForLevel(level), value.coerceAtLeast(0)).apply()
    }

    fun addCorrectForLevel(context: Context, level: String, delta: Int = 1) {
        init(context)
        if (delta == 0) return
        val sp = securePrefs(context)
        val key = correctKeyForLevel(level)
        val updated = (sp.getInt(key, 0) + delta).coerceAtLeast(0)
        sp.edit().putInt(key, updated).apply()
    }

    fun resetCorrectForLevel(context: Context, level: String) {
        init(context)
        securePrefs(context).edit().remove(correctKeyForLevel(level)).apply()
    }

    fun resetCorrectForAllLevels(context: Context) {
        resetCorrectForLevel(context, Levels.INICIANTE)
        resetCorrectForLevel(context, Levels.INTERMEDIARIO)
        resetCorrectForLevel(context, Levels.AVANCADO)
        resetCorrectForLevel(context, Levels.EXPERIENTE)
    }

    fun getTotalCorrectAllLevels(context: Context): Int {
        init(context)
        return listOf(Levels.INICIANTE, Levels.INTERMEDIARIO, Levels.AVANCADO, Levels.EXPERIENTE)
            .sumOf { getCorrectForLevel(context, it) }
    }

    // =====================================================================
    // 🔁 Anti-farm (seen/scored) - helpers
    // =====================================================================

    private fun userScopeKey(): String = sanitizeUserId(currentUserId).ifBlank { "guest" }
    private fun levelScopeKey(level: String): String = canonicalLevel(level).trim().lowercase(Locale.getDefault())

    private fun seenStorageKey(level: String): String =
        "${KEY_SEEN_PREFIX}${userScopeKey()}_${levelScopeKey(level)}"

    private fun scoredStorageKey(level: String): String =
        "${KEY_SCORED_PREFIX}${userScopeKey()}_${levelScopeKey(level)}"

    fun getSeenSet(context: Context, level: String): MutableSet<String> {
        init(context)
        val raw = SecurePrefs.getStringSet(context, seenStorageKey(level), emptySet())
        return HashSet(raw)
    }

    fun markSeen(context: Context, level: String, qKey: String) {
        init(context)
        if (qKey.isBlank()) return
        val set = getSeenSet(context, level)
        if (set.add(qKey)) SecurePrefs.putStringSet(context, seenStorageKey(level), set)
    }

    fun getScoredSet(context: Context, level: String): MutableSet<String> {
        init(context)
        val raw = SecurePrefs.getStringSet(context, scoredStorageKey(level), emptySet())
        return HashSet(raw)
    }

    /**
     * Marca como "pontuada" apenas 1 vez na vida.
     * true = primeira vez
     * false = já pontuada antes
     */
    fun markScoredIfFirstTime(context: Context, level: String, qKey: String): Boolean {
        init(context)
        if (qKey.isBlank()) return false
        val set = getScoredSet(context, level)
        val first = set.add(qKey)
        if (first) SecurePrefs.putStringSet(context, scoredStorageKey(level), set)
        return first
    }

    // =====================================================================
    // 🧹 Logout / Reset
    // =====================================================================

    fun clearSession(context: Context) {
        setActiveUserId(context, "guest")
        Log.d(TAG, "🧹 Sessão limpa. activeUser=guest")
    }

    /**
     * Apaga SOMENTE os dados locais do usuário atual (UID atual).
     * Não apaga o app todo e não mexe em outros usuários.
     */
    fun resetUserLocalData(context: Context) {
        init(context)

        val sp = securePrefs(context)
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
            "${id}_$KEY_TOTAL_CORRECT",

            // Daily
            "${id}_$KEY_DAILY_DONE_DATE",
            "${id}_$KEY_DAILY_STREAK",
            "${id}_$KEY_DAILY_RESULT_DATE",
            "${id}_$KEY_DAILY_CORRECT",
            "${id}_$KEY_DAILY_SCORE",
            "${id}_$KEY_DAILY_XP_EARNED",
            "${id}_$KEY_DAILY_Q_DATE",
            "${id}_$KEY_DAILY_Q1",
            "${id}_$KEY_DAILY_Q2",
            "${id}_$KEY_DAILY_Q3",

            // Cosmetics
            "${id}_$KEY_SELECTED_THEME",
            "${id}_$KEY_SELECTED_FRAME",
            "${id}_$KEY_SELECTED_TITLE",
            "${id}_$KEY_SELECTED_PET",
            "${id}_$KEY_SELECTED_VFX",
            "${id}_$KEY_UNLOCKED_THEMES",
            "${id}_$KEY_UNLOCKED_FRAMES",
            "${id}_$KEY_UNLOCKED_TITLES",
            "${id}_$KEY_UNLOCKED_PETS",
            "${id}_$KEY_UNLOCKED_VFX",
        )

        sp.edit().apply {
            keysToRemove.forEach { remove(it) }
            apply()
        }

        currentStreak = 0
        totalErrors = 0
        resetCorrectForAllLevels(context)
    }

    /** Compat: resetAll(context) reseta o usuário atual */
    fun resetAll(context: Context) {
        resetUserLocalData(context)
    }

    // =====================================================================
    // 💎 Cosmetics (SECURE)
    // =====================================================================

    private fun getUserStringSet(context: Context, key: String, def: Set<String> = emptySet()): MutableSet<String> {
        init(context)
        val sp = securePrefs(context)
        val set = sp.getStringSet(getUserKey(key), def) ?: def
        return set.toMutableSet()
    }

    private fun putUserStringSet(context: Context, key: String, value: Set<String>) {
        init(context)
        securePrefs(context).edit().putStringSet(getUserKey(key), value).apply()
    }

    fun trySpendCoins(context: Context, amount: Int): Boolean {
        init(context)
        if (amount <= 0) return true
        val current = getCoins(context)
        if (current < amount) return false
        addCoins(context, -amount)
        return true
    }

    private fun unlockString(context: Context, setKey: String, id: String) {
        val s = getUserStringSet(context, setKey)
        if (s.add(id)) putUserStringSet(context, setKey, s)
    }

    private fun isStringUnlocked(context: Context, setKey: String, id: String): Boolean {
        init(context)
        val s = securePrefs(context).getStringSet(getUserKey(setKey), emptySet()) ?: emptySet()
        return s.contains(id)
    }

    // Themes
    fun unlockTheme(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_THEMES, id)
    fun isThemeUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_THEMES, id)
    fun setSelectedTheme(context: Context, id: String) =
        securePrefs(context).edit().putString(getUserKey(KEY_SELECTED_THEME), id).apply()
    fun getSelectedTheme(context: Context): String =
        securePrefs(context).getString(getUserKey(KEY_SELECTED_THEME), "theme_default") ?: "theme_default"

    // Frames
    fun unlockFrame(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_FRAMES, id)
    fun isFrameUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_FRAMES, id)
    fun setSelectedFrame(context: Context, id: String) =
        securePrefs(context).edit().putString(getUserKey(KEY_SELECTED_FRAME), id).apply()
    fun getSelectedFrame(context: Context): String =
        securePrefs(context).getString(getUserKey(KEY_SELECTED_FRAME), "frame_none") ?: "frame_none"

    // Titles
    fun unlockTitle(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_TITLES, id)
    fun isTitleUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_TITLES, id)
    fun setSelectedTitle(context: Context, id: String) =
        securePrefs(context).edit().putString(getUserKey(KEY_SELECTED_TITLE), id).apply()
    fun getSelectedTitle(context: Context): String =
        securePrefs(context).getString(getUserKey(KEY_SELECTED_TITLE), "title_none") ?: "title_none"

    // Pets
    fun unlockPet(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_PETS, id)
    fun isPetUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_PETS, id)
    fun setSelectedPet(context: Context, id: String) =
        securePrefs(context).edit().putString(getUserKey(KEY_SELECTED_PET), id).apply()
    fun getSelectedPet(context: Context): String =
        securePrefs(context).getString(getUserKey(KEY_SELECTED_PET), "pet_none") ?: "pet_none"

    fun getPetLevel(context: Context, petId: String): Int {
        init(context)
        val k = getUserKey(KEY_PET_LEVEL_PREFIX + petId)
        return securePrefs(context).getInt(k, 1).coerceIn(1, 3)
    }

    fun setPetLevel(context: Context, petId: String, lvl: Int) {
        init(context)
        val k = getUserKey(KEY_PET_LEVEL_PREFIX + petId)
        securePrefs(context).edit().putInt(k, lvl.coerceIn(1, 3)).apply()
    }

    // VFX
    fun unlockVfx(context: Context, id: String) = unlockString(context, KEY_UNLOCKED_VFX, id)
    fun isVfxUnlocked(context: Context, id: String) = isStringUnlocked(context, KEY_UNLOCKED_VFX, id)
    fun setSelectedVfx(context: Context, id: String) =
        securePrefs(context).edit().putString(getUserKey(KEY_SELECTED_VFX), id).apply()
    fun getSelectedVfx(context: Context): String =
        securePrefs(context).getString(getUserKey(KEY_SELECTED_VFX), "vfx_basic") ?: "vfx_basic"

    // =====================================================================
    // MIGRAÇÃO: prefs antigo -> secure (1x por usuário)
    // =====================================================================

    private fun migrateUserDataToSecureIfNeeded(context: Context) {
        val normal = normalPrefs(context)
        val secure = securePrefs(context)

        val markerKey = getUserKey(KEY_SECURE_MIGRATION_DONE)
        if (secure.getBoolean(markerKey, false)) return

        // Base keys (serão prefixadas com userKey)
        val stringKeys = listOf(
            KEY_USER_NAME,
            KEY_USER_PHOTO,
            KEY_DAILY_DONE_DATE,
            KEY_DAILY_RESULT_DATE,
            KEY_DAILY_Q_DATE,
            KEY_DAILY_Q1,
            KEY_DAILY_Q2,
            KEY_DAILY_Q3,
            KEY_SELECTED_THEME,
            KEY_SELECTED_FRAME,
            KEY_SELECTED_TITLE,
            KEY_SELECTED_PET,
            KEY_SELECTED_VFX
        )

        val intKeys = listOf(
            KEY_USER_AVATAR,
            KEY_TOTAL_SCORE,
            KEY_COINS,
            KEY_XP,
            KEY_HIGHEST_STREAK,
            KEY_TOTAL_CORRECT_GLOBAL,
            KEY_DAILY_STREAK,
            KEY_DAILY_CORRECT,
            KEY_DAILY_SCORE,
            KEY_DAILY_XP_EARNED,
            KEY_TOTAL_CORRECT
        )

        val setKeys = listOf(
            KEY_UNLOCKED_LEVELS,
            KEY_UNLOCKED_AVATARS,
            KEY_UNLOCKED_THEMES,
            KEY_UNLOCKED_FRAMES,
            KEY_UNLOCKED_TITLES,
            KEY_UNLOCKED_PETS,
            KEY_UNLOCKED_VFX
        )

        // Migra (tenta: prefixado e, se existir legado sem prefixo, também)
        for (k in stringKeys) {
            val full = getUserKey(k)
            val v = normal.getString(full, null) ?: normal.getString(k, null)
            if (v != null) secure.edit().putString(full, v).apply()
        }

        for (k in intKeys) {
            val full = getUserKey(k)
            if (normal.contains(full) || normal.contains(k)) {
                runCatching {
                    val v = if (normal.contains(full)) normal.getInt(full, Int.MIN_VALUE) else normal.getInt(k, Int.MIN_VALUE)
                    if (v != Int.MIN_VALUE) secure.edit().putInt(full, v).apply()
                }
            }
        }

        for (k in setKeys) {
            val full = getUserKey(k)
            val v = normal.getStringSet(full, null) ?: normal.getStringSet(k, null)
            if (v != null) secure.edit().putStringSet(full, HashSet(v)).apply()
        }

        // Migra chaves dinâmicas do usuário atual: correct_by_level_*, last_q_index_*, pet_level_*
        val userPrefix = sanitizeUserId(currentUserId) + "_"
        val allKeys = normal.all.keys

        val dynamicPrefixes = listOf(
            userPrefix + KEY_CORRECT_BY_LEVEL,
            userPrefix + KEY_PET_LEVEL_PREFIX,
            userPrefix + KEY_SEEN_PREFIX,
            userPrefix + KEY_SCORED_PREFIX
        )

        for (key in allKeys) {
            if (!key.startsWith(userPrefix)) continue

            val isDynamic =
                key.contains(KEY_LAST_QUESTION_INDEX) || dynamicPrefixes.any { key.startsWith(it) }

            if (!isDynamic) continue

            val value = normal.all[key]
            when (value) {
                is Int -> secure.edit().putInt(key, value).apply()
                is Long -> secure.edit().putLong(key, value).apply()
                is String -> secure.edit().putString(key, value).apply()
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    secure.edit().putStringSet(key, HashSet(value as Set<String>)).apply()
                }
            }
        }

        // ✅ limpa do prefs normal (somente do usuário atual)
        normal.edit().apply {
            for (key in allKeys) {
                if (key.startsWith(userPrefix)) remove(key)
            }
            apply()
        }

        secure.edit().putBoolean(markerKey, true).apply()
        Log.d(TAG, "🔐 Migração para SecurePrefs concluída (uid=$currentUserId)")
    }

    // =====================================================================
    // Constantes públicas (nomes de níveis)
    // =====================================================================

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
