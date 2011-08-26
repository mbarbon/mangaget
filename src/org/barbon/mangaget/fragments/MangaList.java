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
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

public class MangaList extends ListFragment {
    private SimpleCursorAdapter adapter;
    private OnMangaSelected onMangaSelected;

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

    // public interface

    public void setOnMangaSelected(OnMangaSelected listener) {
        onMangaSelected = listener;
    }

    // event handlers

    @Override
    public void onListItemClick (ListView l, View v, int position, long id) {
        if (onMangaSelected != null)
            onMangaSelected.onMangaSelected(id);
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
