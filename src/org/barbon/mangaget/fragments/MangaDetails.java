/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.content.ContentValues;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import android.support.v4.app.Fragment;

import org.barbon.mangaget.Download;
import org.barbon.mangaget.Notifier;
import org.barbon.mangaget.R;

import org.barbon.mangaget.data.DB;

public class MangaDetails extends Fragment {
    private TextView summary, title, genres;
    private View first, progress, content;
    private long currentManga = -1;

    private MangaListener listener = new MangaListener();

    private class MangaListener extends Notifier.OperationNotificationAdapter {
        @Override
        public void onMangaUpdateStarted(long mangaId) {
            if (mangaId != currentManga)
                return;

            setInProgress(true);
        }

        @Override
        public void onMangaUpdateComplete(long mangaId, boolean success) {
            if (mangaId != currentManga)
                return;

            reload();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.manga_details, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        title = (TextView) view.findViewById(R.id.manga_title);
        summary = (TextView) view.findViewById(R.id.manga_summary);
        genres = (TextView) view.findViewById(R.id.manga_genres);
        first = view.findViewById(R.id.select_manga);
        progress = view.findViewById(R.id.manga_progress);
        content = view.findViewById(R.id.manga_details);

        // loadMangaDetails might have been called before onCreate
        if (currentManga != -1)
            loadMangaDetails(currentManga);
    }

    @Override
    public void onResume() {
        super.onResume();

        Notifier.getInstance().add(listener);
    }

    @Override
    public void onPause() {
        super.onPause();

        Notifier.getInstance().remove(listener);
    }

    public void loadMangaDetails(long mangaId) {
        currentManga = mangaId;

        // handle the case when loadMangaDetails is called right after creation
        if (title != null)
            reload();
    }

    public void loadMangaDetails(String title, String mangaUrl) {
        DB db = DB.getInstance(getActivity());
        long mangaId = db.findManga(mangaUrl);

        if (mangaId == -1)
            mangaId = db.insertManga(title, "", mangaUrl,
                                     DB.SUBSCRIPTION_TEMPORARY);

        loadMangaDetails(mangaId);
    }

    // implementation

    private void reload() {
        DB db = DB.getInstance(getActivity());
        ContentValues manga = db.getManga(currentManga);
        ContentValues metadata = db.getMangaMetadata(currentManga);

        title.setText(manga.getAsString(DB.MANGA_TITLE));

        if (metadata.size() != 0) {
            setInProgress(false);

            if (metadata.containsKey("genres"))
                genres.setText(metadata.getAsString("genres"));
            else
                genres.setText(R.string.not_available);

            if (metadata.containsKey("summary"))
                summary.setText(metadata.getAsString("summary"));
            else
                summary.setText(R.string.not_available);
        } else {
            Download.startMangaUpdate(getActivity(), currentManga);
        }
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            first.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
        } else {
            first.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
        }
    }
}