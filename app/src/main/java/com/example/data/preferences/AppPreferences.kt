package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notely_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(prefs.getString(KEY_ACCENT_COLOR, "indigo") ?: "indigo")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val _fontScale = MutableStateFlow(prefs.getFloat(KEY_FONT_SCALE, 1.0f))
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _defaultNoteColor = MutableStateFlow(prefs.getString(KEY_DEFAULT_NOTE_COLOR, "default") ?: "default")
    val defaultNoteColor: StateFlow<String> = _defaultNoteColor.asStateFlow()

    private val _viewMode = MutableStateFlow(prefs.getString(KEY_VIEW_MODE, "grid") ?: "grid") // "grid" or "list"
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_IS_PIN_ENABLED, false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_IS_BIOMETRIC_ENABLED, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _pinCode = MutableStateFlow(prefs.getString(KEY_PIN_CODE, "") ?: "")
    val pinCode: StateFlow<String> = _pinCode.asStateFlow()

    private val _autoBackupReminder = MutableStateFlow(prefs.getBoolean(KEY_AUTO_BACKUP_REMINDER, true))
    val autoBackupReminder: StateFlow<Boolean> = _autoBackupReminder.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_TIME, 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(color: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color).apply()
        _accentColor.value = color
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
        _fontScale.value = scale
    }

    fun setDefaultNoteColor(color: String) {
        prefs.edit().putString(KEY_DEFAULT_NOTE_COLOR, color).apply()
        _defaultNoteColor.value = color
    }

    fun setViewMode(mode: String) {
        prefs.edit().putString(KEY_VIEW_MODE, mode).apply()
        _viewMode.value = mode
    }

    fun setPin(pin: String) {
        prefs.edit()
            .putString(KEY_PIN_CODE, pin)
            .putBoolean(KEY_IS_PIN_ENABLED, pin.isNotEmpty())
            .apply()
        _pinCode.value = pin
        _isPinEnabled.value = pin.isNotEmpty()
    }

    fun disablePin() {
        prefs.edit()
            .putBoolean(KEY_IS_PIN_ENABLED, false)
            .putString(KEY_PIN_CODE, "")
            .apply()
        _isPinEnabled.value = false
        _pinCode.value = ""
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setAutoBackupReminder(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_REMINDER, enabled).apply()
        _autoBackupReminder.value = enabled
    }

    fun setLastBackupTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()
        _lastBackupTime.value = timestamp
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_DEFAULT_NOTE_COLOR = "default_note_color"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_IS_PIN_ENABLED = "is_pin_enabled"
        private const val KEY_IS_BIOMETRIC_ENABLED = "is_biometric_enabled"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_AUTO_BACKUP_REMINDER = "auto_backup_reminder"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_LANGUAGE = "app_language"
    }
}
