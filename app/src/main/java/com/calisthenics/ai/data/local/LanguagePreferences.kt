package com.calisthenics.ai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.calisthenics.ai.ui.locale.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.languageDataStore by preferencesDataStore(name = "language_preferences")

@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    val language: Flow<AppLanguage> = context.languageDataStore.data.map { preferences ->
        AppLanguage.fromTag(preferences[LANGUAGE_KEY]) ?: AppLanguage.ENGLISH
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.languageTag
        }
    }
}
