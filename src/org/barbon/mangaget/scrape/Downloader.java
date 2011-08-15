/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.scrape;

import android.os.AsyncTask;

import android.net.http.AndroidHttpClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.apache.http.client.HttpClient;

import org.apache.http.util.EntityUtils;

import org.apache.http.client.methods.HttpGet;

public class Downloader {
    protected static class CountingInputStream extends FilterInputStream {
        private long byteCount = 0;

        public CountingInputStream(InputStream in) {
            super(in);
        }

        public long getCount() {
            return byteCount;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int size = in.read(buffer);

            if (size != -1)
                byteCount += size;

            return size;
        }

        @Override
        public int read() throws IOException {
            int ch = in.read();

            if (ch != -1)
                byteCount += 1;

            return ch;
        }

        @Override
        public int read(byte[] buffer, int offset, int count)
                throws IOException {
            int size = in.read(buffer, offset, count);

            if (size != -1)
                byteCount += size;

            return size;
        }
    }

    public interface OnDownloadProgress {
        public void downloadStarted();
        public void downloadProgress(long downloaded, long total);
        public void downloadComplete(boolean success);
    }

    public interface DownloadTarget {
        public void startDownload(InputStream stream, String encoding)
            throws Exception;
        public long downloadChunk()
            throws Exception;
        public void completeDownload()
            throws Exception;
        public void abortDownload()
            throws Exception;
    }

    public static class OnDownloadProgressAdapter
            implements OnDownloadProgress {
        @Override public void downloadStarted() { }
        @Override public void downloadProgress(long downloaded, long total) { }
        @Override public void downloadComplete(boolean success) { }
    }

    public class StringDownloadTarget implements DownloadTarget {
        private static final int BUFFER_SIZE = 1024;
        private DownloadDestination destination;
        private InputStream in;
        private ByteArrayOutputStream out;
        private byte[] buffer = new byte[BUFFER_SIZE];

        public StringDownloadTarget(DownloadDestination _destination) {
            destination = _destination;
        }

        @Override
        public void startDownload(InputStream stream, String encoding) {
            destination.charSet = encoding;

            in = stream;
            out = new ByteArrayOutputStream();
        }

        @Override
        public long downloadChunk() throws IOException {
            int size = in.read(buffer);

            if (size != -1)
                out.write(buffer, 0, size);

            return size;
        }

        @Override
        public void completeDownload() {
            destination.stream = new ByteArrayInputStream(out.toByteArray());
        }

        @Override
        public void abortDownload() { }
    }

    public class FileDownloadTarget implements DownloadTarget {
        private static final int BUFFER_SIZE = 1024;
        private DownloadDestination destination;
        private InputStream in;
        private OutputStream out;
        private byte[] buffer = new byte[BUFFER_SIZE];

        public FileDownloadTarget(DownloadDestination _destination) {
            destination = _destination;
        }

        @Override
        public void startDownload(InputStream stream, String encoding)
                throws IOException {
            destination.charSet = encoding;

            in = stream;
            out = new FileOutputStream(destination.path);
        }

        @Override
        public long downloadChunk() throws IOException {
            int size = in.read(buffer);

            if (size != -1)
                out.write(buffer, 0, size);

            return size;
        }

        @Override
        public void completeDownload() throws IOException {
            out.close();
        }

        @Override
        public void abortDownload() throws IOException {
            out.close();
        }
    }

    private class DownloadTask extends AsyncTask<String, Long, Boolean> {
        private OnDownloadProgress progressListener;
        private DownloadTarget downloadTarget;
        private CountingInputStream byteCounter;

        public DownloadTask(OnDownloadProgress listener,
                            DownloadTarget target) {
            progressListener = listener;
            downloadTarget = target;
        }

        @Override
        public void onPreExecute() {
            // nothing to do
        }

        public Boolean doInBackground(String... params) {
            publishProgress(0L);

            AndroidHttpClient client =
                AndroidHttpClient.newInstance("MangaGet/1.0");
            long totalSize = -1;

            try {
                HttpResponse response = client.execute(new HttpGet(params[0]));
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();

                totalSize = entity.getContentLength();
                byteCounter = new CountingInputStream(content);

                downloadTarget.startDownload(
                    byteCounter, EntityUtils.getContentCharSet(entity));
            }
            catch (Exception e) {
                client.close();

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
                try {
                    downloadTarget.abortDownload();
                }
                catch (Exception e1) {
                    // can only ignore the exception...
                }

                client.close();

                return false;
            }

            client.close();

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

    public class DownloadDestination {
        public File path;
        public InputStream stream;
        public String encoding;
        public String baseUrl;

        public DownloadDestination() {
        }

        public DownloadDestination(File _path) {
            path = _path;
        }
    }

    public DownloadDestination requestDownload(
            String url, OnDownloadProgress listener, File path) {
        DownloadDestination destination = new DownloadDestination(path);
        DownloadTarget target = new FileDownloadTarget(destination);
        DownloadTask task = new DownloadTask(listener, target);

        task.execute(url);

        return destination;
    }

    public DownloadDestination requestDownload(
            String url, OnDownloadProgress listener) {
        DownloadDestination destination = new DownloadDestination();
        DownloadTarget target = new StringDownloadTarget(destination);
        DownloadTask task = new DownloadTask(listener, target);

        task.execute(url);

        return destination;
    }
}
