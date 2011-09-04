/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Instrumentation;

import android.content.Context;
import android.content.Intent;

import android.test.InstrumentationTestCase;

import org.barbon.mangaget.tests.Utils;

public class DownloadTest extends InstrumentationTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        Utils.setupTestEnvironment(this);
        Utils.setupTestDatabase(this);
    }

    @Override
    public void tearDown() throws Exception {
        Context targetContext = getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, Download.class);

        targetContext.stopService(intent);

        super.tearDown();
    }

    private static class DownloadListener implements Download.Listener {
        public long id;
        public boolean started, complete;
        public boolean status;

        @Override
        public void onMangaUpdateStarted(long mangaId) {
            started = true;
            id = mangaId;
        }

        @Override
        public void onMangaUpdateComplete(long mangaId,
                                          boolean success) {
            complete = true;
            status = success;
            id = mangaId;
        }
    }

    public void testListenerManager() throws Exception {
        Download.ListenerManager mgr = new Download.ListenerManager();
        Instrumentation instr = getInstrumentation();
        Context targetContext = instr.getTargetContext();

        mgr.connect(targetContext);
        instr.waitForIdleSync();

        Thread.sleep(1000);

        assertEquals(null, mgr.getService());

        System.out.println("Waiting for start");

        targetContext.startService(new Intent(targetContext, Download.class));
        instr.waitForIdleSync();

        while (mgr.getService() == null)
            Thread.sleep(500);

        System.out.println("Started");

        mgr.getService().stopSelf();
        instr.waitForIdleSync();

        System.out.println("Waiting for stop");

        while (mgr.getService() != null)
            Thread.sleep(500);

        System.out.println("Stopped");
    }

    public void testMangaRefresh() throws Exception {
        DownloadListener listener = new DownloadListener();
        Download.ListenerManager mgr = new Download.ListenerManager(listener);
        Instrumentation instr = getInstrumentation();
        Context targetContext = instr.getTargetContext();

        mgr.connect(targetContext);
        instr.waitForIdleSync();

        Download.startMangaUpdate(targetContext, Utils.firstDummyManga);

        System.out.println("Waiting");

        while (!listener.started) {
            assertFalse(listener.complete);
            Thread.sleep(500);
        }

        System.out.println("Started");

        assertEquals(Utils.firstDummyManga, listener.id);

        while (!listener.complete)
            Thread.sleep(500);

        System.out.println("Complete");

        assertEquals(Utils.firstDummyManga, listener.id);
        assertEquals(true, listener.status);

        mgr.disconnect(targetContext);
        instr.waitForIdleSync();
    }

    public void testMangaRefreshFail() throws Exception {
        DownloadListener listener = new DownloadListener();
        Download.ListenerManager mgr = new Download.ListenerManager(listener);
        Instrumentation instr = getInstrumentation();
        Context targetContext = instr.getTargetContext();

        mgr.connect(targetContext);
        instr.waitForIdleSync();

        Download.startMangaUpdate(targetContext, Utils.secondDummyManga);

        System.out.println("Waiting");

        while (!listener.complete)
            Thread.sleep(500);

        System.out.println("Complete");

        assertEquals(Utils.secondDummyManga, listener.id);
        assertEquals(false, listener.status);

        mgr.disconnect(targetContext);
        instr.waitForIdleSync();
    }
}
