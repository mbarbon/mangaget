/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.database.Cursor;

import android.os.Bundle;

import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import android.support.v4.app.ListFragment;

import java.util.HashSet;
import java.util.Set;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.Notifier;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Scraper;

public class MangaList extends ListFragment {
    private static final String SELECTED_ID = "mangaId";

    private SimpleCursorAdapter adapter;
    private OnMangaSelected onMangaSelected;
    private long currentSelection = -1;
    private StatusBinder viewBinder = new StatusBinder();
    private MangaListener listener = new MangaListener();

    public interface OnMangaSelected {
        public void onMangaSelected(long mangaId);
    }

    private class MangaListener extends Notifier.OperationNotificationAdapter {
        @Override
        public void onMangaUpdateStarted(long mangaId) {
            viewBinder.setStatus(mangaId, true);
            adapter.getCursor().requery();
        }

        @Override
        public void onMangaUpdateComplete(long mangaId, boolean success) {
            viewBinder.setStatus(mangaId, false);
            adapter.getCursor().requery();
        }
    }

    private static class StatusBinder
            implements SimpleCursorAdapter.ViewBinder {
        private Set<Long> active = new HashSet<Long>();

        @Override
        public boolean setViewValue(View view, Cursor cursor, int column) {
            if (view.getId() == R.id.manga_progress)
                return bindProgress((ProgressBar) view, cursor, column);
            if (view.getId() == R.id.manga_provider)
                return bindProvider((TextView) view, cursor, column);

            return false;
        }

        public void setStatus(long mangaId, boolean started) {
            if (started)
                active.add(mangaId);
            else
                active.remove(mangaId);
        }

        private boolean bindProgress(ProgressBar progress, Cursor cursor,
                                    int column) {
            long id = cursor.getLong(column);

            if (active.contains(id))
                progress.setVisibility(View.VISIBLE);
            else
                progress.setVisibility(View.INVISIBLE);

            return true;
        }

        private boolean bindProvider(TextView view, Cursor cursor,
                                     int column) {
            String url = cursor.getString(column);

            view.setText(Scraper.getProviderName(url));

            return true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = new SimpleCursorAdapter(
            getActivity(), R.layout.manga_item, null,
            new String[] { DB.MANGA_TITLE, DB.ID, DB.MANGA_URL },
            new int[] { R.id.manga_title, R.id.manga_progress,
                        R.id.manga_provider });
        adapter.setViewBinder(viewBinder);
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

        Notifier.getInstance().add(listener);

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(SELECTED_ID, currentSelection);
    }

    @Override
    public void onPause() {
        super.onPause();

        Notifier.getInstance().remove(listener);
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

        if (id == -1)
            return;
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
