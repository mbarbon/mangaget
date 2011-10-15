/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape.naver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.io.IOException;

import org.barbon.mangaget.scrape.Downloader;
import org.barbon.mangaget.scrape.HtmlScrape;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jsoup.select.Elements;

public class NaverScraper {
    public static HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target) {
        Document doc;

        try {
            doc = Jsoup.parse(target.stream, target.encoding, target.baseUrl);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        Elements manhwas = doc.select("div.resultBox ul.resultList li " +
                                      " img + a");
        List<String> urls = new ArrayList<String>();
        List<String> titles = new ArrayList<String>();

        for (Element manhwa : manhwas) {
            urls.add(manhwa.attr("abs:href"));
            titles.add(manhwa.text());
        }

        Element next = doc.select("div.pagenavigation > a.next").first();
        Element curr = doc.select("div.pagenavigation > span.current").first();
        int currentPage = -1;
        String pagingUrl = null;

        if (curr != null)
            currentPage = Integer.valueOf(curr.text());

        if (next != null)
        {
            String href = next.attr("abs:href");
            int index = href.lastIndexOf("&page=");

            if (index != -1)
                pagingUrl = href.substring(0, index + 6) + "%d";
        }

        HtmlScrape.SearchResultPage page = new HtmlScrape.SearchResultPage();

        page.urls = urls;
        page.titles = titles;
        page.pagingUrl = pagingUrl;
        page.currentPage = currentPage;

        return page;
    }

    public static HtmlScrape.ChapterPage scrapeMangaPage(
            Downloader.DownloadDestination target) {
        Document doc;

        try {
            doc = Jsoup.parse(target.stream, target.encoding, target.baseUrl);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

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
