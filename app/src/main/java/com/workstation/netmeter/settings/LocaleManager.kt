package com.workstation.netmeter.settings

import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Applies the user-selected language to Activity contexts.
 *
 * Hinglish is stored as the `en-IN` resource configuration so that Android's
 * normal resource resolution picks `values-en-rIN/strings.xml` without needing
 * a compatibility shim.
 */
object LocaleManager {

    fun localeFor(language: AppLanguage): Locale = when (language) {
        AppLanguage.EN -> Locale("en")
        AppLanguage.HI -> Locale("hi")
        AppLanguage.HINGLISH -> Locale("en", "IN")
    }

    /** Returns a context whose resources use the given language. */
    fun wrap(base: Context, language: AppLanguage): Context {
        val locale = localeFor(language)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    /** Synchronous read used while the Activity base context is being attached. */
    fun storedLanguage(base: Context): AppLanguage = try {
        val prefs = runBlocking { base.settingsDataStore.data.first() }
        AppLanguage.fromKey(prefs[LANGUAGE_KEY])
    } catch (t: Throwable) {
        AppLanguage.EN
    }

    /** Wraps [base] with the persisted language. */
    fun wrapWithStored(base: Context): Context = wrap(base, storedLanguage(base))
}
