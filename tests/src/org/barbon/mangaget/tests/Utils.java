package org.barbon.mangaget.tests;

import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class Utils {
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
}
