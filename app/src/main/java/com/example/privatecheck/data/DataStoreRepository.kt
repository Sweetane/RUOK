package com.example.privatecheck.data

import kotlinx.coroutines.flow.first // Add Import
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "private_check_prefs")

class DataStoreRepository(private val context: Context) {

    companion object {
        val KEY_STREAK_DAYS = intPreferencesKey("key_current_steak")
        val KEY_LAST_CHECK_IN_DATE = stringPreferencesKey("key_last_check_in_date")
        val KEY_CHECK_IN_HISTORY = stringSetPreferencesKey("key_check_in_history")
        
        // Email Settings
        val KEY_CONTACT_EMAIL = stringPreferencesKey("key_contact_email")
        val KEY_CONTACT_EMAIL_2 = stringPreferencesKey("key_contact_email_2")
        val KEY_CONTACT_EMAIL_3 = stringPreferencesKey("key_contact_email_3")
        val KEY_SENDER_EMAIL = stringPreferencesKey("key_sender_email")
        val KEY_SENDER_PASSWORD = stringPreferencesKey("key_sender_password")
        val KEY_SMTP_HOST = stringPreferencesKey("key_smtp_host")
        // UI Settings
        val KEY_IS_DARK_MODE = booleanPreferencesKey("key_is_dark_mode")
        
        // Widget Settings
        // type: "color" or "image"
        val KEY_WIDGET_BG_TYPE = stringPreferencesKey("key_widget_bg_type") 
        val KEY_WIDGET_BG_COLOR = intPreferencesKey("key_widget_bg_color") // Default: Green
        val KEY_WIDGET_BG_IMAGE_URI = stringPreferencesKey("key_widget_bg_image_uri") // This is the FINAL processed image path
        
        // Editor State (Persistence)
        val KEY_WIDGET_SOURCE_PATH = stringPreferencesKey("key_widget_source_path") // Original Copied File
        val KEY_WIDGET_SCALE = floatPreferencesKey("key_widget_scale")
        val KEY_WIDGET_ROTATION = floatPreferencesKey("key_widget_rotation")
        val KEY_WIDGET_OFFSET_X = floatPreferencesKey("key_widget_offset_x")
        val KEY_WIDGET_OFFSET_Y = floatPreferencesKey("key_widget_offset_y")
        val KEY_WIDGET_CONTRAST = floatPreferencesKey("key_widget_contrast")
        val KEY_WIDGET_WHITE_SCRIM = floatPreferencesKey("key_widget_white_scrim") // Replaces Brightness/Alpha confusion
        
        // App Theme Settings
        val KEY_APP_THEME_COLOR = intPreferencesKey("key_app_theme_color")
    }

