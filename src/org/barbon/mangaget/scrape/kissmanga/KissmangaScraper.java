/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.kissmanga;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.List;

import org.barbon.mangaget.scrape.Downloader;
import org.barbon.mangaget.scrape.HtmlScrape;
import org.barbon.mangaget.scrape.Scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.parser.Tag;

import org.jsoup.select.Elements;

public class KissmangaScraper {
    private static final String[] SUPPORTED_TAGS = new String[] {
        "Action", "Adult", "Adventure", "Comedy", null, "Drama",
        "Ecchi", "Fantasy", "Gender Bender", "Harem", "Historical",
        "Horror", "Josei", null, null, null, null, "Martial Arts",
        "Mature", "Mecha", "Mystery", "One shot", "Psychological",
        "Romance", "School Life", "Sci-fi", "Seinen", "Shotacon",
        "Shoujo", "Shoujo Ai", "Shounen", "Shounen Ai",
        "Slice of Life", "Smut", "Sports", "Supernatural",
        "Tragedy", "Yaoi", "Yuri"
    };

    // scraper interface
    public static class Provider extends Scraper.Provider {
        private static final String KISSMANGA_URL = "http://kissmanga.com/";

        @Override
        public boolean canHandleUrl(String url) {
            return url.startsWith(KISSMANGA_URL);
        }

        @Override
        public String getName() {
            return "Kissmanga";
        }

        @Override
        public List<String> composeSearchForm(Scraper.SearchCriteria criteria) {
            return KissmangaScraper.composeSearchForm(criteria);
        }

        @Override
        public String composePagingUrl(String pagingUrl, int page) {
            return null;
        }

        @Override
        public String[] supportedTags() {
            return SUPPORTED_TAGS;
        }

        @Override
        public List<String> scrapeChapterPages(
                Downloader.DownloadDestination target) {
            if (target.isRedirect)
                return null;

            // Kissmanga has all chapter images in the same HTML page...
            List<String> urls = new ArrayList<String>(1);

            urls.add(target.baseUrl);

            return urls;
        }

        @Override
        public List<String> scrapeImageUrls(
                Downloader.DownloadDestination target) {
            if (target.isRedirect)
                return null;

            return KissmangaScraper.scrapeImageUrls(target);
        }

        @Override
        public HtmlScrape.SearchResultPage scrapeSearchResults(
                Downloader.DownloadDestination target) {
            if (target.isRedirect)
                return KissmangaScraper.scrapeSearchRedirect(target);
            else
                return KissmangaScraper.scrapeSearchResults(target);
        }

        @Override
        public HtmlScrape.ChapterPage scrapeMangaPage(
                Downloader.DownloadDestination target) {
            if (target.isRedirect)
                return null;

            return KissmangaScraper.scrapeMangaPage(target);
        }

        @Override
        public boolean handleRedirects() {
            return true;
        }
    }

    public static List<String> composeSearchForm(Scraper.SearchCriteria criteria) {
        List<String> form = new ArrayList<String>();

        form.add("http://kissmanga.com/Search/Manga");

        if (criteria.title != null) {
            form.add("keyword");
            form.add(criteria.title);
        }

        if (criteria.includeTags != null && criteria.includeTags.size() != 0) {
            form.set(0, "http://kissmanga.com/AdvanceSearch");

            if (criteria.title != null)
                form.set(1, "mangaName");

            form.add("authorArtist");
            form.add("");

            boolean selected = false;

            for (String tag : SUPPORTED_TAGS) {
                form.add("genres");

                if (criteria.includeTags.indexOf(tag) != -1)
                {
                    form.add("1");
                    selected = true;
                }
                else
                    form.add("0");
            }

            form.add("status");
            form.add("");

            if (!selected)
                return null;
        }

        return form;
    }

    // pure HTML scraping

    public static List<String> scrapeImageUrls(Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        List<String> urls = new ArrayList<String>();

        for (Element script : doc.select("script")) {
            String code = script.html();
            int start = 0, end = 0;

            for (;;)
            {
                start = code.indexOf("lstImages.push(\"", end);
                if (start == -1)
                    break;
                end = code.indexOf('"', start + 16);
                if (end == -1)
                    break;

                urls.add(code.substring(start + 16, end));
            }
        }

        return urls;
    }

    public static HtmlScrape.SearchResultPage scrapeSearchRedirect(
            Downloader.DownloadDestination target) {
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();
        int slash = target.baseUrl.lastIndexOf("/");

        if (slash != -1) {
            urls.add(target.baseUrl);
            titles.add(target.baseUrl.substring(slash + 1).replace("-", " "));
        }

        HtmlScrape.SearchResultPage page = new HtmlScrape.SearchResultPage();

        page.urls = urls;
        page.titles = titles;
        page.pagingUrl = null;
        page.currentPage = 1;
        page.lastPage = 1;

        return page;
    }

    public static HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Elements mangas = doc.select("table.listing tr td:lt(1) a");
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();

        for (Element manga : mangas) {
            urls.add(manga.attr("abs:href"));
            titles.add(manga.text());
        }

        HtmlScrape.SearchResultPage page = new HtmlScrape.SearchResultPage();

        page.urls = urls;
        page.titles = titles;
        page.pagingUrl = null;
        page.currentPage = 1;
        page.lastPage = 1;

        return page;
    }

    private static boolean isNumber(char c) {
        return c == '.' || (c >= '0' && c <= '9');
    }

    private static boolean isSkip(char c) {
        return c == ' ' || c == ':';
    }

    public static HtmlScrape.ChapterPage scrapeMangaPage(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Element titleTag = doc.select("div#leftside div.barContent a.bigChar").first();
        Elements links = doc.select("table.listing tr td a");
        List<HtmlScrape.ChapterInfo> chapters =
            new ArrayList<HtmlScrape.ChapterInfo>();
        HtmlScrape.ChapterPage result = new HtmlScrape.ChapterPage(chapters);

        if (titleTag == null)
            return null;

        String titleString = titleTag.text().trim();

        for (Element link : links) {
            if (!link.hasAttr("href"))
                continue;

            String url = link.attr("abs:href");
            String descr = link.text().trim();

            if (!descr.startsWith(titleString))
                continue;

            int start = titleString.length(), end, restart, max = descr.length();

            while (start < max && isSkip(descr.charAt(start)))
                ++start;
            end = start;
            while (end < max && isNumber(descr.charAt(end)))
                ++end;
            restart = end;
            while (restart < max && isSkip(descr.charAt(restart)))
                ++restart;

            if (end - start < 2)
                continue;

            String indexS = descr.substring(start, end);
            String title = restart >= max ? "" : descr.substring(restart);
            HtmlScrape.ChapterInfo info = new HtmlScrape.ChapterInfo();

            info.title = title;
            info.url = url;
            info.index = (int) (Float.valueOf(indexS) * 100);

            // assumes chapters are listed in reverse order
            chapters.add(0, info);
        }

        for (Element para : doc.select("div.barContent p")) {
            Element label = para.select("span.info").first();

            if (label == null)
                continue;

            if (label.text().trim().equals("Summary:")) {
                Node text = para.nextSibling();
                if (text == null || !(text instanceof Element))
                    continue;

                String value = ((Element) text).text().trim();

                result.summary = value;
            } else if (label.text().trim().equals("Genres:")) {
                String value = para.text().substring(8).trim();

                result.genres = new ArrayList<String>();

                for (String genre : value.split(", "))
                    result.genres.add(genre);
            }
        }

        return result;
    }
}
