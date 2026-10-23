package com.roadforge.launcher.library

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.roadforge.launcher.data.AppDatabase
import com.roadforge.launcher.data.GameProfile
import com.roadforge.launcher.data.GameStatus
import com.roadforge.launcher.runtime.RuntimeService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).gameProfileDao()

    val games = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** เรียกจาก UI ก่อน navigate → SAF picker จะถูกจัดการที่ MainActivity */
    fun requestFolderPicker() { /* handled by MainActivity via ActivityResultContracts */ }
    fun requestExePicker()    { /* handled by MainActivity via ActivityResultContracts */ }

    fun launchGame(game: GameProfile) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, RuntimeService::class.java).apply {
            action = RuntimeService.ACTION_LAUNCH
            putExtra(RuntimeService.EXTRA_GAME_ID, game.id)
        }
        ctx.startForegroundService(intent)
    }

    fun removeGame(game: GameProfile) {
        viewModelScope.launch { dao.delete(game) }
    }
}
