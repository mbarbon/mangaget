/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.content.DialogInterface;

import android.database.Cursor;

import android.os.Bundle;

import android.view.View;

import android.widget.SimpleCursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

public class ChapterList extends ListFragment {
    private static final StatusBinder VIEW_BINDER = new StatusBinder();
    private SimpleCursorAdapter adapter;
    private DownloadListener listener = new DownloadListener();
    private Download.ListenerManager manager =
        new Download.ListenerManager(listener);

    public static class DownloadConfirmationDialog extends ConfirmationDialog {
        private static final String TAG = "downloadConfirmationDialog";

        public static DownloadConfirmationDialog newInstance(long chapterId) {
            DownloadConfirmationDialog frag = new DownloadConfirmationDialog();
            Bundle args = getDialogArguments(R.string.download_title,
                                             R.string.start_download,
                                             R.string.cancel);

            args.putLong("chapterId", chapterId);

            frag.setArguments(args);

            return frag;
        }

        public static DownloadConfirmationDialog find(Fragment f) {
            return (DownloadConfirmationDialog)
                f.getFragmentManager().findFragmentByTag(TAG);
        }

        public void show(Fragment f) {
            show(f.getFragmentManager(), TAG);
        }

        public long getChapterId() {
            return getArguments().getLong("chapterId");
        }
    }

    private class DownloadListener extends Download.ListenerAdapter {
        public long currentManga = -1;

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

            // TODO add another icon for the partially downloaded status
            if (status == DB.DOWNLOAD_COMPLETE)
                image.setImageResource(R.drawable.btn_check_buttonless_on);
            else
                image.setImageResource(R.drawable.btn_check_buttonless_off);

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

        setEmptyText(getString(R.string.no_chapter));
        setListAdapter(adapter);
        bindConfirmationDialog(DownloadConfirmationDialog.find(this));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter.getCursor() != null)
            adapter.getCursor().requery();

        manager.connect(getActivity());
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

        listener.currentManga = mangaId;
        adapter.changeCursor(db.getChapterList(mangaId));
    }

    // implementation

    private void bindConfirmationDialog(DownloadConfirmationDialog dialog) {
        if (dialog == null)
            return;

        final long chapterId = dialog.getChapterId();

        DialogInterface.OnClickListener download =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Download.startChapterDownload(getActivity(), chapterId);
                }
            };

        dialog.setPositiveClick(download);
    }

    // event handlers

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        DownloadConfirmationDialog frag =
            DownloadConfirmationDialog.newInstance(id);

        bindConfirmationDialog(frag);

        frag.show(this);
    }
}
