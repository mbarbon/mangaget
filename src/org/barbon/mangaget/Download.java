/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Service;

import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;

import android.os.Environment;
import android.os.IBinder;

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
    private Downloader downloader;
    private File downloadTemp;

    @Override
    public void onCreate() {
        db = DB.getInstance(this);
        downloader = new Downloader();

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        Scraper scraper = new Scraper(db, downloader);

        scraper.updateManga(mangaId, null);
    }

    private void downloadChapter(long chapterId) {
        ContentValues chapter = db.getChapter(chapterId);
        ContentValues manga = db.getManga(chapter.getAsLong(
                                              DB.CHAPTER_MANGA_ID));
        Scraper scraper = new Scraper(db, downloader);
        File externalStorage = Environment.getExternalStorageDirectory();
        String targetPath = new  Formatter()
            .format(manga.getAsString(DB.MANGA_PATTERN),
                    chapter.getAsInteger(DB.CHAPTER_NUMBER))
            .toString();
        File fullPath = new File(externalStorage, targetPath);

        scraper.downloadChapter(chapterId, fullPath.getAbsolutePath(),
                                downloadTemp.getAbsolutePath(), null);
    }
}
