package com.calisthenics.ai.ui.locale

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageManager: LanguageManager
) : ViewModel() {
    val language = languageManager.language

    fun setLanguage(language: AppLanguage) {
        languageManager.setLanguage(language)
    }
}
