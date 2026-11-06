package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.APIExtract
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.Jsoup

class RareToonsIndiaProvider : MainAPI() {
    override var mainUrl = "https://www.rareanimes.fans"
    override var name = "Rare Toons India"
    override val hasMainPage = true
    override var lang = "hi"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)

    override val homepage = 1

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val home = mutableListOf<HomePageList>()

        val elements = document.select("div.recent-releases, div.featured-items, div.post-items")
        val animeList = elements.mapNotNull { element ->
            val title = element.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
            val link = fixUrl(element.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
            val poster = element.selectFirst("img")?.attr("src") ?: element.selectFirst("img")?.attr("data-src")
            newTvSeriesSearchResponse(title, link, TvType.Anime) {
                this.posterUrl = fixUrl(poster)
            }
        }

        if (animeList.isNotEmpty()) {
            home.add(HomePageList("Trending Anime", animeList))
        }

        return newHomePageResponse(home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val document = app.get(searchUrl).document

        return document.select("article, div.item, div.search-item").mapNotNull { element ->
            val title = element.selectFirst("h2 a, h3 a, .title a")?.text() ?: return@mapNotNull null
            val href = fixUrl(element.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
            val poster = element.selectFirst("img")?.attr("src") ?: element.selectFirst("img")?.attr("data-src")

            newTvSeriesSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = fixUrl(poster)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title, h1.title")?.text() ?: "Unknown"
        val poster = document.selectFirst(".poster img, .entry-content img")?.attr("src")
        val description = document.selectFirst(".entry-content p, .synopsis")?.text()
        val genres = document.select(".genres a, .cat-links a").map { it.text() }

        val episodes = mutableListOf<Episode>()
        document.select("ul.episodelist li, .les-content a, .se-c a").forEach { ep ->
            val epName = ep.text()
            val epLink = fixUrl(ep.attr("href"))
            episodes.add(
                newEpisode(epLink) {
                    this.name = epName
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.Anime, episodes) {
            this.posterUrl = fixUrl(poster)
            this.plot = description
            this.tags = genres
        }
    }

    override suspend fun loadLinks(url: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(url).document
        val iframeSrc = document.selectFirst("iframe")?.attr("src") ?: document.selectFirst("iframe")?.attr("data-src")

        if (!iframeSrc.isNullOrEmpty()) {
            loadExtractor(fixUrl(iframeSrc), url, subtitleCallback, callback)
        }

        return true
    }
}
