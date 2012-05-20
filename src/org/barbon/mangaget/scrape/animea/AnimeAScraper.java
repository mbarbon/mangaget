/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.animea;

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

public class AnimeAScraper {
    private static final String[] SUPPORTED_TAGS = new String[] {
        "Adventure", "Comedy", "Doujinshi", "Drama", "Ecchi",
        "Fantasy", "Gender Bender", "Harem", "Historical", "Horror",
        "Josei", "Martial Arts", "Mature", "Mecha",
        "Mystery", "Psychological",
        "Romance", "School Life", "Sci-fi", "Seinen", "Shotacon",
        "Shoujo", "Shoujo Ai", "Shounen", "Shounen Ai", "Slice of Life",
        "Smut", "Sports", "Supernatural",
        "Tragedy", "Yaoi", "Yuri"
    };

    // scraper interface
    public static class Provider extends Scraper.Provider {
        private static final String ANIMEA_URL = "http://manga.animea.net/";

        @Override
        public boolean canHandleUrl(String url) {
            return url.startsWith(ANIMEA_URL);
        }

        @Override
        public String getName() {
            return "AnimeA";
        }

        @Override
        public String composeMangaUrl(String url) {
            // skip=1 is to skip the "Mature content" warning
            return url + "?skip=1";
        }

        @Override
        public String composeSearchUrl(Scraper.SearchCriteria criteria) {
            return AnimeAScraper.composeSearchUrl(criteria);
        }

        @Override
        public String composePagingUrl(String pagingUrl, int page) {
            return super.composePagingUrl(pagingUrl, page - 1);
        }

        @Override
        public List<String> scrapeChapterPages(
                Downloader.DownloadDestination target) {
            return AnimeAScraper.scrapeChapterPages(target);
        }

        @Override
        public List<String> scrapeImageUrls(
                Downloader.DownloadDestination target) {
            List<String> urls = new ArrayList<String>(1);

            urls.add(AnimeAScraper.scrapeImageUrl(target));

            return urls;
        }

        @Override
        public HtmlScrape.SearchResultPage scrapeSearchResults(
                Downloader.DownloadDestination target) {
            return AnimeAScraper.scrapeSearchResults(target);
        }

        @Override
        public HtmlScrape.ChapterPage scrapeMangaPage(
                Downloader.DownloadDestination target) {
            return AnimeAScraper.scrapeMangaPage(target);
        }
    }

    public static String composeSearchUrl(Scraper.SearchCriteria criteria) {
        String encodedTitle = "";
        StringBuffer genres = new StringBuffer();

        try {
            if (criteria.title != null)
                encodedTitle = URLEncoder.encode(criteria.title, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if (criteria.includeTags != null && criteria.includeTags.size() != 0) {
            boolean selected = false;

            for (String tag : SUPPORTED_TAGS) {
                if (criteria.includeTags.indexOf(tag) != -1) {
                    String encoded = tag.replace(' ', '_');

                    // &genre[<tag>]=1
                    genres.append("&genre%5B");
                    genres.append(encoded);
                    genres.append("%5D=1");

                    selected = true;
                }
            }

            if (!selected)
                return null;
        }

        return "http://manga.animea.net/search.html?title=" +
            encodedTitle + genres.toString();
    }

    // pure HTML scraping

    public static List<String> scrapeChapterPages(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Element page = doc.select("select[name=page]").first();

        if (!page.hasAttr("onchange"))
            return null;
        Elements options = page.select("option");
        String urlTemplate = page.attr("onchange");
        List<String> result = new ArrayList<String>();

        urlTemplate = urlTemplate.replaceFirst(
            "javascript:window.location='(.*\\.html).*", "$1");
        urlTemplate = HtmlScrape.absoluteUrl(urlTemplate, target.baseUrl);

        for (Element option : options) {
            if (!option.hasAttr("value"))
                continue;
            result.add(urlTemplate.replaceFirst(
                           "'\\s*\\+\\s*this\\.value\\s*\\+\\s*\\'",
                           option.attr("value")));
        }

        return result;
    }

    public static String scrapeImageUrl(Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Element img = doc.select("img.mangaimg").first();

        if (!img.hasAttr("src"))
            return null;

        return img.attr("abs:src");
    }

    public static HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Elements mangas = doc.select("a.manga_title");
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();

        for (Element manga : mangas) {
            urls.add(manga.attr("abs:href"));
            titles.add(manga.text());
        }

        Elements items = doc.select("div.pagingdiv > ul:not(.order) > li");
        int currentPage = -1, lastPage = -1;
        String pagingUrl = null;

        for (Element item : items) {
            String num = item.text();

            if (   num.equalsIgnoreCase("previous")
                || num.equalsIgnoreCase("next")
                || num.equalsIgnoreCase("..."))
                continue;

            if (   item.children().size() == 0
                && (   !item.hasAttr("class")
                    || !item.attr("class").equals("totalmanga"))) {
                if (currentPage == -1)
                    currentPage = Integer.valueOf(num);
            }
            else if (   item.children().size() == 1
                     && item.child(0).tag() == Tag.valueOf("a")) {
                Element link = item.child(0);
                String href = link.attr("abs:href");
                int index = href.lastIndexOf("&page=");

                if (index == -1)
                    continue;

                pagingUrl = href.substring(0, index + 6).replace("%", "%%") + "%d";
                lastPage = Integer.valueOf(href.substring(index + 6)) + 1;
            }
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
        Elements links = doc.select("ul.chapters_list li > a");
        List<HtmlScrape.ChapterInfo> chapters =
            new ArrayList<HtmlScrape.ChapterInfo>();
        HtmlScrape.ChapterPage result = new HtmlScrape.ChapterPage(chapters);

        for (Element link : links) {
            if (!link.hasAttr("href"))
                continue;

            String url = link.attr("abs:href");

            if (url.indexOf("-chapter-") == -1 || !url.endsWith(".html"))
                continue;
            int dash = url.lastIndexOf('-', url.length() - 13);
            String indexS = url.substring(dash + 1, url.length() - 12);

            int elementIndex = link.parent().childNodes().indexOf(link);
            Node text = link.parent().childNode(elementIndex + 1);

            if (!(text instanceof TextNode))
                continue;

            String title = ((TextNode)text).text().trim();
            HtmlScrape.ChapterInfo info = new HtmlScrape.ChapterInfo();

            info.title = title;
            info.url = url;

            // assumes chapters are listed in reverse order
            chapters.add(0, info);
        }

        Element container = doc.select("div.left_container").first();
        Element metadata = doc.select("div.right_container").first();

        if (container != null) {
            Element summary = container.select("p").get(1);

            result.summary = summary.text().trim();
        }

        if (metadata != null) {
            List<String> genres = result.genres = new ArrayList<String>();

            for (Element link : metadata.select("ul.manga_info li > a")) {
                if (!link.hasAttr("href"))
                    continue;

                String url = link.attr("abs:href");

                if (!url.startsWith("http://manga.animea.net/genre/"))
                    continue;

                genres.add(link.text().trim());
            }
        }

        return result;
    }
}
