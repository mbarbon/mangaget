/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.os.Bundle;

import android.support.v4.app.Fragment;

import org.barbon.mangaget.R;

public class DeleteConfirmationDialog extends ConfirmationDialog
{
    private static final String TAG = "deleteConfirmationDialog";

    public static DeleteConfirmationDialog newInstance(long mangaId) {
        DeleteConfirmationDialog frag = new DeleteConfirmationDialog();
        Bundle args = getDialogArguments(R.string.delete_confirmation_title,
                                         R.string.delete_manga,
                                         R.string.cancel);

        args.putLong("mangaId", mangaId);

        frag.setArguments(args);

        return frag;
    }

    public static DeleteConfirmationDialog find(Fragment f) {
        return (DeleteConfirmationDialog)
            f.getFragmentManager().findFragmentByTag(TAG);
    }

    public void show(Fragment f) {
        show(f.getFragmentManager(), TAG);
    }

    public long getMangaId() {
        return getArguments().getLong("mangaId");
    }
}
