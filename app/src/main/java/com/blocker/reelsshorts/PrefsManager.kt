package com.blocker.reelsshorts

import android.content.Context
import android.content.SharedPreferences

/**
 * Centraliza leitura/escrita das preferências do app:
 * - se o bloqueio de Reels/Shorts está ligado
 * - contadores de quantas vezes cada um foi bloqueado
 */
object PrefsManager {

    private const val PREFS_NAME = "reels_shorts_blocker_prefs"
    private const val KEY_BLOCK_INSTAGRAM = "block_instagram_reels"
    private const val KEY_BLOCK_YOUTUBE = "block_youtube_shorts"
    private const val KEY_COUNT_REELS = "count_reels_blocked"
    private const val KEY_COUNT_SHORTS = "count_shorts_blocked"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isInstagramBlockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_INSTAGRAM, true)

    fun setInstagramBlockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK_INSTAGRAM, enabled).apply()
    }

    fun isYoutubeBlockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_YOUTUBE, true)

    fun setYoutubeBlockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK_YOUTUBE, enabled).apply()
    }

    fun getReelsBlockedCount(context: Context): Int =
        prefs(context).getInt(KEY_COUNT_REELS, 0)

    fun incrementReelsBlockedCount(context: Context) {
        val current = getReelsBlockedCount(context)
        prefs(context).edit().putInt(KEY_COUNT_REELS, current + 1).apply()
    }

    fun getShortsBlockedCount(context: Context): Int =
        prefs(context).getInt(KEY_COUNT_SHORTS, 0)

    fun incrementShortsBlockedCount(context: Context) {
        val current = getShortsBlockedCount(context)
        prefs(context).edit().putInt(KEY_COUNT_SHORTS, current + 1).apply()
    }

    fun resetStats(context: Context) {
        prefs(context).edit()
            .putInt(KEY_COUNT_REELS, 0)
            .putInt(KEY_COUNT_SHORTS, 0)
            .apply()
    }
}
