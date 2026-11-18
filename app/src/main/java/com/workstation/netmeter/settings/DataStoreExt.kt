package com.workstation.netmeter.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore instance per process for user preferences. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Separate store for usage counters, so clearing preferences never loses history. */
val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(name = "usage")

/** Shared key so [LocaleManager] can read the language without a repository round-trip. */
val LANGUAGE_KEY = stringPreferencesKey("language")
