/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.res.Configuration;

import android.os.Bundle;

import android.view.View;

import android.support.v4.app.FragmentActivity;

import org.barbon.mangaget.fragments.ChapterList;

public class Chapters extends FragmentActivity {
    public static final String MANGA_ID = "mangaId";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // activity not needed in landscape mode
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
            finish();

            return;
        }

        setContentView(R.layout.chapters);

        ChapterList chapterList = (ChapterList)
            getSupportFragmentManager().findFragmentById(R.id.chapter_list);

        chapterList.loadChapterList(getIntent().getLongExtra(MANGA_ID, -1));
    }
}
