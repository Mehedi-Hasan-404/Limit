package com.livetvpro.app.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val iconUrl: String? = null,
    val m3uUrl: String? = null,
    val order: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) : Parcelable

@Parcelize
data class Channel(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val groupTitle: String = "",
    val links: List<ChannelLink>? = null,
    val team1Logo: String = "",
    val team2Logo: String = "",
    val isLive: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
) : Parcelable

@Parcelize
data class ChannelLink(
    @SerializedName(value = "quality", alternate = ["label", "name"])
    val quality: String = "",
    val url: String = "",
    val cookie: String? = null,
    val referer: String? = null,
    val origin: String? = null,
    val userAgent: String? = null,
    val drmScheme: String? = null,
    val drmLicenseUrl: String? = null
) : Parcelable

@Parcelize
data class FavoriteChannel(
    val id: String = "",
    val name: String = "",
    val logoUrl: String = "",
    val streamUrl: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val links: List<ChannelLink>? = null
) : Parcelable

@Parcelize
data class LiveEvent(
    val id: String = "",
    val category: String = "",
    val league: String = "",
    val leagueLogo: String = "",
    val team1Name: String = "",
    val team1Logo: String = "",
    val team2Name: String = "",
    val team2Logo: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val isLive: Boolean = false,
    val links: List<LiveEventLink> = emptyList(),
    val title: String = "",
    val description: String = "",
    val wrapper: String = "",
    val eventCategoryId: String = "",
    val eventCategoryName: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
) : Parcelable {
    fun getStatus(currentTime: Long): EventStatus {
        val startTimestamp = parseTimestamp(startTime)
        val endTimestamp = endTime?.let { parseTimestamp(it) }

        return when {
            endTimestamp != null && currentTime >= startTimestamp && currentTime <= endTimestamp -> EventStatus.LIVE
            isLive && currentTime >= startTimestamp -> EventStatus.LIVE
            currentTime < startTimestamp -> EventStatus.UPCOMING
            endTimestamp != null && currentTime > endTimestamp -> EventStatus.RECENT
            currentTime > startTimestamp -> EventStatus.RECENT
            else -> EventStatus.UPCOMING
        }
    }

    private fun parseTimestamp(timeString: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.parse(timeString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

@Parcelize
data class LiveEventLink(
    @SerializedName(value = "quality", alternate = ["label", "name"])
    val quality: String = "",
    val url: String = "",
    val cookie: String? = null,
    val referer: String? = null,
    val origin: String? = null,
    val userAgent: String? = null,
    val drmScheme: String? = null,
    val drmLicenseUrl: String? = null
) : Parcelable

@Parcelize
data class EventCategory(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val logoUrl: String = "",
    val order: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
) : Parcelable

enum class EventStatus {
    LIVE, UPCOMING, RECENT
}

// ──────────────────────────────────────────────────────────────
// Old external event format (kept for backward compatibility)
// Remote Config key: event_data_url (old format)
// ──────────────────────────────────────────────────────────────

@Parcelize
data class ExternalEventLink(
    @SerializedName("name") val name: String = "",
    @SerializedName("link") val link: String = "",
    @SerializedName("scheme") val scheme: Int = 0,
    @SerializedName("api") val api: String = "",
    @SerializedName("tokenApi") val tokenApi: String = ""
) : Parcelable

@Parcelize
data class ExternalLiveEvent(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("visible") val visible: Boolean = true,
    @SerializedName("category") val category: String = "",
    @SerializedName("eventName") val eventName: String = "",
    @SerializedName("eventLogo") val eventLogo: String = "",
    @SerializedName("teamAName") val teamAName: String = "",
    @SerializedName("teamAFlag") val teamAFlag: String = "",
    @SerializedName("teamBName") val teamBName: String = "",
    @SerializedName("teamBFlag") val teamBFlag: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("time") val time: String = "",
    @SerializedName("end_date") val endDate: String = "",
    @SerializedName("end_time") val endTime: String = "",
    @SerializedName("links") val links: List<ExternalEventLink> = emptyList()
) : Parcelable {

    fun toLiveEvent(): LiveEvent {
        val startIso = combineDateTime(date, time)
        val endIso = if (endDate.isNotBlank() && endTime.isNotBlank())
            combineDateTime(endDate, endTime) else null

        val mappedLinks = links.map { extLink ->
            LiveEventLink(
                quality       = extLink.name,
                url           = extLink.link,
                drmScheme     = extLink.scheme.toDrmSchemeString(),
                drmLicenseUrl = extLink.api.ifBlank { null }
            )
        }

        return LiveEvent(
            id                = id.toString(),
            category          = category,
            league            = category,
            leagueLogo        = eventLogo,
            team1Name         = teamAName,
            team1Logo         = teamAFlag,
            team2Name         = teamBName,
            team2Logo         = teamBFlag,
            startTime         = startIso,
            endTime           = endIso,
            isLive            = false,
            links             = mappedLinks,
            title             = eventName,
            description       = "",
            wrapper           = "",
            eventCategoryId   = "",
            eventCategoryName = category
        )
    }

    private fun combineDateTime(date: String, time: String): String {
        return try {
            val inputFmt = java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()
            ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            val outputFmt = java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()
            ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            val parsed = inputFmt.parse("$date $time") ?: return ""
            outputFmt.format(parsed)
        } catch (e: Exception) {
            ""
        }
    }

    private fun Int.toDrmSchemeString(): String? = when (this) {
        0    -> "clearkey"
        1    -> "widevine"
        2    -> "playready"
        else -> null
    }
}

// ──────────────────────────────────────────────────────────────
// NEW external event format (Remote Config key: event_data_url)
// Flat rows grouped by event_id. Each row = one stream/link.
// Same event_id = same event, different channel_title = different links.
//
// DRM: keyid + key → ClearKey. null or "0" = no DRM.
// event_cat → category name; no logo → fallback to ic_launcher_round.
// event_time format: "yyyy-MM-dd HH:mm" (UTC)
// ──────────────────────────────────────────────────────────────

data class NewExternalEventRow(
    @SerializedName("event_title")   val eventTitle: String = "",
    @SerializedName("event_id")      val eventId: String = "",
    @SerializedName("event_cat")     val eventCat: String = "",
    @SerializedName("event_name")    val eventName: String = "",
    @SerializedName("event_time")    val eventTime: String = "",
    @SerializedName("event_end_time") val eventEndTime: String? = null,
    @SerializedName("channel_title") val channelTitle: String = "",
    @SerializedName("stream_url")    val streamUrl: String = "",
    @SerializedName("keyid")         val keyId: String? = null,
    @SerializedName("key")           val key: String? = null,
    @SerializedName("headers")       val headers: String? = null,
    @SerializedName("referer")       val referer: String? = null,
    @SerializedName("origin")        val origin: String? = null,
    @SerializedName("team_a_logo")   val teamALogo: String = "",
    @SerializedName("team_b_logo")   val teamBLogo: String = ""
)

/**
 * Groups a flat list of [NewExternalEventRow] by [NewExternalEventRow.eventId]
 * and converts each group into a single [LiveEvent] with multiple [LiveEventLink]s.
 *
 * - `channel_title` → link quality/name
 * - `keyid` + `key` → ClearKey DRM ("keyid:key"); null/"0" = no DRM
 * - `event_cat` → eventCategoryName (used as category chip in UI)
 * - No category logo in this format → pass empty string;
 *   the UI should fall back to R.mipmap.ic_launcher_round when it's blank.
 * - `event_time` ("yyyy-MM-dd HH:mm", UTC) → ISO-8601 UTC
 */
fun List<NewExternalEventRow>.toGroupedLiveEvents(): List<LiveEvent> {
    return groupBy { it.eventId }
        .map { (_, rows) ->
            val first = rows.first()

            val links = rows.map { row ->
                LiveEventLink(
                    quality       = row.channelTitle,
                    url           = row.streamUrl,
                    referer       = row.referer,
                    origin        = row.origin,
                    // ClearKey DRM: both keyid and key must be non-null and not "0"
                    drmScheme     = if (row.hasDrm()) "clearkey" else null,
                    drmLicenseUrl = if (row.hasDrm()) "${row.keyId}:${row.key}" else null
                )
            }

            LiveEvent(
                id                = first.eventId,
                category          = first.eventCat,
                league            = first.eventCat,
                // No category logo in this format — UI falls back to ic_launcher_round
                leagueLogo        = "",
                team1Name         = first.eventTitle,
                team1Logo         = first.teamALogo,
                team2Name         = first.eventTitle,
                team2Logo         = first.teamBLogo,
                startTime         = first.eventTime.toIso8601Utc() ?: first.eventTime,
                // Use real end time from API; fall back to startTime + 3h if missing or conversion fails
                endTime           = first.eventEndTime?.toIso8601Utc()
                                    ?: first.eventTime.toIso8601UtcPlusHours(3),
                isLive            = false,
                links             = links,
                title             = first.eventName,
                description       = "",
                wrapper           = "",
                eventCategoryId   = first.eventCat,
                eventCategoryName = first.eventCat
            )
        }
}

/** True when the row has valid ClearKey DRM credentials. */
private fun NewExternalEventRow.hasDrm(): Boolean {
    return !keyId.isNullOrBlank() && keyId != "0"
        && !key.isNullOrBlank() && key != "0"
}

/**
 * Converts "yyyy-MM-dd HH:mm" (UTC) → "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" (ISO-8601 UTC).
 * Returns null on parse failure instead of the raw string, so callers can detect and handle it.
 */
private fun String.toIso8601Utc(): String? {
    return try {
        val inputFmt = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm", java.util.Locale.US
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+1")  // API times are UTC+1
            isLenient = false
        }
        val outputFmt = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val parsed = inputFmt.parse(this) ?: return null
        outputFmt.format(parsed)
    } catch (e: Exception) {
        null
    }
}

/**
 * Same as [toIso8601Utc] but adds [hours] to the parsed time.
 * Used to synthesise a default endTime when the API doesn't provide one.
 * Returns null on parse failure.
 */
private fun String.toIso8601UtcPlusHours(hours: Int): String? {
    return try {
        val inputFmt = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm", java.util.Locale.US
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+1")  // API times are UTC+1
            isLenient = false
        }
        val outputFmt = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US
        ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val parsed = inputFmt.parse(this) ?: return null
        outputFmt.format(java.util.Date(parsed.time + hours * 3_600_000L))
    } catch (e: Exception) {
        null
    }
}
