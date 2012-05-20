package org.barbon.mangaget.tests;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;

import android.content.IntentFilter;

import android.content.pm.ActivityInfo;

import android.content.res.Configuration;

import android.test.InstrumentationTestCase;
import android.test.TouchUtils;

import android.view.KeyEvent;

import android.widget.ListView;

import android.support.v4.app.DialogFragment;

public class UiUtils {
    private static Instrumentation instr;
    private static InstrumentationTestCase test;

    public static void setInstrumentation(Instrumentation instrumentation) {
        instr = instrumentation;
        test = null;
    }

    public static void setTestCase(InstrumentationTestCase testCase) {
        test = testCase;
        instr = testCase.getInstrumentation();
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

    public static void waitForIdle(int msec) {
        sleep(msec);

        instr.waitForIdleSync();
    }

    public static Activity reloadActivity(Activity activity) {
        int orientation =
            activity.getResources().getConfiguration().orientation;

        switch (orientation) {
        case Configuration.ORIENTATION_PORTRAIT:
            return forceOrientation(
                activity, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        case Configuration.ORIENTATION_LANDSCAPE:
            return forceOrientation(
                activity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        default:
            throw new RuntimeException("Unknown orientation " + orientation);
        }
    }

    public static Activity forceHorizontal(Activity activity) {
        int orientation =
            activity.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            return activity;

        return forceOrientation(
            activity, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public static Activity forceVertical(Activity activity) {
        int orientation =
            activity.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_PORTRAIT)
            return activity;

        return forceOrientation(
            activity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public static Activity forceOrientation(
            Activity activity, int orientation) {
        IntentFilter filter = null;
        Instrumentation.ActivityMonitor monitor =
            instr.addMonitor(filter, null, false);

        activity.setRequestedOrientation(orientation);

        // wait for activity to reload
        Activity new_activity = monitor.waitForActivity();

        instr.waitForIdleSync();

        return new_activity;
    }

    public static boolean clickAlertDialog(DialogFragment frag, int button) {
        if (frag == null || frag.getDialog() == null)
            return false;

        AlertDialog dialog = (AlertDialog) frag.getDialog();

        TouchUtils.clickView(test, dialog.getButton(button));

        return true;
    }
}
