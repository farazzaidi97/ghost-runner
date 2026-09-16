package com.ghostrunner.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostrunner.core.domain.PaceProfile

@Entity(tableName = "pace_profile")
data class PaceProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val targetMetersPerSec: Float,
    val strideLengthMeters: Float,
    val isDefault: Boolean,
)

internal fun PaceProfileEntity.toDomain(): PaceProfile = PaceProfile(
    id = id,
    label = label,
    targetMetersPerSec = targetMetersPerSec,
    strideLengthMeters = strideLengthMeters,
    isDefault = isDefault,
)

internal fun PaceProfile.toEntity(): PaceProfileEntity = PaceProfileEntity(
    id = id,
    label = label,
    targetMetersPerSec = targetMetersPerSec,
    strideLengthMeters = strideLengthMeters,
    isDefault = isDefault,
)
