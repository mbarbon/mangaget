/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.animea;

import android.content.Context;

import android.os.Environment;

import android.test.InstrumentationTestCase;

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

        assertEquals(2, res.currentPage);
        assertEquals("http://manga.animea.net/search.html?title=&completed=0&yor_range=0&yor=&type=any&author=&artist=&genre%5BAction%5D=2&genre%5BAdventure%5D=0&genre%5BComedy%5D=0&genre%5BDoujinshi%5D=0&genre%5BDrama%5D=0&genre%5BEcchi%5D=0&genre%5BFantasy%5D=0&genre%5BGender_Bender%5D=0&genre%5BHarem%5D=0&genre%5BHistorical%5D=0&genre%5BHorror%5D=0&genre%5BJosei%5D=0&genre%5BMartial_Arts%5D=0&genre%5BMature%5D=0&genre%5BMecha%5D=0&genre%5BMystery%5D=0&genre%5BPsychological%5D=0&genre%5BRomance%5D=0&genre%5BSchool_Life%5D=0&genre%5BSci-fi%5D=0&genre%5BSeinen%5D=0&genre%5BShotacon%5D=0&genre%5BShoujo%5D=0&genre%5BShoujo_Ai%5D=0&genre%5BShounen%5D=0&genre%5BShounen_Ai%5D=0&genre%5BSlice_of_Life%5D=0&genre%5BSmut%5D=0&genre%5BSports%5D=0&genre%5BSupernatural%5D=0&genre%5BTragedy%5D=0&genre%5BYaoi%5D=0&genre%5BYuri%5D=0&page=%d", res.pagingUrl);
        assertEquals(48, res.urls.size());
        assertEquals(48, res.titles.size());

        assertEquals("http://manga.animea.net/2-kaime-no-hajimete-no-koi.html",
                     res.urls.get(0));
        assertEquals("http://manga.animea.net/a-house-in-venice.html",
                     res.urls.get(47));

        assertEquals("2 Kaime no Hajimete no Koi",
                     res.titles.get(0));
        assertEquals("A House in Venice",
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
        List<HtmlScrape.ChapterInfo> res =
            AnimeAScraper.scrapeMangaPage(
                Utils.getPage(this, R.raw.animea_papillon_chapters_html,
                              mangaPage));

        assertEquals(29, res.size());
        assertEquals("http://manga.animea.net/papillon-hana-to-chou-chapter-1-page-1.html",
                     res.get(0).url);
        assertEquals("http://manga.animea.net/papillon-hana-to-chou-chapter-29-page-1.html",
                     res.get(28).url);
    }
}
