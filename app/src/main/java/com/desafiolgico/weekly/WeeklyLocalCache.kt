package com.desafiolgico.weekly

import android.content.Context

object WeeklyLocalCache {

    private const val PREF = "weekly_cache"
    private const val KEY_WEEK_ID = "week_id"
    private const val KEY_SUMMARY = "summary_text"

    fun save(ctx: Context, weekId: String, summary: String) {
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WEEK_ID, weekId)
            .putString(KEY_SUMMARY, summary)
            .apply()
    }

    fun getWeekId(ctx: Context): String =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_WEEK_ID, "") ?: ""

    fun getSummary(ctx: Context): String =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SUMMARY, "") ?: ""
}
