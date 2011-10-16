/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.mangareader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.List;

import org.barbon.mangaget.scrape.Downloader;
import org.barbon.mangaget.scrape.HtmlScrape;
import org.barbon.mangaget.scrape.Scraper;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;

public class MangareaderScraper {
    // scraper interface
    public static class Provider extends Scraper.Provider {
        @Override
        public String composeSearchUrl(String title) {
            try {
                return "http://www.mangareader.net/search/?w=" +
                    URLEncoder.encode(title, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
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
            return new HtmlScrape.ChapterPage(
                MangareaderScraper.scrapeMangaPage(target));
        }
    }

    // pure HTML scraping

    public static List<String> scrapeChapterPages(
            Downloader.DownloadDestination target) {
        Document doc;

        try {
            doc = Jsoup.parse(target.stream, target.encoding, target.baseUrl);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        Document doc;

        try {
            doc = Jsoup.parse(target.stream, target.encoding, target.baseUrl);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        Element img = doc.select("div#imgholder > a > img").first();

        if (!img.hasAttr("src"))
            return null;

        return img.attr("abs:src");
    }

    public static HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target) {
        Document doc;

        try {
            doc = Jsoup.parse(target.stream, target.encoding, target.baseUrl);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements mangas = doc.select("div.manga_name a");
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();

        for (Element manga : mangas) {
            urls.add(manga.attr("abs:href"));
            titles.add(manga.text());
        }

        Elements links = doc.select("div#sp > a");
        Element current = doc.select("div#sp > strong").first();
        int currentPage = -1;
        String pagingUrl = null;

        if (current != null)
            currentPage = Integer.valueOf(current.text());

        for (Element link : links) {
            String href = link.attr("abs:href");
            int index = href.lastIndexOf("&p=");

            if (index == -1)
                continue;

            pagingUrl = href.substring(0, index + 3) + "%d";
            break;
        }

        HtmlScrape.SearchResultPage page = new HtmlScrape.SearchResultPage();

        page.urls = urls;
        page.titles = titles;
        page.pagingUrl = pagingUrl;
        page.currentPage = currentPage;

        return page;
    }

    public static List<HtmlScrape.ChapterInfo> scrapeMangaPage(
            Downloader.DownloadDestination target) {
        Document doc;

        try {
            doc = Jsoup.parse(target.stream, target.encoding, target.baseUrl);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements links = doc.select("div#chapterlist a");
        List<HtmlScrape.ChapterInfo> chapters =
            new ArrayList<HtmlScrape.ChapterInfo>();

        for (Element link : links) {
            if (!link.hasAttr("href"))
                continue;

            String url = link.attr("abs:href");
            String title = link.text();
            HtmlScrape.ChapterInfo info = new HtmlScrape.ChapterInfo();

            info.title = title;
            info.url = url;

            chapters.add(info);
        }

        return chapters;
    }
}
