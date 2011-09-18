/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;

import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;

import android.view.View;

import android.widget.RemoteViews;

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Formatter;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Downloader;

import org.barbon.mangaget.scrape.Scraper;

// TODO handle download errors/missing network connection
// TODO allow stopping download

public class Download extends Service {
    private static final int COMMAND_DOWNLOAD_CHAPTER = 1;
    private static final int COMMAND_UPDATE_MANGA = 2;

    private static final String COMMAND = "command";
    private static final String MANGA_ID = "mangaId";
    private static final String CHAPTER_ID = "chapterId";

    private DB db;
    private File downloadTemp;
    private List<Listener> listeners = new ArrayList<Listener>();

    private class DownloadBinder extends Binder {
        public Download getService() {
            return Download.this;
        }
    }

    public interface Listener {
        public void onMangaUpdateStarted(long mangaId);
        public void onMangaUpdateComplete(long mangaId, boolean success);
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void onMangaUpdateStarted(long mangaId) { }

        @Override
        public void onMangaUpdateComplete(long mangaId, boolean success) { }
    }

    public static class ListenerManager implements ServiceConnection {
        private Listener listener;
        private Download service;

        public ListenerManager() {
            listener = null;
        }

        public ListenerManager(Listener _listener) {
            listener = _listener;
        }

        public void connect(Context context) {
            context.bindService(new Intent(context, Download.class), this, 0);
        }

        public void disconnect(Context context) {
            if (service != null && listener != null)
                service.removeListener(listener);

            context.unbindService(this);
        }

        public Download getService() {
            return service;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            DownloadBinder downloadBinder = (DownloadBinder) binder;
            service = downloadBinder.getService();

            if (listener != null)
                service.addListener(listener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    }

    @Override
    public void onCreate() {
        db = DB.getInstance(this);

        File externalStorage = Environment.getExternalStorageDirectory();

        downloadTemp = new File(externalStorage, "MangaGet");

        if (!downloadTemp.exists())
            downloadTemp.mkdir();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int command = intent.getIntExtra(COMMAND, -1);

        switch (command) {
        case COMMAND_UPDATE_MANGA:
            updateManga(intent.getLongExtra(MANGA_ID, -1L));
            break;
        case COMMAND_DOWNLOAD_CHAPTER:
            downloadChapter(intent.getLongExtra(CHAPTER_ID, -1L));
            break;
        }

        return START_STICKY;
    }

    private final IBinder binder = new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // public interface

    public static void startChapterDownload(Context context, long chapterId) {
        context.startService(chapterDownloadIntent(context, chapterId));
    }

    public static void startMangaUpdate(Context context, long mangaId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_UPDATE_MANGA);
        intent.putExtra(MANGA_ID, mangaId);

        context.startService(intent);
    }

    // implementation

    private static Intent chapterDownloadIntent(
            Context context, long chapterId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_DOWNLOAD_CHAPTER);
        intent.putExtra(CHAPTER_ID, chapterId);

        return intent;
    }

    private class MangaUpdateProgress implements Scraper.OnOperationStatus {
        private long mangaId;

        public MangaUpdateProgress(long _mangaId) {
            mangaId = _mangaId;
        }

        @Override
        public void operationStarted() {
            notifyMangaUpdateStarted(mangaId);
        }

        @Override
        public void operationComplete(boolean success) {
            notifyMangaUpdateComplete(mangaId, success);

            if (success)
                Notifier.getInstance().notifyChapterListUpdate(mangaId);
        }
    }

    private void updateManga(long mangaId) {
        Scraper scraper = Scraper.getInstance(this);

        scraper.updateManga(mangaId, new MangaUpdateProgress(mangaId));
    }

    private class DownloadProgress
            implements Scraper.OnChapterDownloadProgress {
        private Notification notification;
        private NotificationManager manager;
        private RemoteViews contentView;
        private ContentValues manga, chapter;

        public DownloadProgress(ContentValues _manga, ContentValues _chapter) {
            manga = _manga;
            chapter = _chapter;
        }

        @Override
        public void downloadStarted() {
            String ticker =
                getResources().getString(R.string.manga_downloading_ticker);
            Intent notificationIntent = Utils.viewChapterIntent(
                Download.this, chapter.getAsLong(DB.ID));

            manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
            notification = new Notification(R.drawable.stat_download_anim,
                                            ticker,
                                            System.currentTimeMillis());
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.contentView = contentView =
                new RemoteViews(getPackageName(), R.layout.download_progress);
            notification.contentIntent =
               PendingIntent.getActivity(Download.this, 0,
                                         notificationIntent, 0);

            contentView.setTextViewText(
                R.id.download_description,
                formatMsg(R.string.manga_downloading_progress));
            contentView.setProgressBar(R.id.download_progress, 0, 0, true);

            // TODO reuse the same notification id for chapter download
            manager.notify(notification.hashCode(), notification);
        }

        @Override
        public void downloadProgress(int current, int total) {
            notification.iconLevel = current % 6;
            contentView.setProgressBar(
                R.id.download_progress, total, current, false);

            manager.notify(notification.hashCode(), notification);
        }

        @Override
        public void downloadComplete(boolean success) {
            notification.iconLevel = 0;
            notification.flags &= ~Notification.FLAG_ONGOING_EVENT;

            int tickerId, progressId;

            if (success) {
                tickerId = R.string.manga_downloaded_ticker;
                progressId = R.string.manga_downloaded_progress;
            }
            else {
                Intent startDownload = chapterDownloadIntent(
                    Download.this, chapter.getAsLong(DB.ID));

                // re-download when fail notification clicked
                notification.contentIntent = PendingIntent.getService(
                    Download.this, 0, startDownload, 0);
                tickerId = R.string.manga_download_error_ticker;
                progressId = R.string.manga_download_error_progress;
            }

            notification.tickerText = getResources().getString(tickerId);
            contentView.setTextViewText(
                R.id.download_description,
                formatMsg(progressId));
            contentView.setViewVisibility(
                R.id.download_progress_parent, View.INVISIBLE);

            manager.notify(notification.hashCode(), notification);
        }

        // implementation

        private String formatMsg(int id) {
            String pattern = getResources().getString(id);
            String result = new Formatter()
                .format(pattern,
                        manga.getAsString(DB.MANGA_TITLE),
                        chapter.getAsInteger(DB.CHAPTER_NUMBER))
                .toString();

            return result;
        }
    }

    private void downloadChapter(long chapterId) {
        ContentValues chapter = db.getChapter(chapterId);
        ContentValues manga = db.getManga(chapter.getAsLong(
                                              DB.CHAPTER_MANGA_ID));
        Scraper scraper = Scraper.getInstance(this);
        File externalStorage = Environment.getExternalStorageDirectory();
        String targetPath = new Formatter()
            .format(manga.getAsString(DB.MANGA_PATTERN),
                    chapter.getAsInteger(DB.CHAPTER_NUMBER))
            .toString();
        File fullPath = new File(externalStorage, targetPath);
        DownloadProgress progress = new DownloadProgress(manga, chapter);

        scraper.downloadChapter(chapterId, fullPath.getAbsolutePath(),
                                downloadTemp.getAbsolutePath(), progress);
    }

    // notification management

    private void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void notifyMangaUpdateStarted(long mangaId) {
        for (Listener listener : listeners)
            listener.onMangaUpdateStarted(mangaId);
    }

    private void notifyMangaUpdateComplete(long mangaId, boolean success) {
        for (Listener listener : listeners)
            listener.onMangaUpdateComplete(mangaId, success);
    }
}
