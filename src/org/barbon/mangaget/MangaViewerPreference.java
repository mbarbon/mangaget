/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.Context;
import android.content.Intent;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import android.net.Uri;

import android.preference.ListPreference;

import android.util.AttributeSet;

import java.io.File;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MangaViewerPreference extends ListPreference {
    public MangaViewerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadActivityList();
    }

    public MangaViewerPreference(Context context) {
        super(context);
        loadActivityList();
    }

    @Override
    public CharSequence getSummary() {
        return getEntry();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();
    }

    @Override
    public void setValueIndex(int index) {
        super.setValueIndex(index);
        notifyChanged();
    }

    // implementation

    private void loadActivityList() {
        PackageManager manager = getPackageManager();

        List<ResolveInfo> activities = getMangaViewers();
        int count = activities.size();
        String values[] = new String[count];
        CharSequence descriptions[] = new CharSequence[count];

        for (int i = 0; i < count; ++i) {
            ActivityInfo info = activities.get(i).activityInfo;

            values[i] = info.packageName + "/" + info.name;
            descriptions[i] = info.applicationInfo.loadLabel(manager);
        }

        setEntries(descriptions);
        setEntryValues(values);
    }

    private static boolean activityEqual(ResolveInfo a, ResolveInfo b) {
        return a.activityInfo.packageName.equals(b.activityInfo.packageName) &&
            a.activityInfo.name.equals(b.activityInfo.name);
    }

    private static int activitySort(ResolveInfo a, ResolveInfo b) {
        return -a.activityInfo.name.compareToIgnoreCase(b.activityInfo.name);
    }

    private List<ResolveInfo> getMangaViewers() {
        List<ResolveInfo> all = new ArrayList<ResolveInfo>();
        List<ResolveInfo> result = new ArrayList<ResolveInfo>();

        all.addAll(getMangaViewers("zip", "application/zip"));
        all.addAll(getMangaViewers("zip", "application/x-cbz"));
        all.addAll(getMangaViewers("zip", "application/x-cbr"));
        all.addAll(getMangaViewers("cbz", "application/x-cbz"));
        all.addAll(getMangaViewers("cbz", "application/x-cbr"));

        Collections.sort(all, new Comparator<ResolveInfo>() {
                public int compare(ResolveInfo a, ResolveInfo b) {
                    return activitySort(a, b);
                }

                public boolean equals(Object object) {
                    return false;
                }
            });

        for (int i = 0; i < all.size(); ++i) {
            ResolveInfo c = all.get(i);
            boolean found = false;

            for (int j = 0; j < result.size(); ++j) {
                ResolveInfo p = result.get(j);

                if (activityEqual(p, c)) {
                    found = true;
                    break;
                }
            }

            if (!found)
                result.add(c);
        }

        return result;
    }

    private List<ResolveInfo> getMangaViewers(String extension, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_VIEW);

        intent.setDataAndType(
            Uri.fromFile(new File("manga." + extension)), mimeType);

        return getPackageManager().queryIntentActivities(intent, 0);
    }

    private PackageManager getPackageManager() {
        return getContext().getPackageManager();
    }
}
