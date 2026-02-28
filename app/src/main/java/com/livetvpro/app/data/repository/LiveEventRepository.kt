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
     * - Always tries the external URL first (Remote Config: event_data_url).
     * - If external URL is configured → fetch from it, merge with native events
     *   (external first, deduped by id). Native events included only if loaded.
     * - If no external URL → fall back to native events only (original behaviour).
     *
     * NOTE: External events do NOT require isDataLoaded() — they are independent
     * of the native data pipeline and will show even if native data is empty.
     */
    suspend fun getLiveEvents(): List<LiveEvent> = withContext(Dispatchers.IO) {
        val externalEvents: List<LiveEvent>? = dataRepository.getExternalLiveEvents()

        return@withContext if (externalEvents == null) {
            // No external URL configured — use native only (original behaviour)
            if (dataRepository.isDataLoaded()) dataRepository.getLiveEvents() else emptyList()
        } else {
            // External URL configured — merge external + native (if available)
            val nativeEvents = if (dataRepository.isDataLoaded()) dataRepository.getLiveEvents() else emptyList()
            val externalIds = externalEvents.map { it.id }.toSet()
            externalEvents + nativeEvents.filter { it.id !in externalIds }
        }
    }

    suspend fun getEventById(eventId: String): LiveEvent? = withContext(Dispatchers.IO) {
        getLiveEvents().find { it.id == eventId }
    }

    suspend fun getEventCategories(): List<EventCategory> = withContext(Dispatchers.IO) {
        val nativeCategories = if (dataRepository.isDataLoaded())
            dataRepository.getEventCategories() else emptyList()

        // Derive categories from external API event_cat values.
        // No logo URL in this format — logoUrl is left blank so the adapter
        // falls back to ic_launcher_round (already handled in EventCategoryAdapter).
        val externalEvents = dataRepository.getExternalLiveEvents()
        val externalCategories = externalEvents
            ?.map { it.eventCategoryName }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.map { catName ->
                EventCategory(
                    id     = catName,   // use name as id for filtering
                    name   = catName,
                    slug   = catName.lowercase().replace(" ", "_"),
                    logoUrl = ""       // blank → adapter shows ic_launcher_round
                )
            } ?: emptyList()

        // Merge: native first, then external cats whose id isn't already present
        val nativeIds = nativeCategories.map { it.id }.toSet()
        nativeCategories + externalCategories.filter { it.id !in nativeIds }
    }
}
