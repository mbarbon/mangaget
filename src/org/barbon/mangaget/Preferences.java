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

    // implementation

    private static SharedPreferences getPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
