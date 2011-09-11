/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape;

import android.content.Context;

import android.os.Environment;

import android.test.InstrumentationTestCase;

import java.io.File;

import java.util.List;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Downloader;

import org.barbon.mangaget.tests.DummyDownloader;
import org.barbon.mangaget.tests.R;
import org.barbon.mangaget.tests.Utils;

public class ScraperTest extends InstrumentationTestCase {
    private DB db;
    private Context testContext;
    private File tempDir;

    private class OperationProgress
            implements Scraper.OnOperationStatus {
        public boolean started, complete;

        @Override
        public void operationStarted() {
            started = true;
        }

        @Override
        public void operationComplete(boolean success) {
            complete = true;
        }
    }

    private class DownloadProgress
            implements Scraper.OnChapterDownloadProgress {
        public boolean started, progress, complete;

        @Override
        public void downloadStarted() {
            started = true;
        }

        @Override
        public void downloadProgress(int current, int total) {
            progress = true;
        }

        @Override
        public void downloadComplete(boolean success) {
            complete = true;
        }
    }

    private class SearchProgress
            implements Scraper.OnSearchResults {
        public boolean complete;

        @Override
        public void resultsUpdated() {
            complete = true;
        }
    }

    @Override
    public void setUp() throws Exception {
        Utils.setupTestAnimeaEnvironment(this);

        testContext = getInstrumentation().getContext()
            .createPackageContext("org.barbon.mangaget.tests", 0);
        db = DB.getInstance(null);

        // download directory
        File externalStorage = Environment.getExternalStorageDirectory();
        tempDir = new File(externalStorage, "MangaGetTest");
        tempDir.mkdir();
    }

    private long setUpTestManga() {
        return db.insertManga(
            "Title", "MangaGetTest/Dummy-%chapter%.cbz",
            "http://manga.animea.net/papillon-hana-to-chou.html");
    }

    private long setUpTestChapter(long mangaId) {
        return db.insertChapter(
            mangaId, 1, 45, "Chapter title",
            "http://manga.animea.net/papillon-hana-to-chou-chapter-1-page-1.html");
    }

    @Override
    public void tearDown() throws Exception {
    }

    public void testMangaUpdate() throws Throwable {
        final long mangaId = setUpTestManga();
        final Scraper scraper = Scraper.getInstance(testContext);
        final OperationProgress progress = new OperationProgress();

        class UiTask implements Runnable {
            @Override
            public void run() {
                scraper.updateManga(
                    mangaId, progress);
            }
        }

        UiTask uiTask = new UiTask();

        runTestOnUiThread(uiTask);

        while (!progress.complete)
            Thread.sleep(500);

        assertTrue(progress.started);
        assertTrue(progress.complete);

        // TODO test manga chapters have been updated
    }

    public void testFullDownload() throws Throwable {
        final long mangaId = setUpTestManga();
        final long chapterId = setUpTestChapter(mangaId);
        final String targetCbz = new File(tempDir, "Dummy-1.cbz")
            .getAbsolutePath();
        final String tempDirString = tempDir.getAbsolutePath();
        final Scraper scraper = Scraper.getInstance(testContext);
        final DownloadProgress progress = new DownloadProgress();

        class UiTask implements Runnable {
            @Override
            public void run() {
                scraper.downloadChapter(
                    chapterId, targetCbz, tempDirString, progress);
            }
        }

        UiTask uiTask = new UiTask();

        runTestOnUiThread(uiTask);

        while (!progress.complete)
            Thread.sleep(500);

        System.out.println("Temp dir: " + tempDirString);
        System.out.println("Target: " + targetCbz);

        assertTrue(progress.started);
        assertTrue(progress.progress);
        assertTrue(progress.complete);
        assertTrue(new File(targetCbz).exists());
    }

    // TODO test partial downloads
    //      page URLs already filled in
    //      all pages downloaded
    //      page marked downloaded but no file there

    public void testSearchPager() throws Throwable {
        final Scraper scraper = Scraper.getInstance(testContext);
        final SearchProgress progress = new SearchProgress();

        class UiTask implements Runnable {
            public Scraper.ResultPager pager;

            @Override
            public void run() {
                pager = scraper.searchManga("", progress);
                assertEquals(0, pager.getCount()); // start the download
            }
        }

        UiTask uiTask = new UiTask();

        runTestOnUiThread(uiTask);

        while (!progress.complete)
            Thread.sleep(500);

        assertTrue(progress.complete);
        assertEquals(48, uiTask.pager.getCount());
        assertEquals("2 Kaime no Hajimete no Koi",
                     uiTask.pager.getItem(0).title);
        assertEquals("2-kaime-no-hajimete-no-koi",
                     uiTask.pager.getItem(0).pattern);
    }
}
