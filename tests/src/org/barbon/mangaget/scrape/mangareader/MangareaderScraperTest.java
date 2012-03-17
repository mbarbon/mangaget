/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.mangareader;

import android.test.InstrumentationTestCase;

import java.util.List;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.HtmlScrape;

import org.barbon.mangaget.tests.R;
import org.barbon.mangaget.tests.Utils;

public class MangareaderScraperTest extends InstrumentationTestCase {
    public void testScrapeChapterPages() {
        String chapterPage =
            "http://www.mangareader.net/462-28574-1/goong/chapter-1.html";
        List<String> urls = MangareaderScraper.scrapeChapterPages(
            Utils.getPage(this, R.raw.mangareader_goong_c1_p1_html,
                          chapterPage));

        assertEquals(27, urls.size());
        assertEquals("http://www.mangareader.net/462-28574-1/goong/chapter-1.html",
                     urls.get(0));
        assertEquals("http://www.mangareader.net/462-28574-27/goong/chapter-1.html",
                     urls.get(26));
    }

    public void testScrapeImageUrl() {
        String chapterPage =
            "http://www.mangareader.net/462-28574-1/goong/chapter-1.html";
        String url = MangareaderScraper.scrapeImageUrl(
            Utils.getPage(this, R.raw.mangareader_goong_c1_p1_html,
                          chapterPage));

        assertEquals("http://i10.mangareader.net/goong/1/goong-678205.jpg",
                     url);
    }

    public void testScrapeResults() {
        String searchPage =
            "http://www.mangareader.net/search/?w=&p=30";
        HtmlScrape.SearchResultPage res =
            MangareaderScraper.scrapeSearchResults(
                Utils.getPage(this, R.raw.mangareader_results_html,
                              searchPage));

        assertEquals(2, res.currentPage);
        assertEquals(75, res.lastPage);
        assertEquals("http://www.mangareader.net/search/?w=&p=%d",
                     res.pagingUrl);
        assertEquals(30, res.urls.size());
        assertEquals(30, res.titles.size());

        assertEquals("http://www.mangareader.net/132/yotsubato.html",
                     res.urls.get(0));
        assertEquals("http://www.mangareader.net/168/black-cat.html",
                     res.urls.get(29));

        assertEquals("Yotsubato!",
                     res.titles.get(0));
        assertEquals("Black Cat",
                     res.titles.get(29));
    }

    public void testScrapeEmptyResults() {
        String searchPage =
            "http://www.mangareader.net/search/?w=trzrt";
        HtmlScrape.SearchResultPage res =
            MangareaderScraper.scrapeSearchResults(
                Utils.getPage(this, R.raw.mangareader_results_empty_html,
                              searchPage));

        assertEquals(-1, res.currentPage);
        assertEquals(null, res.pagingUrl);
        assertEquals(0, res.urls.size());
        assertEquals(0, res.titles.size());
    }

    public void testScrapeMangaPage() {
        String mangaPage = "http://www.mangareader.net/462/goong.html";
        List<HtmlScrape.ChapterInfo> res =
            MangareaderScraper.scrapeMangaPage(
                Utils.getPage(this, R.raw.mangareader_goong_chapters_html,
                              mangaPage));

        assertEquals(140, res.size());
        assertEquals("http://www.mangareader.net/462-28574-1/goong/chapter-1.html",
                     res.get(0).url);
        assertEquals("http://www.mangareader.net/goong/140",
                     res.get(139).url);
    }
}
