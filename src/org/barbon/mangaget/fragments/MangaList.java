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

    public interface OnMangaSelected {
        public void onMangaSelected(long mangaId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SimpleCursorAdapter(
            getActivity(), R.layout.list_item, null,
            new String[] { DB.MANGA_TITLE },
            new int[] { R.id.item_text });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_manga));
        setListAdapter(adapter);

        if (savedInstanceState != null)
            setSelectedId(savedInstanceState.getLong(SELECTED_ID));

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        adapter.changeCursor(null);
    }

    // public interface

    public void setOnMangaSelected(OnMangaSelected listener) {
        onMangaSelected = listener;
    }

    // implementation

    private void setSelectedId(final long id) {
        currentSelection = id;

        if (onMangaSelected == null)
            return;

        onMangaSelected.onMangaSelected(id);
    }

    private void deleteManga(long id) {
        DB db = DB.getInstance(getActivity());

        db.deleteManga(id);

        // TODO notify changes around/stop downloads/etc.

        adapter.getCursor().requery();
    }

    // event handlers

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        setSelectedId(id);
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
        case R.id.delete:
            // TODO ask for confirmation before delete
            deleteManga(info.id);
            return true;
        case R.id.refresh:
            Download.startMangaUpdate(getActivity(), info.id);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }
}
