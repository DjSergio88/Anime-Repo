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
                title = element.select("img").attr("alt").ifEmpty {
                    link?.attr("title") ?: ""
                }
                thumbnail_url = element.select("img").attr("data-src").ifEmpty {
                    element.select("img").attr("src")
                }
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

    override fun animeDetailsParse(response: Response): SAnime {
        return SAnime.create().apply {
            status = SAnime.COMPLETED
            initialized = true
        }
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val episode = SEpisode.create().apply {
            url = response.request.url.toString()
            name = "Video"
            episode_number = 1f
        }
        return listOf(episode)
    }

    override fun videoListParse(response: Response): List<Video> {
        val html = response.body.string()

        val videoUrl = html.substringAfter("video_url: '").substringBefore("'")
        if (videoUrl.isNotEmpty()) {
            return listOf(Video(videoUrl, "Default", videoUrl))
        }

        val flashvars = html.substringAfter("flashvars = {").substringBefore("};")
        val link = flashvars.substringAfter("\"video_url\":\"").substringBefore("\"").replace("\\/", "/")
        if (link.isNotEmpty()) {
            return listOf(Video(link, "Default", link))
        }

        return emptyList()
    }
}
