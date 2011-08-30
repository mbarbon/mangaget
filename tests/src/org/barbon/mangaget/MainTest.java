/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.test.ActivityInstrumentationTestCase2;

import android.support.v4.app.FragmentManager;

import android.view.KeyEvent;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.fragments.ChapterList;
import org.barbon.mangaget.fragments.MangaList;

import org.barbon.mangaget.tests.Utils;

// TODO add tests for Main activity and fragments,
//      MangaSearch activity and Download service
public class MainTest extends ActivityInstrumentationTestCase2<Main> {
    private MangaList mangaList;
    private ChapterList chapterList;
    private Main activity;
    private FragmentManager manager;

    private void refreshMembers() {
        activity = (Main) getActivity();
        manager = activity.getSupportFragmentManager();

        mangaList = (MangaList)
            manager.findFragmentById(R.id.manga_list);
        chapterList = (ChapterList)
            manager.findFragmentById(R.id.chapter_list);
    }

    public MainTest() {
        super("org.barbon.mangaget", Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Utils.setupTestEnvironment(this);

        // setup DB before getting the activity
        DB db = DB.getInstance(null);

        long m1 = db.insertManga(
            "Title1", "MangaGetTest/Dummy1-%chapter%.cbz",
            "http://manga.animea.net/dummy1.html");
        long m2 = db.insertManga(
            "Title2", "MangaGetTest/Dummy2-%chapter%.cbz",
            "http://manga.animea.net/dummy2.html");

        long c1 = db.insertChapter(
            m1, 1, 45, "Chapter 1",
            "http://manga.animea.net/dummy1-1.html");
        long c2 = db.insertChapter(
            m2, 1, 45, "Chapter 1",
            "http://manga.animea.net/dummy2-1.html");
        long c3 = db.insertChapter(
            m2, 2, 45, "Chapter 2",
            "http://manga.animea.net/dummy2-2.html");

        setActivityInitialTouchMode(false);

        refreshMembers();
    }

    public void testPreconditions() {
        assertEquals(2, mangaList.getListView().getCount());
        assertEquals(0, chapterList.getListView().getCount());
    }

    public void testMangaSelection() {
        activity.runOnUiThread(
            new Runnable() {
                public void run() {
                    mangaList.getListView().requestFocus();
                }
            });

        // select first item
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);

        getInstrumentation().waitForIdleSync();

        assertEquals(1, chapterList.getListView().getCount());

        // select second item
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);

        getInstrumentation().waitForIdleSync();

        assertEquals(2, chapterList.getListView().getCount());
    }
}
