package eu.kanade.tachiyomi.animeextension.en.eporner

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class EPorner : AnimeHttpSource() {

    override val name = "EPorner"

    override val baseUrl = "https://www.eporner.com"

    override val lang = "en"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    private val apiUrl = "https://www.eporner.com/api/v2/video"

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$apiUrl/search/?query=popular&per_page=30&page=$page&thumbsize=big")
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val responseData = json.decodeFromString<EPornerResponse>(response.body.string())
        val animeList = responseData.videos.map { video ->
            SAnime.create().apply {
                url = video.url
                title = video.title
                thumbnail_url = video.default_thumb.src
            }
        }
        return AnimesPage(animeList, responseData.videos.isNotEmpty())
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$apiUrl/search/?query=latest&per_page=30&page=$page&thumbsize=big")
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$apiUrl/search/?query=$query&per_page=30&page=$page&thumbsize=big")
    }

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun animeDetailsParse(response: Response): SAnime {
        // We get basic details from search, but can refine here if needed
        // For now, return the anime as is (initialized = true in episode list)
        return SAnime.create()
    }

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        anime.initialized = true
        return anime
    }

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        val episode = SEpisode.create().apply {
            url = anime.url
            name = "Video"
            episode_number = 1f
        }
        return listOf(episode)
    }

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val id = episode.url.substringAfter("/video-").substringBefore("/")
        val response = client.newCall(GET("$apiUrl/id/?id=$id")).execute()
        val responseData = json.decodeFromString<EPornerVideoDetails>(response.body.string())

        return responseData.sources.mp4.map { (quality, link) ->
            Video(link, quality, link)
        }
    }

    @Serializable
    data class EPornerResponse(
        val videos: List<EPornerVideo>
    )

    @Serializable
    data class EPornerVideo(
        val title: String,
        val url: String,
        val default_thumb: EPornerThumb
    )

    @Serializable
    data class EPornerThumb(
        val src: String
    )

    @Serializable
    data class EPornerVideoDetails(
        val sources: EPornerSources
    )

    @Serializable
    data class EPornerSources(
        val mp4: Map<String, String>
    )
}
