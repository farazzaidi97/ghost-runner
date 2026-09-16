package com.ghostrunner.core.data

import com.ghostrunner.core.domain.PaceProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PaceProfileRepository(private val dao: PaceProfileDao) {

    fun observeAll(): Flow<List<PaceProfile>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun get(id: Long): PaceProfile? = dao.findById(id)?.toDomain()

    suspend fun getDefault(): PaceProfile? = dao.findDefault()?.toDomain()

    suspend fun upsert(profile: PaceProfile): Long {
        val entity = profile.toEntity()
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun delete(profile: PaceProfile) {
        dao.delete(profile.toEntity())
    }

    suspend fun setDefault(id: Long) {
        dao.setDefault(id)
    }
}
