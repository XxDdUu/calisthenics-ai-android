package com.calisthenics.ai.ui.locale

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import com.calisthenics.ai.data.local.LanguagePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the application language independently from authentication and theme state.
 *
 * The Compose host supplies [localizedContext] as the current context. This means all
 * Android resource lookups (including stringResource) immediately use the selected
 * locale without recreating the activity or clearing the navigation/session state.
 */
@Singleton
class LanguageManager @Inject constructor(
    private val preferences: LanguagePreferences,
    @ApplicationContext private val applicationContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    init {
        scope.launch {
            preferences.language.collect { selected ->
                _language.value = selected
                applyDefaultLocale(selected)
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        // Update the observable state first so Compose switches immediately; persistence
        // happens in the same scope and survives logout, offline use, and process restart.
        _language.value = language
        applyDefaultLocale(language)
        scope.launch { preferences.setLanguage(language) }
    }

    fun localizedContext(
        baseContext: Context,
        language: AppLanguage = _language.value
    ): Context {
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(language.locale)
        configuration.setLayoutDirection(language.locale)
        val configContext = baseContext.createConfigurationContext(configuration)
        return object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = configContext.resources
            override fun getAssets(): AssetManager = configContext.assets
        }
    }

    private fun applyDefaultLocale(language: AppLanguage) {
        // DateTimeFormatter/number formatters in existing UI code use Locale.getDefault().
        // Keep those formatters in sync with resource lookups as well.
        Locale.setDefault(language.locale)
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(language.locale)
        configuration.setLayoutDirection(language.locale)
        applicationContext.resources.updateConfiguration(configuration, applicationContext.resources.displayMetrics)
    }
}
