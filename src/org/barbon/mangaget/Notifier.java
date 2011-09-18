/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class Notifier {
    private static Notifier theInstance;
    private Handler mainThread;

    List<DBNotification> dbSubscribers = new ArrayList<DBNotification>();
    List<OperationNotification> operationSubscribers =
        new ArrayList<OperationNotification>();

    // notification classes

    public interface DBNotification {
        public void onMangaUpdate(long mangaId);
        public void onChapterListUpdate(long mangaId);
        public void onChapterUpdate(long mangaId, long chapterId);
    }

    public static class DBNotificationAdapter implements DBNotification {
        @Override public void onMangaUpdate(long mangaId) { }
        @Override public void onChapterListUpdate(long mangaId) { }
        @Override public void onChapterUpdate(long mangaId, long chapterId) { }
    }

    public interface OperationNotification {
        public void onMangaUpdateStarted(long mangaId);
        public void onMangaUpdateComplete(long mangaId, boolean success);
    }

    public static class OperationNotificationAdapter
            implements OperationNotification {
        @Override public void onMangaUpdateStarted(long mangaId) { }
        @Override public void onMangaUpdateComplete(long mangaId,
                                                    boolean success) { }
    }

    private Notifier() {
        // must be called in GUI thread
        mainThread = new Handler(Looper.getMainLooper());
    }

    // public interface

    public static Notifier getInstance() {
        if (theInstance != null)
            return theInstance;

        return theInstance = new Notifier();
    }

    public void add(DBNotification notification) {
        dbSubscribers.add(notification);
    }

    public void add(OperationNotification notification) {
        operationSubscribers.add(notification);
    }

    public void remove(DBNotification notification) {
        dbSubscribers.remove(notification);
    }

    public void remove(OperationNotification notification) {
        operationSubscribers.remove(notification);
    }

    // note: notifications are always handled in the main thread

    public void notifyMangaUpdate(final long mangaId) {
        if (isMainThread()) {
            for (DBNotification notification : dbSubscribers)
                notification.onMangaUpdate(mangaId);
        }
        else {
            mainThread.post(
                new Runnable() {
                    @Override
                    public void run() {
                        notifyMangaUpdate(mangaId);
                    }
                });
        }
    }

    public void notifyChapterListUpdate(final long mangaId) {
        if (isMainThread()) {
            for (DBNotification notification : dbSubscribers)
                notification.onChapterListUpdate(mangaId);
        }
        else {
            mainThread.post(
                new Runnable() {
                    @Override
                    public void run() {
                        notifyChapterListUpdate(mangaId);
                    }
                });
        }
    }

    public void notifyChapterUpdate(final long mangaId, final long chapterId) {
        if (isMainThread()) {
            for (DBNotification notification : dbSubscribers)
                notification.onChapterUpdate(mangaId, chapterId);
        }
        else {
            mainThread.post(
                new Runnable() {
                    @Override
                    public void run() {
                        notifyChapterUpdate(mangaId, chapterId);
                    }
                });
        }
    }

    public void notifyMangaUpdateStarted(final long mangaId) {
        if (isMainThread()) {
            for (OperationNotification notification : operationSubscribers)
                notification.onMangaUpdateStarted(mangaId);
        }
        else {
            mainThread.post(
                new Runnable() {
                    @Override
                    public void run() {
                        notifyMangaUpdateStarted(mangaId);
                    }
                });
        }
    }

    public void notifyMangaUpdateComplete(final long mangaId,
                                          final boolean success) {
        if (isMainThread()) {
            for (OperationNotification notification : operationSubscribers)
                notification.onMangaUpdateComplete(mangaId, success);
        }
        else {
            mainThread.post(
                new Runnable() {
                    @Override
                    public void run() {
                        notifyMangaUpdateComplete(mangaId, success);
                    }
                });
        }
    }

    // implementation

    private boolean isMainThread() {
        return Thread.currentThread() == mainThread.getLooper().getThread();
    }
}
