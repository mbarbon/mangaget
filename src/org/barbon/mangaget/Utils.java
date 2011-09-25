/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;

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
