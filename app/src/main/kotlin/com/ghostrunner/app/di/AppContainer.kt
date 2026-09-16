package com.ghostrunner.app.di

import android.content.Context
import com.ghostrunner.core.runtime.PacingSnapshot
import com.ghostrunner.core.data.GhostRunnerDatabase
import com.ghostrunner.core.data.PaceProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppContainer(context: Context) {
    private val database = GhostRunnerDatabase.get(context)
    val paceProfileRepository = PaceProfileRepository(database.paceProfileDao())

    private val _livePacingSnapshot = MutableStateFlow<PacingSnapshot?>(null)
    val livePacingSnapshot: StateFlow<PacingSnapshot?> = _livePacingSnapshot

    internal fun emitSnapshot(snapshot: PacingSnapshot?) {
        _livePacingSnapshot.value = snapshot
    }
}
