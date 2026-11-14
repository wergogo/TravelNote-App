package com.wergogo.travelnote.util

import com.wergogo.travelnote.DefaultLocaleRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class DefaultImagesTest {
    @get:Rule val localeRule = DefaultLocaleRule()

    @Test
    fun `a new user's random avatar is always a bundled one, whatever the device language`() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))   // uses Arabic-Indic digits; restored by localeRule

        repeat(50) {
            val id = DefaultImages.randomAvatarId()
            assertTrue("unexpected avatar id \"$id\"", id in DefaultImages.avatarKeys)
        }
    }
}
