/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.Intent;

import android.support.v4.app.FragmentTransaction;

import android.os.Bundle;

import org.barbon.mangaget.fragments.MangaSearchResults;

public class MangaSearch extends BaseFragmentActivity {
    private static final String SEARCH_RESULTS = "search_results";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manga_search);

        // needs to be flushed here otherwise the functions belog
        // sees the wrong state
        clearBackStack();

        boolean isLandscape = findViewById(R.id.landscape_container) != null;

        if (isLandscape) {
            // TODO landscape
        }
        else {
            // portrait
            FragmentTransaction transaction = beginTransaction(false);
            MangaSearchResults mangaList = setMangaSearchResults(transaction, true);

            transaction.commit();
        }

        // needs to be flushed here otherwise the onMangaSelected
        // callback does not see the changes above
        getSupportFragmentManager().executePendingTransactions();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    // implementation

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            getMangaSearchResults().performSearch(intent);
        }
    }

    // TODO display manga details on single click

    private MangaSearchResults setMangaSearchResults(
            FragmentTransaction transaction, boolean status) {
        return setFragment(MangaSearchResults.class, transaction,
                           R.id.search_results, SEARCH_RESULTS, status);
    }

    private MangaSearchResults getMangaSearchResults() {
        return (MangaSearchResults) getSupportFragmentManager()
            .findFragmentByTag(SEARCH_RESULTS);
    }
}
