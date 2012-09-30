/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;

import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

import org.barbon.mangaget.Notifier;

import org.barbon.mangaget.data.DB;
import org.barbon.mangaget.R;

public class ChapterDownloadQueue extends ChapterList {
    private class ChapterListener extends Notifier.DBNotificationAdapter {
        @Override
        public void onChapterListUpdate(long mangaId) {
            adapter.getCursor().requery();
        }

        @Override
        public void onChapterUpdate(long mangaId, long chapterId) {
            adapter.getCursor().requery();
        }
    }

    // lifecycle

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        DB db = DB.getInstance(getActivity());

        listener = new ChapterListener();
        adapter.changeCursor(db.getChapterDownloadQueue());
    }

    // event handlers

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) menuInfo;

        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        inflater.inflate(R.menu.queue_context, menu);

        DB db = DB.getInstance(getActivity());
        int status = db.getChapter(info.id).getAsInteger(DB.DOWNLOAD_STATUS);

        if (status != DB.DOWNLOAD_COMPLETE)
            menu.removeItem(R.id.view_chapter);
        if (status != DB.DOWNLOAD_STOPPED && status != DB.DOWNLOAD_DELETED)
            menu.removeItem(R.id.download_chapter);
        if (status != DB.DOWNLOAD_REQUESTED && status != DB.DOWNLOAD_STARTED)
            menu.removeItem(R.id.stop_chapter_download);
    }


    // implementation

    @Override
    protected SimpleCursorAdapter createAdapter() {
        return new SimpleCursorAdapter(
            getActivity(), R.layout.chapter_queue_item, null,
            new String[] { DB.CHAPTER_NUMBER, DB.CHAPTER_TITLE,
                           DB.DOWNLOAD_STATUS, DB.DOWNLOAD_STATUS,
                           "manga_title"},
            new int[] { R.id.chapter_number, R.id.chapter_title,
                        R.id.chapter_downloaded, R.id.chapter_progress,
                        R.id.manga_title });
    }
}
