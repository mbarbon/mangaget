/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape;

import java.util.List;

import java.net.URI;
import java.net.URISyntaxException;

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

    public static class ChapterPage {
        public ChapterPage(List<ChapterInfo> _chapters) {
            chapters = _chapters;
        }

        public List<ChapterInfo> chapters;
        public String previousPage;
        public String nextPage;
    }

    public static String absoluteUrl(String url, String base) {
        try {
            if (!url.startsWith("http://"))
                url = new URI(base).resolve(url).toString();
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return url;
    }
}
