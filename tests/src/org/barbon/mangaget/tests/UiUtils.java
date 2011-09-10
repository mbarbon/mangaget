package org.barbon.mangaget.tests;

import android.app.Activity;
import android.app.Instrumentation;

import android.content.IntentFilter;

import android.content.pm.ActivityInfo;

import android.content.res.Configuration;

import android.view.KeyEvent;

import android.widget.ListView;

public class UiUtils {
    private static Instrumentation instr;

    public static void setInstrumentation(Instrumentation instrumentation) {
        instr = instrumentation;
    }

    public static void sleep(long msec) {
        try {
            Thread.sleep(msec);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void selectListAndMoveToTop(final ListView list) {
        instr.runOnMainSync(
            new Runnable() {
                public void run() {
                    list.requestFocus();
                }
            });

        instr.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
        instr.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
        instr.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
        instr.waitForIdleSync();

        sleep(500);
    }

    public static void selectCurrent() {
        instr.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        instr.waitForIdleSync();

        sleep(1000);  // blech
    }

    public static void moveDown() {
        instr.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        instr.waitForIdleSync();
    }

    public static Activity reloadActivity(Activity activity) {
        IntentFilter filter = null;
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
        Activity new_activity = monitor.waitForActivity();

        instr.waitForIdleSync();

        return new_activity;
    }
}
