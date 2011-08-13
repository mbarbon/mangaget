/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.os.Bundle;

import android.widget.SimpleCursorAdapter;

import android.support.v4.app.ListFragment;

import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

public class ChapterList extends ListFragment {
    private SimpleCursorAdapter adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_chapter));

        adapter = new SimpleCursorAdapter(
            getActivity(), R.layout.chapter_item, null,
            new String[] { DB.CHAPTER_NUMBER, DB.CHAPTER_TITLE },
            new int[] { R.id.chapter_number, R.id.chapter_title });
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();
    }

    // public interface

    public void loadChapterList(long mangaId) {
        DB db = DB.getInstance(getActivity());

        adapter.changeCursor(db.getChapterList(mangaId));
    }
}
