/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.mangareader;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.List;

import org.barbon.mangaget.scrape.Downloader;
import org.barbon.mangaget.scrape.HtmlScrape;
import org.barbon.mangaget.scrape.Scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;

public class MangareaderScraper {
    private static final String[] SUPPORTED_TAGS = new String[] {
        "Action", "Adventure", "Comedy", "Demons", "Drama", "Ecchi",
        "Fantasy", "Gender Bender", "Harem", "Historical", "Horror",
        "Josei", "Magic", "Martial Arts", "Mature", "Mecha",
        "Military", "Mystery", "One Shot", "Psychological",
        "Romance", "School Life", "Sci-Fi", "Seinen",
        "Shoujo", "Shoujo Ai", "Shounen", "Shounen Ai", "Slice of Life",
        "Smut", "Sports", "Super Power", "Supernatural",
        "Tragedy", "Vampire", "Yaoi", "Yuri"
    };

    // scraper interface
    public static class Provider extends Scraper.Provider {
        private static final String MANGAREADER_URL =
            "http://www.mangareader.net/";

        @Override
        public boolean canHandleUrl(String url) {
            return url.startsWith(MANGAREADER_URL);
        }

        @Override
        public String getName() {
            return "MangaReader";
        }

        @Override
        public String composeSearchUrl(Scraper.SearchCriteria criteria) {
            return MangareaderScraper.composeSearchUrl(criteria);
        }

        @Override
        public String composePagingUrl(String pagingUrl, int page) {
            return super.composePagingUrl(pagingUrl, (page - 1) * 30);
        }

        @Override
        public String[] supportedTags() {
            return SUPPORTED_TAGS;
        }

        @Override
        public List<String> scrapeChapterPages(
                Downloader.DownloadDestination target) {
            return MangareaderScraper.scrapeChapterPages(target);
        }

        @Override
        public List<String> scrapeImageUrls(
                Downloader.DownloadDestination target) {
            List<String> urls = new ArrayList<String>(1);

            urls.add(MangareaderScraper.scrapeImageUrl(target));

            return urls;
        }

        @Override
        public HtmlScrape.SearchResultPage scrapeSearchResults(
                Downloader.DownloadDestination target) {
            return MangareaderScraper.scrapeSearchResults(target);
        }

        @Override
        public HtmlScrape.ChapterPage scrapeMangaPage(
                Downloader.DownloadDestination target) {
            return MangareaderScraper.scrapeMangaPage(target);
        }
    }

    public static String composeSearchUrl(Scraper.SearchCriteria criteria) {
        String encodedTitle = "", genres = "";

        if (criteria.title != null) {
            // it seems that Mangareader strips non-ASCII characters
            // from the search term; do the same (and return a null
            // search URL if there aren't any ASCII characters)
            StringBuffer filtered = new StringBuffer();

            for (int i = 0; i < criteria.title.length(); ++i) {
                // keep both ASCII and Latin-1 characters (just in case)
                if (criteria.title.charAt(i) < 255)
                    filtered.append(criteria.title.charAt(i));
            }

            String filteredTitle = filtered.toString().trim();

            if (filteredTitle.length() == 0 && criteria.title.trim().length() != 0)
                return null;

            try {
                encodedTitle = URLEncoder.encode(filteredTitle, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        if (criteria.includeTags != null && criteria.includeTags.size() != 0) {
            boolean selected = false;
            StringBuffer map = new StringBuffer("&genre=");

            for (String tag : SUPPORTED_TAGS) {
                if (criteria.includeTags.indexOf(tag) != -1) {
                    map.append('1');
                    selected = true;
                } else {
                    map.append('0');
                }
            }

            if (!selected)
                return null;

            genres = map.toString();
        }

        return "http://www.mangareader.net/search/?w=" +
            encodedTitle + genres + "&p=0";
    }

    // pure HTML scraping

    public static List<String> scrapeChapterPages(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Element pageMenu = doc.select("select#pageMenu").first();

        Elements options = pageMenu.select("option");
        List<String> result = new ArrayList<String>();

        for (Element option : options) {
            if (!option.hasAttr("value"))
                continue;
            result.add(HtmlScrape.absoluteUrl(
                           option.attr("value"), target.baseUrl));
        }

        return result;
    }

    public static String scrapeImageUrl(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Element img = doc.select("div#imgholder > a > img").first();

        if (!img.hasAttr("src"))
            return null;

        return img.attr("abs:src");
    }

    public static HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Elements mangas = doc.select("div.manga_name a");
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();

        for (Element manga : mangas) {
            urls.add(manga.attr("abs:href"));
            titles.add(manga.text());
        }

        Elements links = doc.select("div#sp > a");
        Element current = doc.select("div#sp > strong").first();
        int currentPage = -1, lastPage = -1;
        String pagingUrl = null;

        if (current != null)
            currentPage = Integer.valueOf(current.text());

        for (Element link : links) {
            String href = link.attr("abs:href");
            int index = href.lastIndexOf("&p=");

            if (index == -1 || link.text().equals(">"))
                continue;

            pagingUrl = href.substring(0, index + 3).replace("%", "%%") + "%d";
            lastPage = Integer.valueOf(href.substring(index + 3)) / 30 + 1;
        }

        HtmlScrape.SearchResultPage page = new HtmlScrape.SearchResultPage();

        page.urls = urls;
        page.titles = titles;
        page.pagingUrl = pagingUrl;
        page.currentPage = currentPage;
        page.lastPage = lastPage > currentPage ? lastPage : currentPage;

        return page;
    }

    public static HtmlScrape.ChapterPage scrapeMangaPage(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Elements links = doc.select("div#chapterlist a");
        List<HtmlScrape.ChapterInfo> chapters =
            new ArrayList<HtmlScrape.ChapterInfo>();
        HtmlScrape.ChapterPage result = new HtmlScrape.ChapterPage(chapters);

        for (Element link : links) {
            if (!link.hasAttr("href"))
                continue;

            String url = link.attr("abs:href");
            if (url.indexOf("/chapter-") == -1 || !url.endsWith(".html"))
                continue;
            int dash = url.lastIndexOf('-'), dot = url.lastIndexOf('.');
            String indexS = url.substring(dash + 1, dot);

            String title = link.text();
            HtmlScrape.ChapterInfo info = new HtmlScrape.ChapterInfo();

            info.title = title;
            info.url = url;
            info.index = (int) (Float.valueOf(indexS) * 100);

            chapters.add(info);
        }

        Element container = doc.select("div#readmangasum").first();

        if (container != null) {
            Element summary = container.select("p").get(0);

            result.summary = summary.text().trim();
        }

        List<String> genres = result.genres = new ArrayList<String>();

        for (Element span : doc.select("div#mangaproperties span.genretags"))
            genres.add(span.text().trim());

        return result;
    }
}
