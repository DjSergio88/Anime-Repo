package eu.kanade.tachiyomi.animeextension.en.tnaflix

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response

class TNAFlix : AnimeHttpSource() {

    override val name = "TNAFlix"

    override val baseUrl = "https://www.tnaflix.com"

    override val lang = "en"

    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/most-popular/$page")
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.video-item, div.thumb-block").map { element ->
            SAnime.create().apply {
                val link = element.select("a").first()
                url = link?.attr("href") ?: ""
                title = element.select("img").attr("alt").ifEmpty { link?.attr("title") ?: "" }
                thumbnail_url = element.select("img").attr("data-src").ifEmpty { element.select("img").attr("src") }
            }
        }.filter { it.url.isNotEmpty() }

        val hasNextPage = document.select("a.pagination-next, ul.pagination li.active + li").isNotEmpty()
        return AnimesPage(animeList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/latest-updates/$page")
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$baseUrl/search.php?query=$query&page=$page")
    }

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun animeDetailsParse(response: Response): SAnime = SAnime.create()

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
        // Need to parse player config for video links
        return emptyList()
    }
}
