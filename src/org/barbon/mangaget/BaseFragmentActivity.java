/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class BaseFragmentActivity extends FragmentActivity {
    // implementation

    protected void clearBackStack() {
        FragmentManager manager = getSupportFragmentManager();

        // clear the backstack
        for (int i = manager.getBackStackEntryCount(); i != 0; --i)
            manager.popBackStack();

        manager.executePendingTransactions();
    }

    protected void popBackStack(String name) {
        FragmentManager manager = getSupportFragmentManager();

        // should be equivalent to manager.popBackStack(name, 0), but
        // for some reason the former does not work
        for (int i = manager.getBackStackEntryCount(); i != 0; --i) {
            if (name.equals(manager.getBackStackEntryAt(i - 1).getName()))
                break;

            manager.popBackStack();
        }

        manager.executePendingTransactions();
    }

    protected FragmentTransaction beginTransaction(boolean push) {
        return beginTransaction(push, null);
    }

    protected FragmentTransaction beginTransaction(boolean push, String name) {
        FragmentTransaction transaction =
            getSupportFragmentManager().beginTransaction();

        transaction.setTransition(FragmentTransaction.TRANSIT_NONE);

        if (push)
            transaction.addToBackStack(name);

        return transaction;
    }

    protected <F extends Fragment> F setFragment(Class<F> klass,
            FragmentTransaction transaction, int id,
            String tag, boolean status) {
        F fragment = klass.cast(getSupportFragmentManager()
                                .findFragmentByTag(tag));

        if ((fragment != null && fragment.isAdded()) == status)
            return fragment;

        if (status) {
            if (fragment == null) {
                try {
                    fragment = klass.newInstance();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                }
            }

            transaction.add(id, fragment, tag);
        }
        else
            transaction.remove(fragment);

        return fragment;
    }
}
