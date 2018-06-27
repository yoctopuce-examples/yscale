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

import com.yoctopuce.YoctoAPI.YMeasure;

import java.util.Locale;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class SettingsFragment extends BasicScaleFragment
{

    private TextView _liveValue;
    private EditText _refWeightEditText;
    private String _unit;
    private double _liveVal;
    private float _weight;
    private long _count;
    private String _countUnit;


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
            _weight = sharedPref.getFloat(getString(R.string.saved_ref_weight), 0);
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
        final Button tare = view.findViewById(R.id.tarebutton);
        tare.setOnClickListener(new View.OnClickListener()
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
                try {
                    final String strVal = s.toString();
                    _weight = Float.valueOf(strVal);
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
                formatRefWeight((float) _liveVal);
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
        Button calibrate = view.findViewById(R.id.calibrate);
        calibrate.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                _activity.onCalibrate();
            }
        });


        return view;
    }

    private void saveSettings()
    {
        _activity.onCountSettingsChange(_weight, _count, _countUnit);
    }

    private void formatRefWeight(float weight)
    {
        _refWeightEditText.setText(String.format(Locale.US, "%f", weight));
    }


    @Override
    public void onNewDeviceArrival(String serialNumber, String unit)
    {
        super.onNewDeviceArrival(serialNumber, unit);
        _unit = unit;
    }

    @Override
    public void onNewDeviceRemoval(String serialNumber)
    {
        super.onNewDeviceRemoval(serialNumber);
        _liveValue.setText(R.string.dummy_content);
    }

    @Override
    public void onNewMeasure(YMeasure measure)
    {
        super.onNewMeasure(measure);
        _liveVal = measure.get_averageValue();
        _liveValue.setText(String.format(Locale.US, "%.1f%s", _liveVal, _unit));
    }

}
