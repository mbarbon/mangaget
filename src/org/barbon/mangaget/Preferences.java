/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.Context;
import android.content.SharedPreferences;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.os.Bundle;

// TODO allow selecting file extension (and use good default)
public class Preferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    // public interface

    public static boolean getUseMangaSubdir(Context context) {
        return getPrefs(context).getBoolean("useMangaSubdir", true);
    }

    public static String getMangaViewer(Context context) {
        return getPrefs(context).getString("mangaViewer", null);
    }

    // implementation

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
