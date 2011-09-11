/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape;

import java.util.List;

public class HtmlScrape {
    public static class SearchResultPage {
        public List<String> urls;
        public List<String> titles;
        public String pagingUrl;
        public int currentPage;
    }

    public static class ChapterInfo {
        public String url;
        public String title;
    }
}
