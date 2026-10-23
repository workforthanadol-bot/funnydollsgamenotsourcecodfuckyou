package com.roadforge.launcher.permissions

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PermissionsViewModel(app: Application) : AndroidViewModel(app) {

    private val _hasOverlay = MutableStateFlow(false)
    private val _hasStorage = MutableStateFlow(false)

    val hasOverlay = _hasOverlay.asStateFlow()
    val hasStorage = _hasStorage.asStateFlow()

    val allGranted = combine(_hasOverlay, _hasStorage) { o, s -> o && s }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init { recheck() }

    fun recheck() {
        val ctx = getApplication<Application>()
        _hasOverlay.value = Settings.canDrawOverlays(ctx)
        _hasStorage.value = if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
