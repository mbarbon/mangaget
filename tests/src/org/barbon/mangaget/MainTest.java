/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Activity;
import android.app.Instrumentation;

import android.content.Context;
import android.content.IntentFilter;

import android.content.pm.ActivityInfo;

import android.content.res.Configuration;

import android.database.Cursor;

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

    private void selectListAndMoveToTop() throws Exception {
        activity.runOnUiThread(
            new Runnable() {
                public void run() {
                    mangaList.getListView().requestFocus();
                }
            });

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        Thread.sleep(500);
    }

    private void selectCurrent() throws Exception {
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);

        getInstrumentation().waitForIdleSync();
        Thread.sleep(1000);  // blech
    }

    private void moveDown() {
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
    }

    private Activity reloadActivity() {
        IntentFilter filter = null;
        Instrumentation instr = getInstrumentation();
        Instrumentation.ActivityMonitor monitor =
            instr.addMonitor(filter, null, false);
        int orientation =
            activity.getResources().getConfiguration().orientation;
        int req_orientation;

        switch (orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            req_orientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            break;
        case Configuration.ORIENTATION_LANDSCAPE:
            req_orientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            break;
        default:
            throw new RuntimeException("Unknown orientation " + orientation);
        }

        activity.setRequestedOrientation(req_orientation);

        // wait for activity to reload
        Activity activity = monitor.waitForActivity();

        instr.waitForIdleSync();

        return activity;
    }

    public MainTest() {
        super("org.barbon.mangaget", Main.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Utils.setupTestEnvironment(this);
        Utils.setupTestDatabase(this);

        setActivityInitialTouchMode(false);

        refreshMembers();
    }

    public void testPreconditions() {
        assertEquals(2, mangaList.getListView().getCount());
        assertEquals(0, chapterList.getListView().getCount());
    }

    public void testMangaSelection() throws Throwable {
        selectListAndMoveToTop();

        // select first item
        selectCurrent();

        assertEquals(1, chapterList.getListView().getCount());

        // select second item
        moveDown();
        selectCurrent();

        assertEquals(2, chapterList.getListView().getCount());
    }

    public void testRestart() throws Throwable {
        selectListAndMoveToTop();

        // select second item
        moveDown();
        selectCurrent();

        // sanity check
        assertEquals(2, chapterList.getListView().getCount());

        // force reload
        setActivity(reloadActivity());
        refreshMembers();

        // check the activity restored correctly
        assertEquals(2, chapterList.getListView().getCount());
    }

    public void testMangaRefresh() throws Throwable {
        selectListAndMoveToTop();

        DB db = DB.getInstance(null);
        Context targetContext = getInstrumentation().getTargetContext();

        // select first item
        selectCurrent();

        // sanity check
        assertEquals(1, chapterList.getListView().getCount());

        // update manga
        Download.startMangaUpdate(targetContext, Utils.firstDummyManga);

        // wait until refresh completes
        for (;;) {
            Cursor cursor = db.getChapterList(Utils.firstDummyManga);
            int count = cursor.getCount();

            cursor.close();

            if (count != 1)
                break;

            Thread.sleep(500);
        }
        Thread.sleep(2000);  // blech

        // check chapter list has been refreshed
        assertEquals(29, chapterList.getListView().getCount());
    }
}
