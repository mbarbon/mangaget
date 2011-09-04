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

import java.util.Formatter;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Downloader;

import org.barbon.mangaget.scrape.animea.Scraper;

public class Download extends Service {
    private static final int COMMAND_DOWNLOAD_CHAPTER = 1;
    private static final int COMMAND_UPDATE_MANGA = 2;

    private static final String COMMAND = "command";
    private static final String MANGA_ID = "mangaId";
    private static final String CHAPTER_ID = "chapterId";

    private DB db;
    private File downloadTemp;

    private class DownloadBinder extends Binder {
        public Download getService() {
            return Download.this;
        }
    }

    public static class ListenerManager implements ServiceConnection {
        private Download service;

        public ListenerManager() {
        }

        public void connect(Context context) {
            context.bindService(new Intent(context, Download.class), this, 0);
        }

        public void disconnect(Context context) {
            context.unbindService(this);
        }

        public Download getService() {
            return service;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            DownloadBinder downloadBinder = (DownloadBinder) binder;
            service = downloadBinder.getService();
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
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_DOWNLOAD_CHAPTER);
        intent.putExtra(CHAPTER_ID, chapterId);

        context.startService(intent);
    }

    public static void startMangaUpdate(Context context, long mangaId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_UPDATE_MANGA);
        intent.putExtra(MANGA_ID, mangaId);

        context.startService(intent);
    }

    // implementation

    private void updateManga(long mangaId) {
        Scraper scraper = Scraper.getInstance(this);

        scraper.updateManga(mangaId, null);
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

            Intent notificationIntent = new Intent(Intent.ACTION_MAIN);

            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            // TODO avoid hardcoding PerfectViewer
            notificationIntent.setComponent(
                ComponentName.unflattenFromString(
                    "com.rookiestudio.perfectviewer/.TStartup"));

            manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
            notification = new Notification(R.drawable.stat_download_anim,
                                            ticker,
                                            System.currentTimeMillis());
            notification.contentView = contentView =
                new RemoteViews(getPackageName(), R.layout.download_progress);
            notification.contentIntent =
               PendingIntent.getActivity(Download.this, 0,
                                         notificationIntent, 0);

            contentView.setTextViewText(
                R.id.download_description,
                formatMsg(R.string.manga_downloading_progress));
            contentView.setProgressBar(R.id.download_progress, 0, 0, true);

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
            notification.tickerText =
                getResources().getString(R.string.manga_downloaded_ticker);
            contentView.setTextViewText(
                R.id.download_description,
                formatMsg(R.string.manga_downloaded_progress));
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
}
