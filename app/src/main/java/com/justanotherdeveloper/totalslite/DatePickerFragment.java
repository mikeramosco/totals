package com.justanotherdeveloper.totalslite;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment {

    private int year, month, day;
    private Calendar min = null;
    private Calendar max = null;

    public DatePickerFragment(Calendar date) {
        year = date.get(Calendar.YEAR);
        month = date.get(Calendar.MONTH);
        day = date.get(Calendar.DAY_OF_MONTH);
    }

    public DatePickerFragment(Calendar date, Calendar min, Calendar max) {
        this(date);
        this.min = min;
        this.max = max;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DatePickerDialog dpd = new DatePickerDialog(requireActivity(), R.style.PickerDialogTheme,
                (DatePickerDialog.OnDateSetListener) getActivity(), year, month, day);

        if(min != null) dpd.getDatePicker().setMinDate(min.getTimeInMillis());
        if(max != null) dpd.getDatePicker().setMaxDate(max.getTimeInMillis());

        return dpd;
    }
}
