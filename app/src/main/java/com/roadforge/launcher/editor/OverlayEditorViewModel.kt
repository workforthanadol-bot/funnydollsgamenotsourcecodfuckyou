package com.roadforge.launcher.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadforge.launcher.data.TouchLayout
import com.roadforge.launcher.data.defaultBeamNGLayout
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OverlayEditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _layout = MutableStateFlow<TouchLayout?>(null)
    val layout = _layout.asStateFlow()

    private val _selectedButtonId = MutableStateFlow<String?>(null)
    val selectedButtonId = _selectedButtonId.asStateFlow()

    fun init(layoutId: String) {
        // TODO: load from DataStore/Room; for now use default
        _layout.value = defaultBeamNGLayout()
    }

    fun selectButton(id: String) {
        _selectedButtonId.value = if (_selectedButtonId.value == id) null else id
    }

    fun moveButton(id: String, dxFraction: Float, dyFraction: Float) {
        val current = _layout.value ?: return
        val updated = current.buttons.map { btn ->
            if (btn.id == id) btn.copy(
                xFraction = (btn.xFraction + dxFraction).coerceIn(0f, 0.95f),
                yFraction = (btn.yFraction + dyFraction).coerceIn(0f, 0.95f),
            ) else btn
        }
        _layout.value = current.copy(buttons = updated)
    }

    fun toggleVisible(id: String) {
        val current = _layout.value ?: return
        val updated = current.buttons.map { btn ->
            if (btn.id == id) btn.copy(isVisible = !btn.isVisible) else btn
        }
        _layout.value = current.copy(buttons = updated)
    }

    fun setAlpha(id: String, alpha: Float) {
        val current = _layout.value ?: return
        val updated = current.buttons.map { btn ->
            if (btn.id == id) btn.copy(alpha = alpha) else btn
        }
        _layout.value = current.copy(buttons = updated)
    }

    fun reset() {
        _layout.value = defaultBeamNGLayout()
        _selectedButtonId.value = null
    }

    fun save() {
        // TODO: persist layout to DataStore
        // For now the state is held in-memory for this session
    }
}
