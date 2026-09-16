package com.ghostrunner.wear.di

import android.content.Context
import com.ghostrunner.core.data.GhostRunnerDatabase
import com.ghostrunner.core.data.PaceProfileRepository
import com.ghostrunner.core.domain.PaceProfile
import com.ghostrunner.core.runtime.PacingSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class AppContainer(context: Context) {
    private val database = GhostRunnerDatabase.get(context)
    val paceProfileRepository = PaceProfileRepository(database.paceProfileDao())

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _livePacingSnapshot = MutableStateFlow<PacingSnapshot?>(null)
    val livePacingSnapshot: StateFlow<PacingSnapshot?> = _livePacingSnapshot

    init {
        scope.launch { seedDefaultsIfEmpty() }
    }

    internal fun emitSnapshot(snapshot: PacingSnapshot?) {
        _livePacingSnapshot.value = snapshot
    }

    private suspend fun seedDefaultsIfEmpty() {
        val current = paceProfileRepository.observeAll().first()
        if (current.isNotEmpty()) return
        paceProfileRepository.upsert(PaceProfile.fromPaceMinKm("Easy 6:00/km", 6.0f))
        val tempoId = paceProfileRepository.upsert(PaceProfile.fromPaceMinKm("Tempo 5:00/km", 5.0f))
        paceProfileRepository.upsert(PaceProfile.fromPaceMinKm("Race 4:30/km", 4.5f))
        paceProfileRepository.setDefault(tempoId)
    }
}
