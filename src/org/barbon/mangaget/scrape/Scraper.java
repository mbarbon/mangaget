/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape;

import android.content.ContentValues;
import android.content.Context;

import android.database.Cursor;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import org.barbon.mangaget.CBZFile;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Downloader;

import org.barbon.mangaget.scrape.animea.AnimeAScraper;

public class Scraper {
    private static final String ANIMEA_URL = "http://manga.animea.net/";
    private static final String MANGAREADER_URL =
        "http://www.mangareader.net/";

    public static final int PROVIDER_ANIMEA = 1;
    public static final int PROVIDER_MANGAREADER = 2;

    public static abstract class Provider {
        // URL manipulation
        public String composeMangaUrl(String url) { return url; }
        public abstract String composeSearchUrl(String title);

        // scraping
        public abstract List<String> scrapeChapterPages(
            Downloader.DownloadDestination target);
        public abstract String scrapeImageUrl(
            Downloader.DownloadDestination target);
        public abstract HtmlScrape.SearchResultPage scrapeSearchResults(
            Downloader.DownloadDestination target);
        public abstract List<HtmlScrape.ChapterInfo> scrapeMangaPage(
            Downloader.DownloadDestination target);
    }

    private static Scraper theInstance;

    private DB db;
    private Downloader downloader;

    public static void setInstance(Scraper instance) {
        theInstance = instance;
    }

    public static Scraper getInstance(Context context) {
        if (theInstance != null)
            return theInstance;

        return theInstance =
            new Scraper(DB.getInstance(context), Downloader.getInstance());
    }

    protected Scraper(DB _db, Downloader _downloader) {
        db = _db;
        downloader = _downloader;
    }

    // public interface

    public interface OnChapterDownloadProgress {
        public void downloadStarted();
        public void downloadProgress(int current, int total);
        public void downloadComplete(boolean success);
    }

    public interface OnOperationStatus {
        public void operationStarted();
        public void operationComplete(boolean success);
    }

    public interface OnSearchResults {
        public void resultsUpdated();
    }

    public ResultPager searchManga(String title, OnSearchResults listener) {
        // TODO add Mangareader provider
        Provider provider = new AnimeAScraper.Provider();
        ResultPager pager = new ResultPager(provider, title, listener);

        return pager;
    }

    public void updateManga(long mangaId, OnOperationStatus listener) {
        ContentValues manga = db.getManga(mangaId);
        MangaInfoDownload info = new MangaInfoDownload();

        info.id = mangaId;
        info.manga = manga;
        info.listener = listener != null ? listener : dummyStatusListener;

        MangaInfoUpdater mangaUpdater = new MangaInfoUpdater(info);

        mangaUpdater.start();
    }

    public void downloadChapter(long chapterId, String targetPath,
                                String tempDir,
                                OnChapterDownloadProgress listener) {
        ContentValues chapter = db.getChapter(chapterId);
        ChapterDownload download = new ChapterDownload();

        download.id = chapterId;
        download.chapter = chapter;
        download.listener =
            listener != null ? listener : dummyDownloadListener;
        download.tempDir = tempDir;
        download.targetPath = targetPath;

        // TODO notify when it really starts
        download.listener.downloadStarted();

        if (chapter.getAsInteger(DB.DOWNLOAD_STATUS) != DB.DOWNLOAD_COMPLETE)
            downloadPageListAndPages(download);
        else
            downloadPages(download);
    }

    public static class MangaInfo {
        public final String title;
        public final String pattern;
        public final String url;
        public final String provider;

        public MangaInfo(String _title, String _url) {
            title = _title;
            url = _url;
            if (title != null)
                pattern = title.replaceAll("[\\s\\W]+" ,"-").toLowerCase();
            else
                pattern = null;

            if (url != null) {
                // TODO use Enum
                if (url.startsWith(ANIMEA_URL))
                    provider = "AnimeA";
                else if (url.startsWith(MANGAREADER_URL))
                    provider = "MangaReader";
                else
                    provider = "";
            } else
                provider = "";
        }
    }

    public class ResultPager extends Downloader.OnDownloadProgressAdapter {
        private Downloader.DownloadDestination target;
        private String startUrl;
        private OnSearchResults listener;
        private List<MangaInfo> items;

        // TODO actually support paging throught long search results

        public ResultPager(Provider provider, String title,
                           OnSearchResults _listener) {
            startUrl = provider.composeSearchUrl(title);
            listener = _listener;
        }

        public boolean isEmpty() {
            return items == null;
        }

        public int getCount() {
            if (items != null)
                return items.size();

            startDownload();

            return 0;
        }

        public MangaInfo getItem(int index) {
            if (items != null)
                return items.get(index);

            startDownload();

            return EMPTY_ITEM;
        }

