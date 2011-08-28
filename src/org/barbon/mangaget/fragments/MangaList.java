/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.os.Bundle;
import android.os.Handler;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

public class MangaList extends ListFragment {
    private static final String SELECTED_ID = "mangaId";

    private SimpleCursorAdapter adapter;
    private OnMangaSelected onMangaSelected;
    private long currentSelection = -1;

    private Runnable notifySelection =
        new Runnable() {
            @Override
            public void run() {
                // using currentSelection could have implications if
                // the member changes while multiple notifications are
                // in progress; this should never be the case
                if (onMangaSelected != null)
                    onMangaSelected.onMangaSelected(currentSelection);
            }
        };

    public interface OnMangaSelected {
        public void onMangaSelected(long mangaId);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_manga));

        adapter = new SimpleCursorAdapter(
            getActivity(), R.layout.list_item, null,
            new String[] { DB.MANGA_TITLE },
            new int[] { R.id.item_text });
        setListAdapter(adapter);

        if (savedInstanceState != null)
            setSelectedId(savedInstanceState.getLong(SELECTED_ID), true);

        registerForContextMenu(getListView());
    }

    @Override
    public void onStart() {
        super.onStart();

        DB db = DB.getInstance(getActivity());

        adapter.changeCursor(db.getMangaList());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(SELECTED_ID, currentSelection);
    }

    // public interface

    public void setOnMangaSelected(OnMangaSelected listener) {
        onMangaSelected = listener;
    }

    // implementation

    private void setSelectedId(final long id, boolean delayed) {
        currentSelection = id;

        if (onMangaSelected == null)
            return;

        if (!delayed)
            onMangaSelected.onMangaSelected(currentSelection);
        else
            new Handler().post(notifySelection);
    }

    // event handlers

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        setSelectedId(id, false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();

        inflater.inflate(R.menu.manga_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.refresh:
            Download.startMangaUpdate(getActivity(), info.id);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
}
