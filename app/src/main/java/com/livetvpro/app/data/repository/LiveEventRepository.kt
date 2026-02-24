package com.livetvpro.app.data.repository

import com.livetvpro.app.data.models.EventCategory
import com.livetvpro.app.data.models.LiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveEventRepository @Inject constructor(
    private val dataRepository: NativeDataRepository
) {
    suspend fun getLiveEvents(): List<LiveEvent> = withContext(Dispatchers.IO) {
        if (!dataRepository.isDataLoaded()) {
            return@withContext emptyList()
        }
        return@withContext dataRepository.getLiveEvents()
    }

    suspend fun getEventById(eventId: String): LiveEvent? = withContext(Dispatchers.IO) {
        getLiveEvents().find { it.id == eventId }
    }

    suspend fun getEventCategories(): List<EventCategory> = withContext(Dispatchers.IO) {
        if (!dataRepository.isDataLoaded()) {
            return@withContext emptyList()
        }
        return@withContext dataRepository.getEventCategories()
    }
}
