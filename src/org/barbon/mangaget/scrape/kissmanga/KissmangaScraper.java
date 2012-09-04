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
        public String composeSearchUrl(Scraper.SearchCriteria criteria) {
            return KissmangaScraper.composeSearchUrl(criteria);
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
            // Kissmanga has all chapter images in the same HTML page...
            List<String> urls = new ArrayList<String>(1);

            urls.add(target.baseUrl);

            return urls;
        }

        @Override
        public List<String> scrapeImageUrls(
                Downloader.DownloadDestination target) {
            return KissmangaScraper.scrapeImageUrls(target);
        }

        @Override
        public HtmlScrape.SearchResultPage scrapeSearchResults(
                Downloader.DownloadDestination target) {
            return KissmangaScraper.scrapeSearchResults(target);
        }

        @Override
        public HtmlScrape.ChapterPage scrapeMangaPage(
                Downloader.DownloadDestination target) {
            return KissmangaScraper.scrapeMangaPage(target);
        }
    }

    public static String composeSearchUrl(Scraper.SearchCriteria criteria) {
        String encodedTitle = "";

        try {
            if (criteria.title != null)
                encodedTitle = URLEncoder.encode(criteria.title, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if (criteria.includeTags != null && criteria.includeTags.size() != 0) {
            // advanced search requires an HTTP POST
            return null;
        }

        return "http://kissmanga.com/Search/Manga?keyword=" +
            encodedTitle;
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
