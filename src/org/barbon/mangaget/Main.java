/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

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

        mangaList.setOnMangaSelected(
            new MangaList.OnMangaSelected() {
                public void onMangaSelected(long mangaId) {
                    chapterList.loadChapterList(mangaId);
                }
            });
    }
}