        @Override
        public void downloadCompleteBackground(boolean success) {
            super.downloadCompleteBackground(success);

            if (!success)
                return;

            HtmlScrape.SearchResultPage results =
                getProvider(target).scrapeSearchResults(target);

            items = new ArrayList<MangaInfo>();

            for (int i = 0; i < results.titles.size(); ++i)
                items.add(new MangaInfo(results.titles.get(i),
                                        results.urls.get(i)));
        }

        @Override
        public void downloadComplete(boolean success) {
            super.downloadComplete(success);

            target = null;

            listener.resultsUpdated();
        }

        private void startDownload() {
            if (target != null)
                return;

            target = downloader.requestDownload(startUrl, this);
        }
    }

    // implementation

    private static final OnChapterDownloadProgress dummyDownloadListener =
        new OnChapterDownloadProgress() {
            @Override
            public void downloadStarted() { }

            @Override
            public void downloadProgress(int current, int total) { }

            @Override
            public void downloadComplete(boolean success) { }
        };

    private static final OnOperationStatus dummyStatusListener =
        new OnOperationStatus() {
            @Override
            public void operationStarted() { }

            @Override
            public void operationComplete(boolean success) { }
        };

    private static class MangaInfoDownload {
        public long id;
        public ContentValues manga;
        public OnOperationStatus listener;
    }

    private static class ChapterDownload {
        public long id;
        public ContentValues chapter;
        public String targetPath, tempDir;
        public OnChapterDownloadProgress listener;
    }

    private static class PageDownload {
        public long id;
        public String url;
        public String imageUrl;
        public String targetPath;
        public int status;
    }

    private class MangaInfoUpdater
            extends Downloader.OnDownloadProgressAdapter {
        private MangaInfoDownload info;
        private Downloader.DownloadDestination target;

        public MangaInfoUpdater(MangaInfoDownload _info) {
            super();

            info = _info;
        }

        public void start() {
            String url = info.manga.getAsString(DB.MANGA_URL);
            target = downloader.requestDownload(
                getProvider(target).composeMangaUrl(url), this);
        }

        @Override
        public void downloadStarted() {
            info.listener.operationStarted();
        }

        @Override
        public void downloadCompleteBackground(boolean success) {
            super.downloadCompleteBackground(success);

            if (!success)
                return;

            List<HtmlScrape.ChapterInfo> chapters =
                getProvider(target).scrapeMangaPage(target);

            for (int i = 0; i < chapters.size(); ++i)
                db.insertOrUpdateChapter(info.id, i + 1, -1,
                                         chapters.get(i).title,
                                         chapters.get(i).url);
        }

        @Override
        public void downloadComplete(boolean success) {
            super.downloadComplete(success);

            info.listener.operationComplete(success);
        }
    }

    private class ChapterDownloader
            extends Downloader.OnDownloadProgressAdapter {
        private ChapterDownload download;
        private Downloader.DownloadDestination target;

        public ChapterDownloader(ChapterDownload info) {
            super();

            download = info;
        }

        public void start() {
            target = downloader.requestDownload(
                download.chapter.getAsString(DB.CHAPTER_URL), this);
        }

        @Override
        public void downloadStarted() {
            super.downloadStarted();

            db.updateChapterStatus(download.id, DB.DOWNLOAD_STARTED);
        }

        @Override
        public void downloadCompleteBackground(boolean success) {
            super.downloadCompleteBackground(success);

            if (!success)
                return;

            List<String> pageUrls =
                getProvider(target).scrapeChapterPages(target);

            int index = 0;
            for (String url : pageUrls)
                db.insertPage(download.id, index++, url, null,
                              DB.DOWNLOAD_REQUESTED);

            // TODO only mark as complete after packing the chapter
            //      (must fix check below)
            db.updateChapterStatus(download.id, DB.DOWNLOAD_COMPLETE);
        }

        @Override
        public void downloadComplete(boolean success) {
            super.downloadComplete(success);

            if (!success) {
                db.updateChapterStatus(download.id, DB.DOWNLOAD_REQUESTED);
                download.listener.downloadComplete(success);

                return;
            }

            downloadPages(download);
        }
    }

    private class PageDownloader {
        private class PageInfoDownloader
                extends Downloader.OnDownloadProgressAdapter {
            private PageDownload page;
            private Downloader.DownloadDestination target;

            public PageInfoDownloader(PageDownload _page) {
                page = _page;
            }

            public void start() {
                target = downloader.requestDownload(page.url, this);
            }

            @Override
            public void downloadStarted() {
                super.downloadStarted();

                db.updatePageStatus(page.id, DB.DOWNLOAD_STARTED);
            }

            @Override
            public void downloadCompleteBackground(boolean success) {
                super.downloadCompleteBackground(success);

                if (!success)
                    return;

                page.imageUrl = getProvider(target).scrapeImageUrl(target);
                db.updatePageImage(page.id, page.imageUrl);
            }

            @Override
            public void downloadComplete(boolean success) {
                super.downloadComplete(success);

                if (!success) {
                    db.updatePageStatus(page.id, DB.DOWNLOAD_REQUESTED);
                    download.listener.downloadComplete(success);

                    // TODO stop all downloads

                    return;
                }

                count -= 1;

                download.listener.downloadProgress(total - count, total);
                downloadStep();
            }
        }

