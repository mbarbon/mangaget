/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.tests;

import android.content.Context;

import android.os.AsyncTask;

import java.io.File;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import org.barbon.mangaget.scrape.Downloader;

public class DummyDownloader extends Downloader {
    private Map<String, Entry> entries = new HashMap<String, Entry>();
    private Context context;

    private class Entry {
        public String url;
        public int id;
        public String encoding;
    }

    private class DummyDownloadTask extends AsyncTask<String, Long, Boolean> {
        private OnDownloadProgress progressListener;
        private DownloadTarget downloadTarget;
        private CountingInputStream byteCounter;

        public DummyDownloadTask(OnDownloadProgress listener,
                                 DownloadTarget target) {
            progressListener = listener;
            downloadTarget = target;
        }

        @Override
        public void onPreExecute() {
            // nothing to do
        }

        public Boolean doInBackground(String... params) {
            System.out.println("Download for " + params[0]);

            publishProgress(0L);

            long totalSize = -1;

            try {
                Entry entry = entries.get(params[0]);
                InputStream content =
                    context.getResources().openRawResource(entry.id);

                // TODO set totalSize
                byteCounter = new CountingInputStream(content);

                downloadTarget.startDownload(byteCounter, entry.encoding,
                                             params[0]);
            }
            catch (Exception e) {
                progressListener.downloadCompleteBackground(false);

                return false;
            }

            try {
                for (;;) {
                    long size = downloadTarget.downloadChunk();

                    if (size == -1)
                        break;

                    publishProgress(1L, byteCounter.getCount(), totalSize);
                }

                downloadTarget.completeDownload();
            }
            catch(Exception e) {
                progressListener.downloadCompleteBackground(false);

                return false;
            }

            progressListener.downloadCompleteBackground(true);

            return true;
        }

        @Override
        public void onProgressUpdate(Long... values) {
            // TODO abort on exception
            if (values[0] == 0)
                progressListener.downloadStarted();
            else
                progressListener.downloadProgress(values[1], values[2]);
        }

        @Override
        public void onPostExecute(Boolean result) {
            progressListener.downloadComplete(result);
        }
    }

    public DummyDownloader(Context _context) {
        context = _context;
    }

    public void addUrl(String url, int id) {
        addUrl(url, id, null);
    }

    public void addUrl(String url, int id, String encoding) {
        Entry entry = new Entry();

        entry.url = url;
        entry.id = id;
        entry.encoding = encoding;

        entries.put(url, entry);
    }

    @Override
    public DownloadDestination requestDownload(
            String url, OnDownloadProgress listener, File path) {
        DownloadDestination destination = new DownloadDestination(path);
        DownloadTarget target = new FileDownloadTarget(destination);
        DummyDownloadTask task = new DummyDownloadTask(listener, target);

        executeLater(task, url);

        return destination;
    }

    @Override
    public DownloadDestination requestDownload(
            String url, OnDownloadProgress listener) {
        DownloadDestination destination = new DownloadDestination();
        DownloadTarget target = new StringDownloadTarget(destination);
        DummyDownloadTask task = new DummyDownloadTask(listener, target);

        executeLater(task, url);

        return destination;
    }

    private void executeLater(final DummyDownloadTask task, final String url) {
        new android.os.Handler().post(
            new Runnable() {
                @Override
                public void run() {
                    task.execute(url);
                }
            });
    }
}
