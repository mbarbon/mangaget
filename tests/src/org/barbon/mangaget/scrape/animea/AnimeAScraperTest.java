/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.animea;

import android.content.Context;

import android.os.Environment;

import android.test.InstrumentationTestCase;
import android.test.MoreAsserts;

import java.io.File;

import java.util.List;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Downloader;
import org.barbon.mangaget.scrape.HtmlScrape;

import org.barbon.mangaget.tests.DummyDownloader;
import org.barbon.mangaget.tests.R;
import org.barbon.mangaget.tests.Utils;

public class AnimeAScraperTest extends InstrumentationTestCase {
    public void testScrapeResults() {
        String searchPage =
            "http://manga.animea.net/search.html?title=";
        HtmlScrape.SearchResultPage res =
            AnimeAScraper.scrapeSearchResults(
                Utils.getPage(this, R.raw.animea_results_html,
                              searchPage));

        assertEquals(3, res.currentPage);
        assertEquals(167, res.lastPage);
        assertEquals("http://manga.animea.net/search.html?title=&page=%d",
                     res.pagingUrl);
        assertEquals(48, res.urls.size());
        assertEquals(48, res.titles.size());

        assertEquals("http://manga.animea.net/666-satan.html",
                     res.urls.get(0));
        assertEquals("http://manga.animea.net/a-ri-sa.html",
                     res.urls.get(47));

        assertEquals("666 Satan",
                     res.titles.get(0));
        assertEquals("A-ri-sa",
                     res.titles.get(47));
    }

    public void testScrapeEmptyResults() {
        String searchPage =
            "http://manga.animea.net/search.html?title=pavillon";
        HtmlScrape.SearchResultPage res =
            AnimeAScraper.scrapeSearchResults(
                Utils.getPage(this, R.raw.animea_results_empty_html,
                              searchPage));

        assertEquals(-1, res.currentPage);
        assertEquals(null, res.pagingUrl);
        assertEquals(0, res.urls.size());
        assertEquals(0, res.titles.size());
    }

    public void testScrapeMangaPage() {
        String mangaPage = "http://manga.animea.net/";
        HtmlScrape.ChapterPage results = AnimeAScraper.scrapeMangaPage(
                Utils.getPage(this, R.raw.animea_papillon_chapters_html,
                              mangaPage));
        List<HtmlScrape.ChapterInfo> res = results.chapters;

        assertEquals(29, res.size());
        assertEquals("http://manga.animea.net/papillon-hana-to-chou-chapter-1.html",
                     res.get(0).url);
        assertEquals("http://manga.animea.net/papillon-hana-to-chou-chapter-29.html",
                     res.get(28).url);
        assertEquals("Ageha, an ordinary (and rather unpopular",
                     results.summary.substring(0, 40));
        MoreAsserts.assertEquals(
            new String[] { "Drama", "Romance", "School Life", "Shoujo" },
            results.genres.toArray());
    }
}
