/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.Intent;

import android.os.Bundle;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.support.v4.app.FragmentTransaction;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.fragments.ChapterDownloadQueue;
import org.barbon.mangaget.fragments.MangaChapterList;
import org.barbon.mangaget.fragments.MangaList;

public class Main extends BaseFragmentActivity {
    private static final String MANGA_LIST = "manga_list";
    private static final String CHAPTER_LIST = "chapter_list";
    private static final String QUEUE_LIST = "queue_list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // needs to be flushed here otherwise the functions below
        // sees the wrong state
        clearBackStack();

        boolean isLandscape = findViewById(R.id.landscape_container) != null;

        if (isLandscape) {
            // landscape
            FragmentTransaction transaction = beginTransaction(false);
            MangaList mangaList = setMangaList(transaction, true);

            setChapterDownloadQueue(transaction, false);
            setChapterList(transaction, true);
            transaction.commit();

            mangaList.setOnMangaSelected(
                new MangaList.OnMangaSelected() {
                    public void onMangaSelected(long mangaId) {
                        popBackStack("chapters");
                        getChapterList().loadChapterList(mangaId);
                    }
                });
        } else {
            // portrait
            FragmentTransaction transaction = beginTransaction(false);
            MangaList mangaList = setMangaList(transaction, true);

            setChapterDownloadQueue(transaction, false);
            setChapterList(transaction, false);
            transaction.commit();

            mangaList.setOnMangaSelected(
                new MangaList.OnMangaSelected() {
                    public void onMangaSelected(long mangaId) {
                        MangaChapterList chapterList = pushChapterList();

                        chapterList.loadChapterList(mangaId);
                    }
                });
        }

        // needs to be flushed here otherwise the onMangaSelected
        // callback does not see the changes above
        getSupportFragmentManager().executePendingTransactions();

        // start listening for network connectivity changes and resume
        // pending downloads if any
        Download.initialize(this);

        // clean up temporary manga entries created during search
        DB.getInstance(this).deleteTemporaryManga();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onCreateOptionsMenu(menu))
            return false;

        MenuInflater inflater = new MenuInflater(this);

        inflater.inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu))
            return false;

        menu.findItem(R.id.download_queue).setVisible(
            getSupportFragmentManager().findFragmentByTag(QUEUE_LIST) == null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.preferences:
            startActivity(new Intent(this, Preferences.class));

            return true;
        case R.id.advanced_search:
            startActivity(new Intent(this, MangaAdvancedSearch.class));

            return true;
        case R.id.stop_all_downloads:
            Download.stopAllDownloads(this);

            return true;
        case R.id.download_all:
            Download.downloadAll(this);

            return true;
        case R.id.download_queue:
            displayDownloadQueue();

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // implementation

    private MangaChapterList pushChapterList() {
        FragmentTransaction transaction = beginTransaction(true, "chapters");
        MangaChapterList chapterList = setChapterList(transaction, true);

        setChapterDownloadQueue(transaction, false);
        setMangaList(transaction, false);
        transaction.commit();

        return chapterList;
    }

    private ChapterDownloadQueue pushChapterDownloadQueue(boolean hideManga) {
        FragmentTransaction transaction = beginTransaction(true, "queue");
        ChapterDownloadQueue queue = setChapterDownloadQueue(transaction, true);

        setChapterList(transaction, false);
        setMangaList(transaction, !hideManga);
        transaction.commit();

        return queue;
    }

    private MangaList setMangaList(
            FragmentTransaction transaction, boolean status) {
        return setFragment(MangaList.class, transaction,
                           R.id.manga_list, MANGA_LIST, status);
    }

    private MangaChapterList setChapterList(
            FragmentTransaction transaction, boolean status) {
        return setFragment(MangaChapterList.class, transaction,
                           R.id.chapter_list, CHAPTER_LIST, status);
    }

    private ChapterDownloadQueue setChapterDownloadQueue(
            FragmentTransaction transaction, boolean status) {
        return setFragment(ChapterDownloadQueue.class, transaction,
                           R.id.chapter_list, QUEUE_LIST, status);
    }

    private MangaList getMangaList() {
        return (MangaList) getSupportFragmentManager()
            .findFragmentByTag(MANGA_LIST);
    }

    private MangaChapterList getChapterList() {
        return (MangaChapterList) getSupportFragmentManager()
            .findFragmentByTag(CHAPTER_LIST);
    }

    private void displayDownloadQueue() {
        boolean isLandscape = findViewById(R.id.landscape_container) != null;

        if (isLandscape) {
            // landscape
            pushChapterDownloadQueue(false);
        } else {
            // portrait
            pushChapterDownloadQueue(true);
        }
    }
}
