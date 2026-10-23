package com.roadforge.launcher.diagnostics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadforge.launcher.runtime.DeviceInfo
import com.roadforge.launcher.runtime.DeviceInspector
import com.roadforge.launcher.runtime.RuntimeChecker
import com.roadforge.launcher.runtime.RuntimeStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiagnosticsViewModel(app: Application) : AndroidViewModel(app) {

    private val checker = RuntimeChecker(app)

    private val _deviceInfo     = MutableStateFlow<DeviceInfo?>(null)
    private val _runtimeStatus  = MutableStateFlow<RuntimeStatus?>(null)
    private val _isChecking     = MutableStateFlow(false)

    val deviceInfo    = _deviceInfo.asStateFlow()
    val runtimeStatus = _runtimeStatus.asStateFlow()
    val isChecking    = _isChecking.asStateFlow()

    fun init(gameId: String) {
        recheck()
    }

    fun recheck() {
        viewModelScope.launch {
            _isChecking.value = true
            val device = DeviceInspector.inspect(getApplication())
            _deviceInfo.value    = device
            _runtimeStatus.value = checker.checkAll(device)
            _isChecking.value = false
        }
    }
}
