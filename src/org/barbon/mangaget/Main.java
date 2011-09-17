/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.support.v4.app.FragmentActivity;

import org.barbon.mangaget.fragments.ChapterList;
import org.barbon.mangaget.fragments.MangaList;

public class Main extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        final MangaList mangaList = (MangaList)
            getSupportFragmentManager().findFragmentById(R.id.manga_list);
        final ChapterList chapterList = (ChapterList)
            getSupportFragmentManager().findFragmentById(R.id.chapter_list);

        if (   mangaList != null && chapterList != null
            && chapterList.isInLayout()) {
            // landscape
            mangaList.setOnMangaSelected(
                new MangaList.OnMangaSelected() {
                    public void onMangaSelected(long mangaId) {
                        chapterList.loadChapterList(mangaId);
                    }
                });
        }
        else {
            // portrait
            mangaList.setOnMangaSelected(
                new MangaList.OnMangaSelected() {
                    private Intent chapters =
                        new Intent(Main.this, Chapters.class);

                    public void onMangaSelected(long mangaId) {
                        chapters.putExtra(Chapters.MANGA_ID, mangaId);

                        startActivity(chapters);
                    }
                });
        }
    }
}