    // Security: EncryptedPrefs for Password
    private val masterKey = androidx.security.crypto.MasterKey.Builder(context)
        .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
        context,
        "secure_private_check_prefs",
        masterKey,
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )


    
    // Legacy Key for migration
    private val KEY_SENDER_PASSWORD_LEGACY = stringPreferencesKey("key_sender_password")
    private val KEY_SECURE_PASSWORD = "secure_sender_password"

    suspend fun performSecurityMigration() {
        // Check legacy DataStore
        val preferences = context.dataStore.data.first()
        val legacyPassword = preferences[KEY_SENDER_PASSWORD_LEGACY]
        
        if (!legacyPassword.isNullOrEmpty()) {
            // 1. Move to SecurePrefs
            securePrefs.edit().putString(KEY_SECURE_PASSWORD, legacyPassword).apply()
            _senderPasswordFlow.value = legacyPassword
            
            // 2. Remove from DataStore
            context.dataStore.edit { prefs ->
                prefs.remove(KEY_SENDER_PASSWORD_LEGACY)
            }
            android.util.Log.d("Security", "Migrated legacy password to encrypted storage")
        }
    }

    val streakDays: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[KEY_STREAK_DAYS] ?: 0 }

    val lastCheckInDate: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_LAST_CHECK_IN_DATE] ?: "" }

    val contactEmail: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_CONTACT_EMAIL] ?: "" }

    val contactEmail2: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_CONTACT_EMAIL_2] ?: "" }

    val contactEmail3: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_CONTACT_EMAIL_3] ?: "" }
        
    val senderEmail: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_SENDER_EMAIL] ?: "" }

    // Password Logic: Read from SecurePrefs into a StateFlow to mimic DataStore behavior
    private val _senderPasswordFlow = kotlinx.coroutines.flow.MutableStateFlow(
        securePrefs.getString(KEY_SECURE_PASSWORD, "") ?: ""
    )
    val senderPassword: Flow<String> = _senderPasswordFlow

    val smtpHost: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_SMTP_HOST] ?: "smtp.qq.com" }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEY_IS_DARK_MODE] ?: false }

    val appThemeColor: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[KEY_APP_THEME_COLOR] ?: -7357297 } // Default Green (0xFF8FBC8F)

    val checkInHistory: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[KEY_CHECK_IN_HISTORY] ?: emptySet() }

    // Widget Flows
    val widgetBgType: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_BG_TYPE] ?: "color" }
        
    val widgetBgColor: Flow<Int> = context.dataStore.data
        // Default Green: 0xFF8FBC8F -> Int Value: -7357303
        .map { preferences -> preferences[KEY_WIDGET_BG_COLOR] ?: -7357297 } 

    val widgetBgImageUri: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_BG_IMAGE_URI] }

    // Editor State Flows
    val widgetSourcePath: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_SOURCE_PATH] }
        
    val widgetScale: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_SCALE] ?: 1f }
        
    val widgetRotation: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_ROTATION] ?: 0f }
        
    val widgetOffsetX: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_OFFSET_X] ?: 0f }
        
    val widgetOffsetY: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_OFFSET_Y] ?: 0f }
        
    val widgetContrast: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_CONTRAST] ?: 1f }
        
    val widgetWhiteScrim: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[KEY_WIDGET_WHITE_SCRIM] ?: 0f }

    suspend fun updateStreak(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STREAK_DAYS] = days
        }
    }

    suspend fun updateLastCheckInDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_CHECK_IN_DATE] = date
        }
    }

    suspend fun saveSettings(
        contact1: String, 
        contact2: String, 
        contact3: String, 
        sender: String, 
        pass: String, 
        smtp: String
    ) {
        // 1. Save sensitive data to EncryptedSharedPreferences
        securePrefs.edit().putString(KEY_SECURE_PASSWORD, pass).apply()
        _senderPasswordFlow.value = pass

        // 2. Save non-sensitive data to DataStore
        context.dataStore.edit { preferences ->
            preferences[KEY_CONTACT_EMAIL] = contact1
            preferences[KEY_CONTACT_EMAIL_2] = contact2
            preferences[KEY_CONTACT_EMAIL_3] = contact3
            preferences[KEY_SENDER_EMAIL] = sender
            preferences[KEY_SMTP_HOST] = smtp
        }
    }

    suspend fun toggleDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_MODE] = isDark
        }
    }

    suspend fun saveWidgetSettings(type: String, color: Int, imageUri: String?) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIDGET_BG_TYPE] = type
            preferences[KEY_WIDGET_BG_COLOR] = color
            if (imageUri != null) {
                preferences[KEY_WIDGET_BG_IMAGE_URI] = imageUri
                // We do NOT reset adjustments here anymore. Adjustments persist for the Source Path.
            }
        }
    }
    
    suspend fun saveWidgetSourcePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIDGET_SOURCE_PATH] = path
            // Reset adjustments only when SOURCE changes
            preferences[KEY_WIDGET_SCALE] = 1f
            preferences[KEY_WIDGET_ROTATION] = 0f
            preferences[KEY_WIDGET_OFFSET_X] = 0f
            preferences[KEY_WIDGET_OFFSET_Y] = 0f
            preferences[KEY_WIDGET_CONTRAST] = 1f
            preferences[KEY_WIDGET_WHITE_SCRIM] = 0f
        }
    }

    suspend fun saveWidgetImageAdjustments(
        scale: Float,
        rotation: Float,
        offsetX: Float,
        offsetY: Float,
        contrast: Float,
        whiteScrim: Float
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WIDGET_SCALE] = scale
            preferences[KEY_WIDGET_ROTATION] = rotation
            preferences[KEY_WIDGET_OFFSET_X] = offsetX
            preferences[KEY_WIDGET_OFFSET_Y] = offsetY
            preferences[KEY_WIDGET_CONTRAST] = contrast
            preferences[KEY_WIDGET_WHITE_SCRIM] = whiteScrim
        }
    }

    suspend fun saveAppThemeColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME_COLOR] = color
        }
    }

    suspend fun addCheckInDate(date: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_CHECK_IN_HISTORY] ?: emptySet()
            preferences[KEY_CHECK_IN_HISTORY] = current + date
        }
    }
}
