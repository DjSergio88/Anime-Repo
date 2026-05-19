package eu.kanade.tachiyomi.animeextension.en.spankbang

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

class SpankBang : AnimeHttpSource() {

    override val name = "SpankBang"

    override val baseUrl = "https://spankbang.com"

    override val lang = "en"

    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request {
        return GET("$baseUrl/trending_videos/$page/")
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val animeList = document.select("div.video-item").map { element ->
            SAnime.create().apply {
                url = element.select("a.n").attr("href")
                title = element.select("a.n").text()
                thumbnail_url = element.select("img").attr("data-src").ifEmpty {
                    element.select("img").attr("src")
                }
            }
        }
        val hasNextPage = document.select("a.next").isNotEmpty()
        return AnimesPage(animeList, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/new_videos/$page/")
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET("$baseUrl/s/$query/$page/")
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

        val streamKey = html.substringAfter("data-streamkey=\"").substringBefore("\"")
        if (streamKey.isEmpty()) {
            return emptyList()
        }

        val id = response.request.url.toString().substringAfter("/").substringBefore("/")
        val apiResponse = client.newCall(GET("$baseUrl/api/videos/stream?id=$id&data=$streamKey", headers)).execute()
        val json = apiResponse.body.string()

        val qualities = listOf("4k", "1080p", "720p", "480p", "320p", "240p")
        return qualities.mapNotNull { quality ->
            val link = json.substringAfter("\"$quality\":[\"").substringBefore("\"").replace("\\/", "/")
            if (link.contains("http")) {
                Video(link, quality, link)
            } else {
                null
            }
        }
    }
}
