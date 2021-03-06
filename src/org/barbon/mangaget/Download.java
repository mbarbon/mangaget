/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.database.Cursor;

import android.net.ConnectivityManager;

import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;

import android.view.View;

import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Formatter;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Scraper;

public class Download extends Service {
    private static final int COMMAND_DOWNLOAD_CHAPTER = 1;
    private static final int COMMAND_STOP_DOWNLOAD_CHAPTER = 3;
    private static final int COMMAND_UPDATE_MANGA = 2;
    private static final int COMMAND_RESUME_DOWNLOADS = 4;
    private static final int COMMAND_DOWNLOAD_ALL_CHAPTERS = 5;
    private static final int COMMAND_SCAN_DOWNLOADED_FILES = 6;
    private static final int COMMAND_STOP_ALL_DOWNLOADS = 7;
    private static final int COMMAND_DOWNLOAD_ALL = 8;
    private static final int COMMAND_DELETE_MANGA = 9;

    private static final String COMMAND = "command";
    private static final String MANGA_ID = "mangaId";
    private static final String CHAPTER_ID = "chapterId";
    private static final String DELETE_CHAPTERS = "deleteChapters";

    // TODO make configurable
    private static final int MAX_CONCURRENT_DOWNLOADS = 3;

    private DB db;
    private File downloadTemp;
    private Map<Long, PendingTask> chapterDownloads =
        new HashMap<Long, PendingTask>();
    private List<Long> pendingDownloads = new ArrayList<Long>();
    private int operationCount;
    private MangaListener listener = new MangaListener();

    private static boolean initialized;

    private class DownloadBinder extends Binder {
        public Download getService() {
            return Download.this;
        }
    }

