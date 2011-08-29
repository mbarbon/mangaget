package org.barbon.mangaget.tests;

import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.test.InstrumentationTestCase;

import org.barbon.mangaget.data.DB;

import org.barbon.mangaget.scrape.Downloader;

import org.barbon.mangaget.tests.DummyDownloader;

public class Utils {
    private static boolean setUp = false;

    public static boolean networkConnected(Context context) {
        ConnectivityManager manager =
            (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo active = manager.getActiveNetworkInfo();

        System.out.println("Network connected: " +
                           Boolean.toString(active != null &&
                                            active.isConnected()));

        return active != null && active.isConnected();
    }

    public static void setupTestEnvironment(InstrumentationTestCase test)
            throws Exception {
        Context targetContext = test.getInstrumentation().getTargetContext();

        if (setUp) {
            // scrub database
            SQLiteDatabase db = targetContext.openOrCreateDatabase(
                "manga_test", Context.MODE_PRIVATE, null);

            db.execSQL("DELETE FROM manga");

            return;
        }

        setUp = true;

        Context testContext = test.getInstrumentation().getContext()
            .createPackageContext("org.barbon.mangaget.tests", 0);
        DummyDownloader downloader = new DummyDownloader(testContext);

        final String baseUrl = "http://manga.animea.net";
        final String baseImage = "http://s2-a.animea-server.net";

        // set up dummy search results
        downloader.addUrl(baseUrl + "/search.html?title=",
                          R.raw.animea_results_html);

        // set up dummy pages
        String base = baseUrl + "/papillon-hana-to-chou-chapter-1-page-";

        downloader.addUrl(base + "1.html", R.raw.papillon_c1_p1_html);

        for (int i = 2; i < 46; ++i)
            downloader.addUrl(base + Integer.toString(i) + ".html",
                              R.raw.papillon_c1_p2_html);

        // set up dummy images
        String p1 = baseImage + "/5338%2F1_JHMCN%2F00_fuuchifighters.jpg" ;
        String pn = baseImage + "/5338%2F1_JHMCN%2F001_cover.jpg" ;

        downloader.addUrl(p1, R.raw.papillon_dummy_img);
        downloader.addUrl(pn, R.raw.papillon_dummy_img);

        // set up dummy database
        targetContext.deleteDatabase("manga_test");

        DB db = DB.getNewInstance(targetContext, "manga_test");

        DB.setInstance(db);
        Downloader.setInstance(downloader);
    }
}
