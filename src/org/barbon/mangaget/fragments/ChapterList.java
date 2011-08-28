/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.database.Cursor;

import android.os.Bundle;

import android.view.View;

import android.widget.SimpleCursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

public class ChapterList extends ListFragment {
    private static final StatusBinder VIEW_BINDER = new StatusBinder();
    private SimpleCursorAdapter adapter;

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
            else
                image.setImageResource(R.drawable.btn_check_buttonless_off);

            return true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_chapter));

        adapter = new SimpleCursorAdapter(
            getActivity(), R.layout.chapter_item, null,
            new String[] { DB.CHAPTER_NUMBER, DB.CHAPTER_TITLE,
                           DB.DOWNLOAD_STATUS },
            new int[] { R.id.chapter_number, R.id.chapter_title,
                        R.id.chapter_downloaded });
        adapter.setViewBinder(VIEW_BINDER);
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        adapter.changeCursor(null);
    }

    // public interface

    public void loadChapterList(long mangaId) {
        DB db = DB.getInstance(getActivity());

        adapter.changeCursor(db.getChapterList(mangaId));
    }

    // event handlers

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // TODO check if re-download and ask for confirmation
        Download.startChapterDownload(getActivity(), id);
    }
}
