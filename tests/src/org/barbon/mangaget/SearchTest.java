/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.SearchManager;

import android.content.Context;
import android.content.Intent;

import android.test.ActivityInstrumentationTestCase2;

import android.widget.ListView;

import org.barbon.mangaget.tests.Utils;

public class SearchTest extends ActivityInstrumentationTestCase2<MangaSearch> {
    private MangaSearch activity;

    public SearchTest() {
        super("org.barbon.mangaget", MangaSearch.class);
    }

    public Intent searchIntent(String query) {
        Context targetContext = getInstrumentation().getTargetContext();
        Intent intent = new Intent(targetContext, MangaSearch.class);

        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);

        return intent;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Utils.setupTestEnvironment(this);
        Utils.setupTestDatabase(this);

        setActivityInitialTouchMode(false);
    }

    public void testSimpleSearch() throws Exception {
        setActivityIntent(searchIntent(""));
        activity = (MangaSearch) getActivity();
        ListView resultList = activity.getListView();

        while (resultList.getCount() == 0)
            Thread.sleep(500);

        assertEquals(48, resultList.getCount());
    }
}
