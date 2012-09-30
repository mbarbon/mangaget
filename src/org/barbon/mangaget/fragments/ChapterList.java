/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.content.ContentValues;
import android.content.Intent;

import android.database.Cursor;

import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.Notifier;
import org.barbon.mangaget.R;
import org.barbon.mangaget.Utils;

import org.barbon.mangaget.data.DB;

public abstract class ChapterList extends ListFragment {
    private static final StatusBinder VIEW_BINDER = new StatusBinder();

    protected SimpleCursorAdapter adapter;
    protected Notifier.DBNotificationAdapter listener;

    private static class StatusBinder
            implements SimpleCursorAdapter.ViewBinder {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int column) {
            if (view.getId() == R.id.chapter_downloaded)
                return bindImage((ImageView) view, cursor, column);
            if (view.getId() == R.id.chapter_progress)
                return bindProgress((ProgressBar) view, cursor, column);
            if (view.getId() == R.id.chapter_number)
                return bindNumber((TextView) view, cursor, column);

            return false;
        }

        private boolean bindImage(ImageView image, Cursor cursor, int column) {
            int status = cursor.getInt(column);

            if (status == DB.DOWNLOAD_COMPLETE)
                image.setImageResource(R.drawable.btn_check_buttonless_on);
            else if (status == DB.DOWNLOAD_STOPPED ||
                     status == DB.DOWNLOAD_DELETED)
                image.setImageResource(R.drawable.btn_check_buttonless_off);
            else if (status == DB.DOWNLOAD_REQUESTED)
                image.setImageResource(R.drawable.btn_circle_pressed);

            if (status != DB.DOWNLOAD_STARTED)
                image.setVisibility(View.VISIBLE);
            else
                image.setVisibility(View.INVISIBLE);

            return true;
        }

        private boolean bindProgress(ProgressBar progress, Cursor cursor,
                                    int column) {
            int status = cursor.getInt(column);

            if (status == DB.DOWNLOAD_STARTED)
                progress.setVisibility(View.VISIBLE);
            else
                progress.setVisibility(View.INVISIBLE);

            return true;
        }

        private boolean bindNumber(TextView text, Cursor cursor,
                                   int column) {
            text.setText(Utils.formatChapterNumber(cursor.getInt(column)));

            return true;
        }
    }

    // lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter = createAdapter();
        adapter.setViewBinder(VIEW_BINDER);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_chapter));
        setListAdapter(adapter);

        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();

        Notifier.getInstance().add(listener);

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();
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

    // event handlers

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showChapter(id);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
        case R.id.download_chapter:
            Download.startChapterDownload(getActivity(), info.id);
            return true;
        case R.id.stop_chapter_download:
            Download.stopChapterDownload(getActivity(), info.id);
            return true;
        case R.id.view_chapter:
            showChapter(info.id);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    // implementation

    protected abstract SimpleCursorAdapter createAdapter();

    private void showChapter(long chapterId) {
        DB db = DB.getInstance(getActivity());
        ContentValues chapter = db.getChapter(chapterId);

        // unobtrusive alert if chapter has not been downloaded
        if (chapter.getAsInteger(DB.DOWNLOAD_STATUS) != DB.DOWNLOAD_COMPLETE) {
            Toast.makeText(getActivity(), R.string.chapter_not_downloaded,
                           Toast.LENGTH_SHORT).show();

            return;
        }

        Intent view = Utils.viewChapterIntent(getActivity(), chapterId);

        getActivity().startActivity(view);
    }
}
