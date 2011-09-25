/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;

import android.database.Cursor;

import android.net.Uri;

import android.os.Environment;

import java.io.File;

import java.util.Formatter;

import org.barbon.mangaget.data.DB;

public class Utils {
    public static String getChapterPath(Context context, long chapterId) {
        DB db = DB.getInstance(context);
        ContentValues chapter = db.getChapter(chapterId);
        ContentValues manga = db.getManga(chapter.getAsLong(
                                              DB.CHAPTER_MANGA_ID));
        File fullPath = getChapterPath(manga, chapter);

        return fullPath.getAbsolutePath();
    }

    public static Intent viewChapterIntent(Context context, long chapterId) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        Uri chapter = new Uri.Builder()
            .scheme("file")
            .path(Utils.getChapterPath(context, chapterId))
            .build();

        // TODO avoid hardcoding PerfectViewer
        view.setComponent(
            ComponentName.unflattenFromString(
                "com.rookiestudio.perfectviewer/.TViewerMain"));
        view.setData(chapter);

        return view;
    }

    public static void updateChapterStatus(Context context, long mangaId) {
        DB db = DB.getInstance(context);
        String pattern = db.getManga(mangaId).getAsString(DB.MANGA_PATTERN);
        Cursor chapters = db.getChapterList(mangaId);
        int idI = chapters.getColumnIndex(DB.ID);
        int statusI = chapters.getColumnIndex(DB.DOWNLOAD_STATUS);
        int numberI = chapters.getColumnIndex(DB.CHAPTER_NUMBER);

        while (chapters.moveToNext()) {
            int status = chapters.getInt(statusI);
            int number = chapters.getInt(numberI);
            int id = chapters.getInt(idI);

            if (status != DB.DOWNLOAD_COMPLETE &&
                getChapterPath(pattern, number).exists()) {
                db.updateChapterStatus(id, DB.DOWNLOAD_COMPLETE);
                Notifier.getInstance().notifyChapterUpdate(mangaId, id);
            }
        }

        chapters.close();
    }

    // internal

    private static File getChapterPath(ContentValues manga,
                                       ContentValues chapter) {
        return getChapterPath(manga.getAsString(DB.MANGA_PATTERN),
                              chapter.getAsInteger(DB.CHAPTER_NUMBER));
    }

    private static File getChapterPath(String pattern, int chapter) {
        File externalStorage = Environment.getExternalStorageDirectory();
        String targetPath = new Formatter()
            .format(pattern, chapter)
            .toString();
        File fullPath = new File(externalStorage, targetPath);

        return fullPath;
    }
}
