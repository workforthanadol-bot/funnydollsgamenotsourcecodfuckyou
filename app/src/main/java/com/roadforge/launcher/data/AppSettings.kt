package com.roadforge.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object AppSettingsKeys {
    val THEME_DARK = booleanPreferencesKey("theme_dark")
    val CONFIRM_EXIT = booleanPreferencesKey("confirm_exit")
    val DEFAULT_GAME_ID = stringPreferencesKey("default_game_id")
    val TOUCH_OVERLAY_ENABLED = booleanPreferencesKey("touch_overlay_enabled")
    val ACTIVE_LAYOUT_ID = stringPreferencesKey("active_layout_id")
    val SHOW_FPS = booleanPreferencesKey("show_fps")
    val PERF_OVERLAY = booleanPreferencesKey("perf_overlay")
    val LAST_ACTIVE_TAB = intPreferencesKey("last_active_tab")
    val RUNTIME_BASE_DIR = stringPreferencesKey("runtime_base_dir")
    val FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
}

class AppSettingsRepository(private val context: Context) {

    // ─── Flow accessors ─────────────────────────────────────────

    val isDarkTheme: Flow<Boolean>              = context.dataStore.data.map { it[AppSettingsKeys.THEME_DARK]            ?: true }
    val darkThemeFlow: Flow<Boolean>            = isDarkTheme
    val touchOverlayEnabled: Flow<Boolean>      = context.dataStore.data.map { it[AppSettingsKeys.TOUCH_OVERLAY_ENABLED] ?: true }
    val touchOverlayEnabledFlow: Flow<Boolean>  = touchOverlayEnabled
    val showFps: Flow<Boolean>                  = context.dataStore.data.map { it[AppSettingsKeys.SHOW_FPS]              ?: false }
    val showFpsFlow: Flow<Boolean>              = showFps
    val perfOverlayFlow: Flow<Boolean>          = context.dataStore.data.map { it[AppSettingsKeys.PERF_OVERLAY]          ?: false }
    val confirmExitFlow: Flow<Boolean>          = context.dataStore.data.map { it[AppSettingsKeys.CONFIRM_EXIT]          ?: true }
    val runtimeBaseDirFlow: Flow<String>        = context.dataStore.data.map { it[AppSettingsKeys.RUNTIME_BASE_DIR]      ?: "" }
    val activeLayoutId: Flow<String>            = context.dataStore.data.map { it[AppSettingsKeys.ACTIVE_LAYOUT_ID]      ?: "default_beamng" }
    val isFirstLaunch: Flow<Boolean>            = context.dataStore.data.map { !(it[AppSettingsKeys.FIRST_LAUNCH_DONE]   ?: false) }

    // ─── Suspend setters ─────────────────────────────────────────

    suspend fun setDarkTheme(v: Boolean)           = context.dataStore.edit { it[AppSettingsKeys.THEME_DARK]            = v }
    suspend fun setTouchOverlayEnabled(v: Boolean) = context.dataStore.edit { it[AppSettingsKeys.TOUCH_OVERLAY_ENABLED] = v }
    suspend fun setShowFps(v: Boolean)             = context.dataStore.edit { it[AppSettingsKeys.SHOW_FPS]              = v }
    suspend fun setPerfOverlay(v: Boolean)         = context.dataStore.edit { it[AppSettingsKeys.PERF_OVERLAY]          = v }
    suspend fun setConfirmExit(v: Boolean)         = context.dataStore.edit { it[AppSettingsKeys.CONFIRM_EXIT]          = v }
    suspend fun setRuntimeBaseDir(v: String)       = context.dataStore.edit { it[AppSettingsKeys.RUNTIME_BASE_DIR]      = v }
    suspend fun setActiveLayoutId(id: String)      = context.dataStore.edit { it[AppSettingsKeys.ACTIVE_LAYOUT_ID]      = id }
    suspend fun setFirstLaunchDone()               = context.dataStore.edit { it[AppSettingsKeys.FIRST_LAUNCH_DONE]     = true }
}
