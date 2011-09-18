/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class Utils {
    public static Intent viewChapterIntent(Context context, long chapterId) {
        Intent view = new Intent(Intent.ACTION_MAIN);

        view.addCategory(Intent.CATEGORY_LAUNCHER);
        // TODO avoid hardcoding PerfectViewer
        view.setComponent(
            ComponentName.unflattenFromString(
                "com.rookiestudio.perfectviewer/.TStartup"));

        return view;
    }
}
