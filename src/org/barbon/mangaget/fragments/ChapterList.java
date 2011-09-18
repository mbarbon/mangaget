/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.content.DialogInterface;
import android.content.Intent;

import android.database.Cursor;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.R;
import org.barbon.mangaget.Utils;

import org.barbon.mangaget.data.DB;

public class ChapterList extends ListFragment {
    private static final String SELECTED_ID = "mangaId";
    private static final StatusBinder VIEW_BINDER = new StatusBinder();

    private SimpleCursorAdapter adapter;
    private long currentManga = -1;
    private DownloadListener listener = new DownloadListener();
    private Download.ListenerManager manager =
        new Download.ListenerManager(listener);

    private class DownloadListener extends Download.ListenerAdapter {
        @Override
        public void onMangaUpdateComplete(long mangaId, boolean success) {
            if (!success || mangaId != currentManga)
                return;

            loadChapterList(mangaId);
        }
    }

    private static class StatusBinder
            implements SimpleCursorAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int column) {
            if (view.getId() != R.id.chapter_downloaded)
                return false;

            ImageView image = (ImageView) view;
            int status = cursor.getInt(column);

            if (status == DB.DOWNLOAD_COMPLETE)
                image.setImageResource(R.drawable.btn_check_buttonless_on);
            else if (status == DB.DOWNLOAD_STOPPED)
                image.setImageResource(R.drawable.btn_check_buttonless_off);
            else
                image.setImageResource(R.drawable.btn_circle_pressed);

            return true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SimpleCursorAdapter(
            getActivity(), R.layout.chapter_item, null,
            new String[] { DB.CHAPTER_NUMBER, DB.CHAPTER_TITLE,
                           DB.DOWNLOAD_STATUS },
            new int[] { R.id.chapter_number, R.id.chapter_title,
                        R.id.chapter_downloaded });
        adapter.setViewBinder(VIEW_BINDER);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null)
            loadChapterList(savedInstanceState.getLong(SELECTED_ID));

        setEmptyText(getString(R.string.no_chapter));
        setListAdapter(adapter);

        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();

        manager.connect(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(SELECTED_ID, currentManga);
    }

    @Override
    public void onPause() {
        super.onPause();

        manager.disconnect(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        adapter.changeCursor(null);
    }

    // public interface

    public void loadChapterList(long mangaId) {
        DB db = DB.getInstance(getActivity());

        currentManga = mangaId;
        adapter.changeCursor(db.getChapterList(mangaId));
    }

    // event handlers

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO handle the case when chapter has not been downloaded yet
        Intent view = Utils.viewChapterIntent(getActivity(), id);

        getActivity().startActivity(view);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        inflater.inflate(R.menu.chapters_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.download_chapter:
            Download.startChapterDownload(getActivity(), info.id);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
}
