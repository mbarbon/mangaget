/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.content.Intent;

import android.support.v4.app.FragmentTransaction;

import android.os.Bundle;

import org.barbon.mangaget.fragments.MangaDetails;
import org.barbon.mangaget.fragments.MangaSearchResults;

public class MangaSearch extends BaseFragmentActivity {
    private static final String SEARCH_RESULTS = "search_results";
    private static final String MANGA_DETAILS = "manga_details";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.manga_search);

        // needs to be flushed here otherwise the functions belog
        // sees the wrong state
        clearBackStack();

        boolean isLandscape = findViewById(R.id.landscape_container) != null;

        if (isLandscape) {
            // landscape
            FragmentTransaction transaction = beginTransaction(false);
            MangaSearchResults mangaList = setMangaSearchResults(transaction, true);

            setMangaDetails(transaction, true);
            transaction.commit();

            mangaList.setOnMangaSelected(
                new MangaSearchResults.OnMangaSelected() {
                    public void onMangaSelected(String title, String url) {
                        getMangaDetails().loadMangaDetails(title, url);
                    }
                });
        }
        else {
            // portrait
            FragmentTransaction transaction = beginTransaction(false);
            MangaSearchResults mangaList = setMangaSearchResults(transaction, true);

            setMangaDetails(transaction, false);
            transaction.commit();

            mangaList.setOnMangaSelected(
                new MangaSearchResults.OnMangaSelected() {
                    public void onMangaSelected(String title, String url) {
                        MangaDetails mangaDetails = pushMangaDetails();

                        mangaDetails.loadMangaDetails(title, url);
                    }
                });
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

    private MangaDetails pushMangaDetails() {
        FragmentTransaction transaction = beginTransaction(true);
        MangaDetails mangaDetails = setMangaDetails(transaction, true);

        setMangaSearchResults(transaction, false);
        transaction.commit();

        return mangaDetails;
    }

    private MangaSearchResults setMangaSearchResults(
            FragmentTransaction transaction, boolean status) {
        return setFragment(MangaSearchResults.class, transaction,
                           R.id.search_results, SEARCH_RESULTS, status);
    }

    private MangaDetails setMangaDetails(
            FragmentTransaction transaction, boolean status) {
        return setFragment(MangaDetails.class, transaction,
                           R.id.manga_details, MANGA_DETAILS, status);
    }

    private MangaSearchResults getMangaSearchResults() {
        return (MangaSearchResults) getSupportFragmentManager()
            .findFragmentByTag(SEARCH_RESULTS);
    }

    private MangaDetails getMangaDetails() {
        return (MangaDetails) getSupportFragmentManager()
            .findFragmentByTag(MANGA_DETAILS);
    }
}
