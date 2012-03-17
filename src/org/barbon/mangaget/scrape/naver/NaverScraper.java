/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.naver;

import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.barbon.mangaget.scrape.Downloader;
import org.barbon.mangaget.scrape.HtmlScrape;
import org.barbon.mangaget.scrape.Scraper;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;

public class NaverScraper {
    // scraper interface
    public static class Provider extends Scraper.Provider {
        private static final String NAVER_URL = "http://comic.naver.com/";

        @Override
        public boolean canHandleUrl(String url) {
            return url.startsWith(NAVER_URL);
        }

        @Override
        public String getName() {
            return "Naver";
        }

        @Override
        public String composeSearchUrl(String title) {
            try {
                return "http://comic.naver.com/search.nhn?m=webtoon&keyword=" +
                    URLEncoder.encode(title, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<String> scrapeChapterPages(
                Downloader.DownloadDestination target) {
            // Naver has all chapter images in the same HTML page...
            List<String> urls = new ArrayList<String>(1);

            urls.add(target.baseUrl);

            return urls;
        }

        @Override
        public List<String> scrapeImageUrls(
                Downloader.DownloadDestination target) {
            return NaverScraper.scrapeImageUrls(target);
        }

        @Override
        public HtmlScrape.SearchResultPage scrapeSearchResults(
                Downloader.DownloadDestination target) {
            return NaverScraper.scrapeSearchResults(target);
        }

        @Override
        public HtmlScrape.ChapterPage scrapeMangaPage(
                Downloader.DownloadDestination target) {
            return NaverScraper.scrapeMangaPage(target);
        }
    }

    // pure HTML scraping

    public static List<String> scrapeImageUrls(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Elements imgs = doc.select("div.wt_viewer > img");
        List<String> urls = new ArrayList<String>();

        for (Element img : imgs) {
            if (!img.hasAttr("src"))
                continue;

            urls.add(img.attr("abs:src"));
        }

        return urls;
    }

    public static HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target) {
        Document doc = HtmlScrape.parseHtmlPage(target);
        Elements manhwas = doc.select("div.resultBox ul.resultList li " +
                                      " img + a");
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();

        for (Element manhwa : manhwas) {
            urls.add(manhwa.attr("abs:href"));
            titles.add(manhwa.text());
        }

        Elements links = doc.select("div.pagenavigation > a");
        Element curr = doc.select("div.pagenavigation > span.current").first();
        int currentPage = -1, lastPage = -1;
        String pagingUrl = null;

        if (curr != null)
            currentPage = Integer.valueOf(curr.text());

        for (Element link : links)
        {
            if (link.hasAttr("class"))
                continue;

            String href = link.attr("abs:href");
            int index = href.lastIndexOf("&page=");

            if (index == -1)
                continue;

            pagingUrl = href.substring(0, index + 6) + "%d";
            lastPage = Integer.valueOf(href.substring(index + 6));
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
        Elements links = doc.select("table.viewList td.title a");
        List<HtmlScrape.ChapterInfo> chapters =
            new ArrayList<HtmlScrape.ChapterInfo>();
        HtmlScrape.ChapterPage chapterPage =
            new HtmlScrape.ChapterPage(chapters);

        for (Element link : links) {
            String url = link.attr("abs:href");
            String title = link.text();
            HtmlScrape.ChapterInfo info = new HtmlScrape.ChapterInfo();

            info.title = title;
            info.url = url;

            chapters.add(info);
        }

        Collections.reverse(chapters);

        // navigation link
        Element next = doc.select("div.pagenavigation > a.next").first();

        if (next != null)
            // the "next" link goes to previous chapters...
            chapterPage.previousPage = next.attr("abs:href");

        return chapterPage;
    }
}
