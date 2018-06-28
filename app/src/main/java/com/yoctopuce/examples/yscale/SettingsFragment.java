package com.yoctopuce.examples.yscale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class SettingsFragment extends BasicScaleFragment
{

    private TextView _liveValue;
    private EditText _refWeightEditText;
    private double _liveVal;
    private double _weight;
    private long _count;
    private String _countUnit;
    private Button _tare;
    private Button _calibrate;
    private boolean _disable_onchange = false;
    //private boolean _devicePresent = false;


    public SettingsFragment()
    {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);
        final Activity activity = getActivity();

        if (activity != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
            final String weightStr = sharedPref.getString(getString(R.string.saved_ref_weight), "0.0");
            _weight = Double.valueOf(weightStr);
            _count = sharedPref.getLong(getString(R.string.saved_ref_count), 0);
            _countUnit = sharedPref.getString(getString(R.string.saved_ref_unit), "Item(s)");
        }


        _liveValue = view.findViewById(R.id.liveValue);
        _refWeightEditText = view.findViewById(R.id.ref_weight);
        EditText refCountEditText = view.findViewById(R.id.refCount);
        EditText unitCountEditText = view.findViewById(R.id.countUnit);

        formatRefWeight(_weight);
        refCountEditText.setText(String.format(Locale.US, "%d", _count));
        unitCountEditText.setText(_countUnit);


        //configure tare button
        _tare = view.findViewById(R.id.tarebutton);
        _tare.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _activity.onTare();
            }
        });
        // handle weight change
        _refWeightEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (_disable_onchange)
                    return;
                try {
                    final String strVal = s.toString();
                    _weight = Double.valueOf(strVal);
                    saveSettings();

                } catch (NumberFormatException ignored) {
                }

            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        //configure set weight button
        final Button setValue = view.findViewById(R.id.setWeightbutton);
        setValue.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _disable_onchange = true;
                formatRefWeight(_liveVal);
                _weight = _liveVal;
                saveSettings();
                _disable_onchange = false;

            }
        });
        //handle count ref changes
        refCountEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                try {
                    final String strVal = s.toString();
                    _count = Long.valueOf(strVal);
                    saveSettings();
                } catch (NumberFormatException ignored) {
                }

            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        //handle count ref changes
        unitCountEditText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                try {
                    _countUnit = s.toString();
                    saveSettings();
                } catch (NumberFormatException ignored) {
                }

            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });


        // configure calibrate button
        _calibrate = view.findViewById(R.id.calibrate);
        _calibrate.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _activity.onCalibrate();
            }
        });
        updateButtonState(_devicePresent);
        return view;
    }

    private void saveSettings()
    {
        _activity.onCountSettingsChange(_weight, _count, _countUnit);
    }

    private void formatRefWeight(double weight)
    {
        _refWeightEditText.setText(String.format(Locale.US, "%s", weight));
    }


    @Override
    public void onNewDeviceArrival(String serialNumber, String unit)
    {
        super.onNewDeviceArrival(serialNumber, unit);
        updateButtonState(true);
    }

    @Override
    public void onNewDeviceRemoval(String serialNumber)
    {
        super.onNewDeviceRemoval(serialNumber);
        _liveValue.setText(R.string.dummy_content);
        updateButtonState(false);
    }

    private void updateButtonState(boolean b)
    {
        if (_calibrate != null) {
            _calibrate.setEnabled(b);
        }
        if (_tare != null) {
            _tare.setEnabled(b);
        }
    }

    @Override
    public void onNewMeasure(double measure)
    {
        super.onNewMeasure(measure);
        _liveVal = measure;
        if (_liveValue != null) {
            _liveValue.setText(String.format(Locale.US, "%s%s", _liveVal, _unit));
        }
    }

}
