package com.yoctopuce.examples.yscale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.util.Objects;

/**
 * Created by Yoctopuce on 30.04.2018.
 */
public class CountRefDialogFragment extends DialogFragment {


    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface CalibrateDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, long value, long maxValue);
    }

    // Use this instance of the interface to deliver action events
    private CalibrateDialogListener _listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            _listener = (CalibrateDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement CalibrateDialogListener");
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.fragement_set_ref, null);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.calibrate_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText ref = view.findViewById(R.id.ref_weight);
                        EditText max = view.findViewById(R.id.max_weight);
                        try {
                            long ref_val = Long.valueOf(ref.getText().toString());
                            long max_val = Long.valueOf(max.getText().toString());
                            _listener.onDialogPositiveClick(CountRefDialogFragment.this, ref_val, max_val);
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                });
        return builder.create();
    }
}
