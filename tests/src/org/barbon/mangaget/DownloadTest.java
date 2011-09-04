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
    }

    @Override
    public void tearDown() throws Exception {
        Context targetContext = getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, Download.class);

        targetContext.stopService(intent);

        super.tearDown();
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
}
