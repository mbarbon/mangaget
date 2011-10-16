/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.naver;

import android.test.InstrumentationTestCase;

import java.util.List;

import org.barbon.mangaget.scrape.HtmlScrape;

import org.barbon.mangaget.tests.R;
import org.barbon.mangaget.tests.Utils;

public class NaverScraperTest extends InstrumentationTestCase {
    public void testScrapeImageUrls() {
        String chapterPage =
            "http://comic.naver.com/webtoon/detail.nhn?titleId=22896&no=1&weekday=mon";
        List<String> urls = NaverScraper.scrapeImageUrls(
            Utils.getPage(this, R.raw.naver_pink_lady_c1_p1_html,
                          chapterPage));

        assertEquals(8, urls.size());
        assertEquals("http://imgcomic.naver.com/webtoon/22896/1/001.jpg",
                     urls.get(0));
        assertEquals("http://imgcomic.naver.com/webtoon/22896/1/008.jpg",
                     urls.get(7));
    }

    public void testScrapeResults() {
        String searchPage =
            "http://comic.naver.com/search.nhn?m=webtoon&" +
            "keyword=%ED%95%91%ED%81%AC%EB%A0%88%EC%9D%B4%EB%94%94";
        HtmlScrape.SearchResultPage res =
            NaverScraper.scrapeSearchResults(
                Utils.getPage(this, R.raw.naver_results_html,
                              searchPage));

        assertEquals(1, res.currentPage);
        assertEquals("http://comic.naver.com/search.nhn?m=webtoon&keyword=%ED%95%91%ED%81%AC%EB%A0%88%EC%9D%B4%EB%94%94&page=%d",
                     res.pagingUrl);
        assertEquals(2, res.urls.size());
        assertEquals(2, res.titles.size());

        assertEquals("http://comic.naver.com/webtoon/list.nhn?titleId=22896",
                     res.urls.get(0));
        assertEquals("http://comic.naver.com/webtoon/list.nhn?titleId=68684",
                     res.urls.get(1));

        assertEquals("핑크레이디",
                     res.titles.get(0));
        assertEquals("핑크레이디 클래식",
                     res.titles.get(1));
    }

    public void testScrapeMangaPage() {
        String mangaPage =
            "http://comic.naver.com/webtoon/list.nhn?titleId=22896";
        HtmlScrape.ChapterPage list =
            NaverScraper.scrapeMangaPage(
                Utils.getPage(this, R.raw.naver_pink_lady_chapters_html,
                              mangaPage));
        List<HtmlScrape.ChapterInfo> res = list.chapters;

        assertEquals(10, res.size());
        assertEquals("http://comic.naver.com/webtoon/detail.nhn?" +
                     "titleId=22896&no=79&weekday=mon",
                     res.get(0).url);
        assertEquals("http://comic.naver.com/webtoon/detail.nhn?" +
                     "titleId=22896&no=88&weekday=mon",
                     res.get(9).url);

        assertEquals(null, list.nextPage);
        assertEquals("http://comic.naver.com/webtoon/list.nhn?titleId=22896&page=2",
                     list.previousPage);
    }
}