    private static class ConnectivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(
                ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

            if (noConnectivity)
                return;

            context.startService(resumeDownloadsIntent(context));
        }
    }

    public static class ServiceManager implements ServiceConnection {
        private Download service;

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

        // display toast when manga info update completes
        Notifier.getInstance().add(listener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // resume pending downloads on restart
        if (intent == null) {
            if (Utils.isNetworkConnected(this))
                resumeDownloads();

            // should never happen
            stopIfIdle();

            return START_STICKY;
        }

        int command = intent.getIntExtra(COMMAND, -1);

        switch (command) {
        case COMMAND_UPDATE_MANGA:
            updateManga(intent.getLongExtra(MANGA_ID, -1L), false);
            break;
        case COMMAND_DOWNLOAD_CHAPTER:
            downloadChapter(intent.getLongExtra(CHAPTER_ID, -1L));
            break;
        case COMMAND_DOWNLOAD_ALL_CHAPTERS:
            downloadAllChapters(intent.getLongExtra(MANGA_ID, -1L));
            break;
        case COMMAND_STOP_DOWNLOAD_CHAPTER:
            stopDownloadChapter(intent.getLongExtra(CHAPTER_ID, -1L));
            break;
        case COMMAND_STOP_ALL_DOWNLOADS:
            stopAllDownloads();
            break;
        case COMMAND_DOWNLOAD_ALL:
            downloadAll();
            break;
        case COMMAND_RESUME_DOWNLOADS:
            resumeDownloads();
            break;
        case COMMAND_SCAN_DOWNLOADED_FILES:
            scanDownloadedFiles(intent.getLongExtra(MANGA_ID, -1L));
            break;
        case COMMAND_DELETE_MANGA:
            deleteManga(intent.getLongExtra(MANGA_ID, -1L),
                        intent.getBooleanExtra(DELETE_CHAPTERS, false));
            break;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Notifier.getInstance().remove(listener);
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

    public static void startDownloadAllChapters(Context context, long mangaId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_DOWNLOAD_ALL_CHAPTERS);
        intent.putExtra(MANGA_ID, mangaId);

        context.startService(intent);
    }

    public static void stopChapterDownload(Context context, long chapterId) {
        context.startService(chapterStopDownloadIntent(context, chapterId));
    }

    public static void startMangaUpdate(Context context, long mangaId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_UPDATE_MANGA);
        intent.putExtra(MANGA_ID, mangaId);

        context.startService(intent);
    }

    public static void scanDownloadedFiles(Context context, long mangaId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_SCAN_DOWNLOADED_FILES);
        intent.putExtra(MANGA_ID, mangaId);

        context.startService(intent);
    }

    public static void stopAllDownloads(Context context) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_STOP_ALL_DOWNLOADS);

        context.startService(intent);
    }

    public static void downloadAll(Context context) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_DOWNLOAD_ALL);

        context.startService(intent);
    }

    public static void deleteManga(Context context, long mangaId, boolean deleteChapters) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_DELETE_MANGA);
        intent.putExtra(MANGA_ID, mangaId);
        intent.putExtra(DELETE_CHAPTERS, deleteChapters);

        context.startService(intent);
    }

    public static void initialize(Context context) {
        if (initialized)
            return;

        initialized = true;

        // listen for connectivity status changes
        IntentFilter filter =
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        context.registerReceiver(new ConnectivityReceiver(), filter);

        // resume pending dowloads
        if (Utils.isNetworkConnected(context))
            context.startService(resumeDownloadsIntent(context));
    }

    // implementation

    private static Intent chapterDownloadIntent(
            Context context, long chapterId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_DOWNLOAD_CHAPTER);
        intent.putExtra(CHAPTER_ID, chapterId);

        return intent;
    }

    private static Intent chapterStopDownloadIntent(
            Context context, long chapterId) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_STOP_DOWNLOAD_CHAPTER);
        intent.putExtra(CHAPTER_ID, chapterId);

        return intent;
    }

    private static Intent resumeDownloadsIntent(Context context) {
        Intent intent = new Intent(context, Download.class);

        intent.putExtra(COMMAND, COMMAND_RESUME_DOWNLOADS);

        return intent;
    }

    private class MangaListener extends Notifier.OperationNotificationAdapter {
        @Override
        public void onMangaUpdateComplete(long mangaId, boolean success) {
            int id;

            if (success)
                id = R.string.manga_info_update_complete_toast;
            else
                id = R.string.manga_info_update_error_toast;

            Toast.makeText(Download.this, id, Toast.LENGTH_SHORT).show();
        }
    }

    private class MangaUpdateProgress implements Scraper.OnOperationStatus {
        private long mangaId;
        private boolean autoDownload;

        public MangaUpdateProgress(long _mangaId, boolean _autoDownload) {
            mangaId = _mangaId;
            autoDownload = _autoDownload;
        }

        @Override
        public void operationStarted() {
            Notifier.getInstance().notifyMangaUpdateStarted(mangaId);
        }

        @Override
        public void operationComplete(boolean success) {
            Notifier.getInstance().notifyMangaUpdateComplete(mangaId, success);

            if (success) {
                // check already downloaded chapter before notifying
                Utils.updateChapterStatus(Download.this, mangaId);
                Notifier.getInstance().notifyChapterListUpdate(mangaId);

                if (autoDownload)
                    downloadAllChapters(mangaId);
            }

            --operationCount;

            stopIfIdle();
        }
    }

    private void updateManga(long mangaId, boolean autoDownload) {
        Scraper scraper = Scraper.getInstance(this);

        ++operationCount;

        scraper.updateManga(mangaId, new MangaUpdateProgress(mangaId, autoDownload));
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
            long chapterId = chapter.getAsLong(DB.ID);
            String ticker =
                getResources().getString(R.string.manga_downloading_ticker);
            Intent stopDownload = chapterStopDownloadIntent(
                Download.this, chapterId);

            manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
            notification = new Notification(R.drawable.stat_download_anim,
                                            ticker,
                                            System.currentTimeMillis());
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.contentView = contentView =
                new RemoteViews(getPackageName(), R.layout.download_progress);
            notification.contentIntent = PendingIntent.getService(
                Download.this, (int) chapterId, stopDownload,
                PendingIntent.FLAG_CANCEL_CURRENT);

            contentView.setTextViewText(
                R.id.download_description,
                formatMsg(R.string.manga_downloading_progress));
            contentView.setProgressBar(R.id.download_progress, 0, 0, true);

            manager.notify(chapterNotificationId(chapterId), notification);
        }

        @Override
        public void downloadProgress(int current, int total) {
            long chapterId = chapter.getAsLong(DB.ID);

            notification.iconLevel = current % 6;
            contentView.setProgressBar(
                R.id.download_progress, total, current, false);

            manager.notify(chapterNotificationId(chapterId), notification);
        }

        @Override
        public void downloadComplete(boolean success) {
            PendingTask task =
                chapterDownloads.remove(chapter.getAsLong(DB.ID));

            notification.iconLevel = 0;
            notification.flags &= ~Notification.FLAG_ONGOING_EVENT;

            int tickerId = -1, progressId = -1;
            long chapterId = chapter.getAsLong(DB.ID);

            if (success) {
                // on success, remove notification
                notification = null;
            }
            else {
                Intent startDownload = chapterDownloadIntent(
                    Download.this, chapterId);

                // re-download when fail notification clicked
                notification.contentIntent = PendingIntent.getService(
                    Download.this, (int) chapterId, startDownload,
                    PendingIntent.FLAG_CANCEL_CURRENT);
                if (task.isCancelled()) {
                    tickerId = R.string.manga_download_cancelled_ticker;
                    progressId = R.string.manga_download_cancelled_progress;
                }
                else {
                    tickerId = R.string.manga_download_error_ticker;
                    progressId = R.string.manga_download_error_progress;
                }
            }

            if (notification != null) {
                notification.tickerText = getResources().getString(tickerId);
                contentView.setTextViewText(
                    R.id.download_description,
                    formatMsg(progressId));
                contentView.setViewVisibility(
                    R.id.download_progress_parent, View.INVISIBLE);

                manager.notify(chapterNotificationId(chapterId), notification);
            }
            else
                manager.cancel(chapterNotificationId(chapterId));

            enqueueNextDownload();
            stopIfIdle();
        }

        // implementation

        private String formatMsg(int id) {
            int number = chapter.getAsInteger(DB.CHAPTER_NUMBER);
            String pattern = getResources().getString(id);
            String result = new Formatter()
                .format(pattern,
                        manga.getAsString(DB.MANGA_TITLE),
                        Utils.formatChapterNumber(number))
                .toString();

            return result;
        }
    }

    private void stopDownloadChapter(long chapterId) {
        updateChapterStatus(chapterId, DB.DOWNLOAD_STOPPED);
        if (pendingDownloads.contains(chapterId))
            pendingDownloads.remove(chapterId);
        if (chapterDownloads.containsKey(chapterId))
            chapterDownloads.get(chapterId).cancel();
    }

    private void downloadAllChapters(long mangaId) {
        Cursor chapters = db.getChapterList(mangaId);
        int statusI = chapters.getColumnIndex(DB.DOWNLOAD_STATUS);
        int idI = chapters.getColumnIndex(DB.ID);

        while (chapters.moveToNext()) {
            int status = chapters.getInt(statusI);
            long chapterId = chapters.getLong(idI);

            if (status != DB.DOWNLOAD_STOPPED)
                continue;

            downloadChapter(chapterId);
        }

        chapters.close();
    }

    private void stopAllChapters(long mangaId) {
        Cursor chapters = db.getChapterList(mangaId);
        int statusI = chapters.getColumnIndex(DB.DOWNLOAD_STATUS);
        int idI = chapters.getColumnIndex(DB.ID);

        while (chapters.moveToNext()) {
            int status = chapters.getInt(statusI);
            long chapterId = chapters.getLong(idI);

            if (status == DB.DOWNLOAD_STOPPED)
                continue;

            stopDownloadChapter(chapterId);
        }

        chapters.close();
    }

    private void downloadChapter(long chapterId) {
        if (chapterDownloads.containsKey(chapterId))
            return;
        if (pendingDownloads.contains(chapterId)) {
            // if the chapter is already enqueued, try to start the download
            enqueueNextDownload();

            return;
        }

        updateChapterStatus(chapterId, DB.DOWNLOAD_REQUESTED);
        pendingDownloads.add(chapterId);
        enqueueNextDownload();
    }

    private void enqueueNextDownload() {
        if (pendingDownloads.isEmpty())
            return;
        if (chapterDownloads.size() >= MAX_CONCURRENT_DOWNLOADS)
            return;
        if (!Utils.isNetworkConnected(this))
            return;

        long chapterId = pendingDownloads.remove(0);

        ContentValues chapter = db.getChapter(chapterId);
        ContentValues manga = db.getManga(chapter.getAsLong(
                                              DB.CHAPTER_MANGA_ID));
        Scraper scraper = Scraper.getInstance(this);
        File fullPath = Utils.getChapterFile(
            manga, chapter.getAsInteger(DB.CHAPTER_NUMBER));
        DownloadProgress progress = new DownloadProgress(manga, chapter);

        PendingTask task = scraper.downloadChapter(
            chapterId, fullPath.getAbsolutePath(),
            downloadTemp.getAbsolutePath(), progress);

        chapterDownloads.put(chapterId, task);
    }

    private void resumeDownloads() {
        Cursor chapters = db.getAllChapterList();
        int statusI = chapters.getColumnIndex(DB.DOWNLOAD_STATUS);
        int idI = chapters.getColumnIndex(DB.ID);

        while (chapters.moveToNext()) {
            int status = chapters.getInt(statusI);
            long chapterId = chapters.getLong(idI);

            if (status != DB.DOWNLOAD_REQUESTED &&
                    status != DB.DOWNLOAD_STARTED)
                continue;

            downloadChapter(chapterId);
        }

        chapters.close();
    }

    private void stopAllDownloads() {
        Cursor chapters = db.getAllChapterList();
        int idI = chapters.getColumnIndex(DB.ID);
        int statusI = chapters.getColumnIndex(DB.DOWNLOAD_STATUS);

        while (chapters.moveToNext()) {
            long chapterId = chapters.getLong(idI);
            int status = chapters.getInt(statusI);

            if (status == DB.DOWNLOAD_STARTED || status == DB.DOWNLOAD_REQUESTED)
                stopDownloadChapter(chapterId);
        }
    }

    private void downloadAll() {
        Cursor manga = db.getSubscribedMangaList();
        int idI = manga.getColumnIndex(DB.ID);
        int statusI = manga.getColumnIndex(DB.MANGA_SUBSCRIPTION_STATUS);

        while (manga.moveToNext()) {
            long mangaId = manga.getLong(idI);
            int status = manga.getInt(statusI);

            if (status == DB.SUBSCRIPTION_FOLLOWING)
                updateManga(mangaId, true);
        }
    }

    private boolean isOperationInProgress() {
        return (operationCount + chapterDownloads.size() +
                pendingDownloads.size()) > 0;
    }

    private void stopIfIdle() {
        if (!isOperationInProgress())
            stopSelf();
    }

    private static int chapterNotificationId(long chapterId) {
        return (int) chapterId;
    }

    private void updateChapterStatus(long chapterId, int status) {
        ContentValues chapter = db.getChapter(chapterId);

        if (chapter.getAsInteger(DB.DOWNLOAD_STATUS) == status)
            return;

        db.updateChapterStatus(chapterId, status);
        Notifier.getInstance().notifyChapterUpdate(
            chapter.getAsLong(DB.CHAPTER_MANGA_ID),
            chapterId);
    }

    private void scanDownloadedFiles(long mangaId) {
        ContentValues manga = db.getManga(mangaId);

        // handles temporary mangas
        if (manga.getAsString(DB.MANGA_PATTERN).equals(""))
            return;

        Cursor chapters = db.getChapterList(mangaId);
        boolean updated = false;
        int idI = chapters.getColumnIndex(DB.ID);
        int numberI = chapters.getColumnIndex(DB.CHAPTER_NUMBER);
        int statusI = chapters.getColumnIndex(DB.DOWNLOAD_STATUS);

        while (chapters.moveToNext()) {
            File file = Utils.getChapterFile(manga, chapters.getInt(numberI));
            long chapterId = chapters.getLong(idI);
            int status = chapters.getInt(statusI);

            if (file.exists() && status != DB.DOWNLOAD_COMPLETE) {
                db.updateChapterStatus(chapterId, DB.DOWNLOAD_COMPLETE,
                                       file.lastModified() / 1000);
                updated = true;
            } else if (!file.exists() && status == DB.DOWNLOAD_COMPLETE) {
                db.updateChapterStatus(chapterId, DB.DOWNLOAD_DELETED);
                updated = true;
            }
        }

        chapters.close();

        if (updated)
            Notifier.getInstance().notifyChapterListUpdate(mangaId);
    }

    class DeleteChapters extends AsyncTask<File, Void, Void>
    {
        private boolean deleteDir(File dir)
        {
            if (!dir.exists() || !dir.isDirectory())
                return false;

            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    if (!deleteDir(file))
                        return false;
                } else {
                    if (!file.delete())
                        return false;
                }
            }

            return dir.delete();
        }

        @Override
        public Void doInBackground(File... params) {
            deleteDir(params[0].getParentFile());

            return null;
        }
    }

    private void deleteManga(long mangaId, boolean deleteChapters) {
        DB db = DB.getInstance(this);
        ContentValues manga = db.getManga(mangaId);
        File path = Utils.getChapterFile(manga, 1);

        if (!db.updateMangaSubscription(mangaId, DB.SUBSCRIPTION_TEMPORARY))
            return;

        stopAllChapters(mangaId);
        Notifier.getInstance().notifyMangaUpdate(mangaId);

        if (path != null && deleteChapters)
        {
            DeleteChapters delete = new DeleteChapters();

            delete.execute(path);
        }
    }
}
