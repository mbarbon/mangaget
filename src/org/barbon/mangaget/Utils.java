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

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import android.os.Environment;

import java.io.File;

import java.util.Formatter;
import java.util.List;

import org.barbon.mangaget.data.DB;

public class Utils {
    public static void deleteChapters(Context context, long mangaId) {
        DB db = DB.getInstance(context);
        ContentValues manga = db.getManga(mangaId);

        Cursor chapters = db.getChapterList(mangaId);
        int numberI = chapters.getColumnIndex(DB.CHAPTER_NUMBER);

        while (chapters.moveToNext()) {
            File file = Utils.getChapterFile(manga, chapters.getInt(numberI));

            if (file.exists())
                file.delete();
        }

        chapters.close();
    }

    public static int mangaChapterInfo(Context context, long mangaId, List<Integer> missing) {
        DB db = DB.getInstance(context);

        Cursor chapters = db.getChapterList(mangaId);
        int numberI = chapters.getColumnIndex(DB.CHAPTER_NUMBER);
        int last = 0;

        while (chapters.moveToNext()) {
            int number = chapters.getInt(numberI);

            // ignore "fractional" chapters, there is no way to check
            // whether they are missing
            if ((number / 100) == (last / 100))
                continue;

            if ((number / 100) != (last / 100) + 1) {
                missing.add((last - last % 100) + 100);
                missing.add((number - number % 100) - 100);
            }

            last = number;
        }

        chapters.close();

        return last;
    }

    public static String formatChapterNumber(int number) {
        int chap = number / 100, part = number % 100;

        if (part == 0)
            return String.valueOf(chap);
        else
            return String.valueOf(chap) + "." + String.valueOf(part);
    }

    public static String formatMissingChapters(List<Integer> missing) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < missing.size(); i += 2) {
            if (missing.get(i).equals(missing.get(i + 1))) {
                buffer.append(formatChapterNumber(missing.get(i)));
            } else {
                buffer.append(formatChapterNumber(missing.get(i)));
                buffer.append("-");
                buffer.append(formatChapterNumber(missing.get(i + 1)));
            }

            buffer.append(", ");
        }

        return buffer.substring(0, buffer.length() - 2).toString();
    }

    public static String getChapterPath(Context context, long chapterId) {
        DB db = DB.getInstance(context);
        ContentValues chapter = db.getChapter(chapterId);
        ContentValues manga = db.getManga(chapter.getAsLong(
                                              DB.CHAPTER_MANGA_ID));
        int number = chapter.getAsInteger(DB.CHAPTER_NUMBER);
        File fullPath = getChapterFile(manga, number);

        return fullPath.getAbsolutePath();
    }

    public static File getChapterFile(ContentValues manga,
                                      int chapter) {
        return getChapterFile(manga.getAsString(DB.MANGA_PATTERN),
                              chapter);
    }

    public static Intent viewChapterIntent(Context context, long chapterId) {
        Intent view = new Intent(Intent.ACTION_VIEW);
        Uri chapter = new Uri.Builder()
            .scheme("file")
            .path(Utils.getChapterPath(context, chapterId))
            .build();

        view.setComponent(ComponentName.unflattenFromString(
                              Preferences.getMangaViewer(context)));
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

            if (status != DB.DOWNLOAD_COMPLETE) {
                File path = getChapterFile(pattern, number);

                if (path != null && path.exists()) {
                    db.updateChapterStatus(id, DB.DOWNLOAD_COMPLETE);
                    Notifier.getInstance().notifyChapterUpdate(mangaId, id);
                }
            }
        }

        chapters.close();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager =
            (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo active = manager.getActiveNetworkInfo();

        return active != null && active.isConnected();
    }

    // internal

    private static String getChapterPath(ContentValues manga,
                                         int chapter) {
        return getChapterFile(manga, chapter).getAbsolutePath();
    }

    private static File getChapterFile(ContentValues manga,
                                       ContentValues chapter) {
        return getChapterFile(manga.getAsString(DB.MANGA_PATTERN),
                              chapter.getAsInteger(DB.CHAPTER_NUMBER));
    }

    private static File getChapterFile(String pattern, int chapter) {
        if (pattern.equals(""))
            return null;

        File externalStorage = Environment.getExternalStorageDirectory();
        String targetPath;

        // hack to handle subchapters (seem to be rare, so an hack is OK)
        if (chapter % 100 == 0)
        {
            targetPath = new Formatter()
                .format(pattern, chapter / 100)
                .toString();
        }
        else
        {
            // uses ~ so it's sorted after the . in .cbz
            pattern = pattern.replace("%02d", "%02d~%02d");
            pattern = pattern.replace("%03d", "%03d~%02d");

            targetPath = new Formatter()
                .format(pattern, chapter / 100, chapter % 100)
                .toString();
        }

        File fullPath = new File(externalStorage, targetPath);

        return fullPath;
    }
}
