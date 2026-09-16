package com.ghostrunner.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ghostrunner.core.util.mpsToMinPerKm
import com.ghostrunner.core.data.PaceProfileRepository
import com.ghostrunner.core.domain.PaceProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    repository: PaceProfileRepository,
    profileId: Long?,
    onDone: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var paceMinPerKm by remember { mutableStateOf("5.0") }
    var strideMeters by remember { mutableStateOf("1.0") }
    var isDefault by remember { mutableStateOf(false) }
    var existingId by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profileId) {
        if (profileId != null) {
            repository.get(profileId)?.let { existing ->
                existingId = existing.id
                label = existing.label
                paceMinPerKm = "%.2f".format(mpsToMinPerKm(existing.targetMetersPerSec))
                strideMeters = "%.2f".format(existing.strideLengthMeters)
                isDefault = existing.isDefault
            }
        }
    }

    val paceFloat = paceMinPerKm.replace(',', '.').toFloatOrNull()
    val strideFloat = strideMeters.replace(',', '.').toFloatOrNull()
    val canSave = label.isNotBlank() && paceFloat != null && paceFloat > 0f &&
        strideFloat != null && strideFloat > 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profileId == null) "New profile" else "Edit profile") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = paceMinPerKm,
                onValueChange = { paceMinPerKm = it },
                label = { Text("Target pace (min/km)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = strideMeters,
                onValueChange = { strideMeters = it },
                label = { Text("Stride length (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                Text("Default profile")
            }
            Button(
                enabled = canSave,
                onClick = {
                    val pace = paceFloat ?: return@Button
                    val stride = strideFloat ?: return@Button
                    val mps = 1000f / (pace * 60f)
                    scope.launch {
                        val saved = PaceProfile(
                            id = existingId,
                            label = label.trim(),
                            targetMetersPerSec = mps,
                            strideLengthMeters = stride,
                            isDefault = isDefault,
                        )
                        val savedId = repository.upsert(saved)
                        if (isDefault) repository.setDefault(if (existingId == 0L) savedId else existingId)
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
