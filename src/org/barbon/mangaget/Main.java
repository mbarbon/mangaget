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
import android.view.View;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import org.barbon.mangaget.fragments.ChapterList;
import org.barbon.mangaget.fragments.MangaList;

public class Main extends FragmentActivity {
    private static final String MANGA_LIST = "manga_list";
    private static final String CHAPTER_LIST = "chapter_list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // needs to be flushed here otherwise the functions belog
        // sees the wrong state
        clearBackStack();

        boolean isLandscape = findViewById(R.id.landscape_container) != null;

        if (isLandscape) {
            // landscape
            FragmentTransaction transaction = beginTransaction(false);
            MangaList mangaList = setMangaList(transaction, true);

            setChapterList(transaction, true);
            transaction.commit();

            mangaList.setOnMangaSelected(
                new MangaList.OnMangaSelected() {
                    public void onMangaSelected(long mangaId) {
                        getChapterList().loadChapterList(mangaId);
                    }
                });
        }
        else {
            // portrait
            FragmentTransaction transaction = beginTransaction(false);
            MangaList mangaList = setMangaList(transaction, true);

            setChapterList(transaction, false);
            transaction.commit();

            mangaList.setOnMangaSelected(
                new MangaList.OnMangaSelected() {
                    public void onMangaSelected(long mangaId) {
                        ChapterList chapterList = pushChapterList();

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.preferences:
            startActivity(new Intent(this, Preferences.class));

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    // implementation

    private ChapterList pushChapterList() {
        FragmentTransaction transaction = beginTransaction(true);
        ChapterList chapterList = setChapterList(transaction, true);

        setMangaList(transaction, false);
        transaction.commit();

        return chapterList;
    }

    private void clearBackStack() {
        FragmentManager manager = getSupportFragmentManager();

        // clear the backstack
        for (int i = manager.getBackStackEntryCount(); i != 0; --i)
            manager.popBackStack();

        manager.executePendingTransactions();
    }

    private FragmentTransaction beginTransaction(boolean push) {
        FragmentTransaction transaction =
            getSupportFragmentManager().beginTransaction();

        transaction.setTransition(FragmentTransaction.TRANSIT_NONE);

        if (push)
            transaction.addToBackStack(null);

        return transaction;
    }

    private <F extends Fragment> F setFragment(Class<F> klass,
            FragmentTransaction transaction, int id,
            String tag, boolean status) {
        F fragment = klass.cast(getSupportFragmentManager()
                                .findFragmentByTag(tag));

        if ((fragment != null && fragment.isAdded()) == status)
            return fragment;

        if (status) {
            if (fragment == null) {
                try {
                    fragment = klass.newInstance();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }

            transaction.add(id, fragment, tag);
        }
        else
            transaction.remove(fragment);

        return fragment;
    }

    private MangaList setMangaList(
            FragmentTransaction transaction, boolean status) {
        return setFragment(MangaList.class, transaction,
                           R.id.manga_list, MANGA_LIST, status);
    }

    private ChapterList setChapterList(
            FragmentTransaction transaction, boolean status) {
        return setFragment(ChapterList.class, transaction,
                           R.id.chapter_list, CHAPTER_LIST, status);
    }

    private MangaList getMangaList() {
        return (MangaList) getSupportFragmentManager()
            .findFragmentByTag(MANGA_LIST);
    }

    private ChapterList getChapterList() {
        return (ChapterList) getSupportFragmentManager()
            .findFragmentByTag(CHAPTER_LIST);
    }
}
