/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape;

import android.test.AndroidTestCase;
import android.test.UiThreadTest;
import android.test.InstrumentationTestCase;

import static org.barbon.mangaget.tests.Utils.networkConnected;

public class DownloaderTest extends InstrumentationTestCase {
    private Downloader downloader = new Downloader();

    private class Progress implements Downloader.OnDownloadProgress {
        public boolean started, complete, successful;
        public int callCount = -1, byteCount = -1;

        @Override
        public void downloadStarted() {
            started = true;
        }

        @Override
        public void downloadProgress(long downloaded, long total) {
            callCount += 1;
            byteCount = (int)downloaded;
        }

        @Override
        public void downloadComplete(boolean success) {
            successful = success;
            complete = true;
        }
    }

    public void testStringDownload() throws Throwable {
        if (!networkConnected(getInstrumentation().getContext()))
            return;

        final Progress progress = new Progress();

        class UiTask implements Runnable {
            Downloader.DownloadDestination destination;

            @Override
            public void run() {
                destination = downloader.requestDownload(
                    "http://barbon.org/web/", progress);
            }
        }

        UiTask uiTask = new UiTask();

        runTestOnUiThread(uiTask);

        while (!progress.complete)
            Thread.sleep(500);

        assertTrue(progress.started);
        assertTrue(progress.complete);
        assertTrue(progress.callCount > 0);
        assertTrue(progress.successful);
        assertEquals(5141, progress.byteCount);
    }
}
