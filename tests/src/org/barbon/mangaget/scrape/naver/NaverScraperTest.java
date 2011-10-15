/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.naver;

import android.test.InstrumentationTestCase;

import org.barbon.mangaget.scrape.HtmlScrape;

import org.barbon.mangaget.tests.R;
import org.barbon.mangaget.tests.Utils;

public class NaverScraperTest extends InstrumentationTestCase {
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
}
