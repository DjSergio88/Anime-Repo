package eu.kanade.tachiyomi.animeextension.en.xhamster

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

class XHamster : AnimeHttpSource() {

    override val name = "xHamster"

    override val baseUrl = "https://xhamster.com"

    override val lang = "en"

    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/best/all/monthly?p=$page")
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.video-thumb").map { element ->
            SAnime.create().apply {
                url = element.select("a.video-thumb__image-container").attr("href")
                title = element.select("div.video-thumb__name").text()
                thumbnail_url = element.select("img").attr("data-src").ifEmpty {
                    element.select("img").attr("src")
                }
            }
        }
        val hasNextPage = document.select("a[data-page=next]").isNotEmpty()
        return AnimesPage(animeList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/new?p=$page")
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$baseUrl/search/$query?p=$page")
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

        val initials = html.substringAfter("window.initials = ").substringBefore("};") + "}"
        if (!initials.contains("http")) {
            return emptyList()
        }

        val sources = initials.substringAfter("\"sources\":{").substringBefore("}")
        val qualities = listOf("1080p", "720p", "480p", "360p", "240p")

        return qualities.mapNotNull { quality ->
            val link = sources.substringAfter("\"$quality\":\"").substringBefore("\"").replace("\\/", "/")
            if (link.contains("http")) {
                Video(link, quality, link)
            } else {
                null
            }
        }
    }
}
