package com.roadforge.launcher.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadforge.launcher.data.AppSettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiSettings(
    val darkTheme: Boolean = true,
    val showFps: Boolean = false,
    val perfOverlay: Boolean = false,
    val touchOverlayEnabled: Boolean = true,
    val confirmExit: Boolean = true,
    val runtimeBaseDir: String = "",
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppSettingsRepository(app)

    val settings: StateFlow<UiSettings> = combine(
        repo.darkThemeFlow,
        repo.showFpsFlow,
        repo.perfOverlayFlow,
        repo.touchOverlayEnabledFlow,
        repo.confirmExitFlow,
        repo.runtimeBaseDirFlow,
    ) { arr ->
        UiSettings(
            darkTheme            = arr[0] as Boolean,
            showFps              = arr[1] as Boolean,
            perfOverlay          = arr[2] as Boolean,
            touchOverlayEnabled  = arr[3] as Boolean,
            confirmExit          = arr[4] as Boolean,
            runtimeBaseDir       = arr[5] as String,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())

    fun setDarkTheme(v: Boolean)           = viewModelScope.launch { repo.setDarkTheme(v) }
    fun setShowFps(v: Boolean)             = viewModelScope.launch { repo.setShowFps(v) }
    fun setPerfOverlay(v: Boolean)         = viewModelScope.launch { repo.setPerfOverlay(v) }
    fun setTouchOverlayEnabled(v: Boolean) = viewModelScope.launch { repo.setTouchOverlayEnabled(v) }
    fun setConfirmExit(v: Boolean)         = viewModelScope.launch { repo.setConfirmExit(v) }
}
