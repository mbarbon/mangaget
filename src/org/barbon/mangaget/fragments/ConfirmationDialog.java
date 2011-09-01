/*
 * Copyright (c) Mattia Barbon <mattia@barbon.org>
 * distributed under the terms of the MIT license
 */

package org.barbon.mangaget.fragments;

import android.app.AlertDialog;
import android.app.Dialog;

import android.content.DialogInterface;

import android.os.Bundle;

import android.support.v4.app.DialogFragment;

public class ConfirmationDialog extends DialogFragment {
    private DialogInterface.OnClickListener positiveClick;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        Bundle args = getArguments();
        int title = args.getInt("title");
        int positive = args.getInt("positive");
        int negative = args.getInt("negative");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(title)
               .setPositiveButton(positive, null)
               .setNegativeButton(negative, null);

        AlertDialog dialog = builder.create();

        doSetPositiveClick(dialog);

        return dialog;
    }

    // public interface

    public static Bundle getDialogArguments(int title, int positive,
                                            int negative) {
        Bundle args = new Bundle();

        args.putInt("title", title);
        args.putInt("positive", positive);
        args.putInt("negative", negative);

        return args;
    }

    public void setPositiveClick(DialogInterface.OnClickListener click) {
        positiveClick = click;

        doSetPositiveClick((AlertDialog) getDialog());
    }

    // implementation

    private void doSetPositiveClick(AlertDialog dialog) {
        if (dialog == null)
            return;

        int positive = getArguments().getInt("positive");

        dialog.setButton(AlertDialog.BUTTON_POSITIVE,
                         getActivity().getResources().getString(positive),
                         positiveClick);
    }
}
