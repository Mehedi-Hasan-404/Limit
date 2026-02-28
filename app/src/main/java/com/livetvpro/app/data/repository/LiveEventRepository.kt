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
    /**
     * Returns live events.
     *
     * - If Remote Config has `event_data_url` set → fetch from that URL and
     *   merge with native events (external first, deduped by id).
     * - Otherwise → native events only (original behaviour, unchanged).
     */
    suspend fun getLiveEvents(): List<LiveEvent> = withContext(Dispatchers.IO) {
        val nativeEvents: List<LiveEvent> = if (dataRepository.isDataLoaded()) {
            dataRepository.getLiveEvents()
        } else {
            emptyList()
        }

        val externalEvents: List<LiveEvent>? = dataRepository.getExternalLiveEvents()

        return@withContext if (externalEvents == null) {
            // No external URL configured — original behaviour
            nativeEvents
        } else {
            // Merge: external first, then native events not already present
            val externalIds = externalEvents.map { it.id }.toSet()
            externalEvents + nativeEvents.filter { it.id !in externalIds }
        }
    }

    suspend fun getEventById(eventId: String): LiveEvent? = withContext(Dispatchers.IO) {
        getLiveEvents().find { it.id == eventId }
    }

    suspend fun getEventCategories(): List<EventCategory> = withContext(Dispatchers.IO) {
        if (!dataRepository.isDataLoaded()) return@withContext emptyList()
        dataRepository.getEventCategories()
    }
}