        private class PageImageDownloader
                extends Downloader.OnDownloadProgressAdapter {
            private PageDownload page;

            public PageImageDownloader(PageDownload _page) {
                page = _page;
            }

            public void start() {
                downloader.requestDownload(page.imageUrl, this,
                                           new File(page.targetPath));
            }

            @Override
            public void downloadStarted() {
                super.downloadStarted();

                db.updatePageStatus(page.id, DB.DOWNLOAD_STARTED);
            }

            @Override
            public void downloadComplete(boolean success) {
                super.downloadComplete(success);

                if (!success) {
                    db.updatePageStatus(page.id, DB.DOWNLOAD_REQUESTED);
                    download.listener.downloadComplete(success);

                    // TODO stop all downloads

                    return;
                }

                page.status = DB.DOWNLOAD_COMPLETE;
                db.updatePageStatus(page.id, DB.DOWNLOAD_COMPLETE);
                count -= 1;

                // TODO rename the image according to image type
                //      using magic + content-type + (real) url

                if (count == 0)
                    downloadFinished();
                else {
                    download.listener.downloadProgress(total - count, total);

                    downloadStep();
                }
            }
        }

        private ChapterDownload download;
        private List<PageDownload> pages;
        private int count, total;

        public PageDownloader(ChapterDownload _download,
                              List<PageDownload> _pages) {
            download = _download;
            pages = _pages;
        }

        public void start() {
            for (PageDownload page : pages) {
                if (page.imageUrl == null)
                    total = count += 2;
                else if (   page.status != DB.DOWNLOAD_COMPLETE
                         || !new File(page.targetPath).exists())
                    total = count += 1;
            }

            // start download
            if (count == 0)
                downloadFinished();
            else {
                download.listener.downloadProgress(0, total);

                downloadStep();
            }
        }

        private void downloadStep() {
            for (PageDownload page : pages) {
                if (page.imageUrl == null) {
                    downloadPageInfo(page);

                    break;
                }
                else if (   page.status != DB.DOWNLOAD_COMPLETE
                         || !new File(page.targetPath).exists()) {
                    downloadPageImage(page);

                    break;
                }
            }
        }

        private void downloadPageInfo(PageDownload page) {
            PageInfoDownloader downloader = new PageInfoDownloader(page);

            downloader.start();
        }

        private void downloadPageImage(PageDownload page) {
            PageImageDownloader downloader = new PageImageDownloader(page);

            downloader.start();
        }

        private void downloadFinished() {
            createChapterArchive(download, pages);
            download.listener.downloadComplete(true);
        }
    }

    private static final MangaInfo EMPTY_ITEM = new MangaInfo(null, null);

    private void downloadPages(ChapterDownload download) {
        Cursor pageCursor = db.getPages(download.id);
        List<PageDownload> pageList = new ArrayList<PageDownload>();
        final int idI = pageCursor.getColumnIndex(DB.ID);
        final int urlI = pageCursor.getColumnIndex(DB.PAGE_URL);
        final int imageUrlI = pageCursor.getColumnIndex(DB.PAGE_IMAGE_URL);
        final int statusI = pageCursor.getColumnIndex(DB.DOWNLOAD_STATUS);

        while (pageCursor.moveToNext()) {
            PageDownload page = new PageDownload();

            page.id = pageCursor.getInt(idI);
            page.url = pageCursor.getString(urlI);
            page.imageUrl = pageCursor.getString(imageUrlI);
            page.status = pageCursor.getInt(statusI);

            String imgName =
                download.chapter.getAsString(DB.CHAPTER_MANGA_ID) + "-" +
                Long.toString(download.id) + "-" +
                Long.toString(page.id) + ".jpg";

            page.targetPath = new File(download.tempDir, imgName)
                .getAbsolutePath();

            pageList.add(page);
        }

        pageCursor.close();

        PageDownloader pages = new PageDownloader(download, pageList);

        pages.start();
    }

    private void downloadPageListAndPages(ChapterDownload download) {
        ChapterDownloader chapter = new ChapterDownloader(download);

        chapter.start();
    }

    private static void createChapterArchive(ChapterDownload chapter,
                                            List<PageDownload> pages) {
        List<String> paths = new ArrayList<String>();

        for (PageDownload page : pages)
            paths.add(page.targetPath);

        CBZFile.createFile(chapter.targetPath, paths);
    }

    private static Provider getProvider(
            Downloader.DownloadDestination target) {
        // TODO add Mangareader provider
        return new AnimeAScraper.Provider();
    }
}
