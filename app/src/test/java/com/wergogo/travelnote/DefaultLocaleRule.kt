package com.wergogo.travelnote

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale

/** Pins [Locale.getDefault] for the duration of a test and restores it afterwards. */
class DefaultLocaleRule(private val locale: Locale = Locale.US) : TestWatcher() {
    private var original: Locale = Locale.getDefault()

    override fun starting(description: Description) {
        original = Locale.getDefault()
        Locale.setDefault(locale)
    }

    override fun finished(description: Description) {
        Locale.setDefault(original)
    }
}
