/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.Notifier;

import org.barbon.mangaget.data.DB;
import org.barbon.mangaget.R;

public class MangaChapterList extends ChapterList {
    private static final String SELECTED_ID = "mangaId";

    private long currentManga = -1;

    private class ChapterListener extends Notifier.DBNotificationAdapter {
        @Override
        public void onChapterListUpdate(long mangaId) {
            if (mangaId != currentManga)
                return;

            adapter.getCursor().requery();
        }

        @Override
        public void onChapterUpdate(long mangaId, long chapterId) {
            if (mangaId != currentManga)
                return;

            adapter.getCursor().requery();
        }
    }

    // lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // loadChapterList might have been called before onCreate
        if (currentManga != -1 && getActivity() != null)
            loadChapterList(currentManga);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listener = new ChapterListener();

        if (savedInstanceState != null)
            loadChapterList(savedInstanceState.getLong(SELECTED_ID));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (currentManga != -1)
            loadChapterList(currentManga);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(SELECTED_ID, currentManga);
    }

    // public interface

    public void loadChapterList(long mangaId) {
        DB db = DB.getInstance(getActivity());

        currentManga = mangaId;

        // handle the case when loadChapterList is called right after creation
        if (adapter != null) {
            adapter.changeCursor(db.getChapterList(mangaId));

            Download.scanDownloadedFiles(getActivity(), currentManga);
        }
    }

    // event handlers

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) menuInfo;

        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        inflater.inflate(R.menu.chapters_context, menu);

        DB db = DB.getInstance(getActivity());
        int status = db.getChapter(info.id).getAsInteger(DB.DOWNLOAD_STATUS);

        if (status != DB.DOWNLOAD_COMPLETE)
            menu.removeItem(R.id.view_chapter);
        if (status != DB.DOWNLOAD_STOPPED && status != DB.DOWNLOAD_DELETED) {
            menu.removeItem(R.id.download_chapter);
            menu.removeItem(R.id.download_all_chapters);
        }
        if (status != DB.DOWNLOAD_REQUESTED && status != DB.DOWNLOAD_STARTED)
            menu.removeItem(R.id.stop_chapter_download);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.download_all_chapters:
            Download.startDownloadAllChapters(getActivity(), currentManga);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    // implementation

    @Override
    protected SimpleCursorAdapter createAdapter() {
        return new SimpleCursorAdapter(
            getActivity(), R.layout.chapter_item, null,
            new String[] { DB.CHAPTER_NUMBER, DB.CHAPTER_TITLE,
                           DB.DOWNLOAD_STATUS, DB.DOWNLOAD_STATUS },
            new int[] { R.id.chapter_number, R.id.chapter_title,
                        R.id.chapter_downloaded, R.id.chapter_progress });
    }
}
