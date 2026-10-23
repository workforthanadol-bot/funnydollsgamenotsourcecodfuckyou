package com.roadforge.launcher.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadforge.launcher.data.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).gameProfileDao()
    private val manager = SetupManager(app)

    val setupState = manager.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetupState.Idle)

    val tasks = manager.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val progress = manager.progressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val logLines = manager.logLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var gameId: String? = null

    fun init(id: String) {
        gameId = id
    }

    fun startSetup() {
        val id = gameId ?: return
        viewModelScope.launch {
            val profile = dao.getById(id) ?: return@launch
            manager.run(profile)
        }
    }

    fun cancelSetup() {
        manager.cancel()
    }
}
