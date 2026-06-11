package com.wergogo.travelnote.util

import com.wergogo.travelnote.R

object DefaultImages {
    val avatars: Map<String, Int> = mapOf(
        "avatar_01" to R.drawable.avatar_01,
        "avatar_02" to R.drawable.avatar_02,
        "avatar_03" to R.drawable.avatar_03,
        "avatar_04" to R.drawable.avatar_04,
        "avatar_05" to R.drawable.avatar_05,
        "avatar_06" to R.drawable.avatar_06,
        "avatar_07" to R.drawable.avatar_07,
        "avatar_08" to R.drawable.avatar_08,
        "avatar_09" to R.drawable.avatar_09,
        "avatar_10" to R.drawable.avatar_10
    )

    val covers: Map<String, Int> = mapOf(
        "cover_beach"     to R.drawable.cover_beach,
        "cover_mountains" to R.drawable.cover_mountains,
        "cover_city"      to R.drawable.cover_city,
        "cover_forest"    to R.drawable.cover_forest,
        "cover_desert"    to R.drawable.cover_desert,
        "cover_snow"      to R.drawable.cover_snow
    )

    fun avatar(id: String?): Int = avatars[id] ?: R.drawable.avatar_fallback

    fun cover(id: String?): Int = covers[id] ?: R.drawable.cover_default

    /** All avatar keys for display in AvatarPicker. */
    val avatarKeys: List<String> = avatars.keys.toList()

    /** All cover keys for display in CoverPicker. */
    val coverKeys: List<String> = covers.keys.toList()
}
