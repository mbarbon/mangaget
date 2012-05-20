/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Activity;
import android.app.Instrumentation;

import android.content.Context;
import android.content.DialogInterface;

import android.database.Cursor;

import android.test.ActivityInstrumentationTestCase2;

import android.support.v4.app.FragmentManager;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.fragments.ChapterList;
import org.barbon.mangaget.fragments.DeleteConfirmationDialog;
import org.barbon.mangaget.fragments.MangaList;

import org.barbon.mangaget.tests.UiUtils;
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

        UiUtils.setTestCase(this);
        Utils.setupTestAnimeaEnvironment(this);
        Utils.setupTestAnimeaDatabase(this);

        setActivityInitialTouchMode(false);

        setActivity(UiUtils.forceHorizontal(getActivity()));
        refreshMembers();
    }

    public void testPreconditions() {
        assertEquals(2, mangaList.getListView().getCount());
        assertEquals(0, chapterList.getListView().getCount());
    }

    public void testMangaSelection() {
        UiUtils.selectListAndMoveToTop(mangaList.getListView());

        // select first item
        UiUtils.selectCurrent();

        assertEquals(1, chapterList.getListView().getCount());

        // select second item
        UiUtils.moveDown();
        UiUtils.selectCurrent();

        assertEquals(2, chapterList.getListView().getCount());
    }

    // skip until I find a way of testing recreation without rotating
    public void skiptestRestart() {
        UiUtils.selectListAndMoveToTop(mangaList.getListView());

        // select second item
        UiUtils.moveDown();
        UiUtils.selectCurrent();

        // sanity check
        assertEquals(2, chapterList.getListView().getCount());

        // force reload
        setActivity(UiUtils.reloadActivity(activity));
        refreshMembers();

        // check the activity restored correctly
        assertEquals(2, chapterList.getListView().getCount());
    }

    public void testMangaRefresh() {
        UiUtils.selectListAndMoveToTop(mangaList.getListView());

        DB db = DB.getInstance(null);
        Context targetContext = getInstrumentation().getTargetContext();

        // select first item
        UiUtils.selectCurrent();

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

            UiUtils.sleep(500);
        }

        UiUtils.waitForIdle(2000);

        // check chapter list has been refreshed
        assertEquals(29, chapterList.getListView().getCount());
    }

    public void testRemoveManga() {
        Instrumentation instr = getInstrumentation();

        UiUtils.selectListAndMoveToTop(mangaList.getListView());
        UiUtils.moveDown();
        UiUtils.selectCurrent();

        // sanity check
        assertEquals(2, mangaList.getListView().getCount());

        assertTrue(instr.invokeContextMenuAction(activity, R.id.delete, 0));

        // click on confirmation dialog
        for (;;) {
            if (UiUtils.clickAlertDialog(
                    DeleteConfirmationDialog.find(mangaList),
                    DialogInterface.BUTTON_POSITIVE))
                break;

            UiUtils.sleep(500);
        }

        DB db = DB.getInstance(null);

        // wait until refresh completes
        for (;;) {
            Cursor cursor = db.getSubscribedMangaList();
            int count = cursor.getCount();

            cursor.close();

            if (count == 1)
                break;

            UiUtils.sleep(500);
        }

        UiUtils.waitForIdle(2000);

        // check chapter list has been refreshed
        assertEquals(1, mangaList.getListView().getCount());
    }
}
