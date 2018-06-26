package com.yoctopuce.examples.yscale;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.yoctopuce.YoctoAPI.YMeasure;

import java.util.Locale;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class SettingsFragment extends BasicScaleFragment
{

    private TextView _liveValue;
    private TextView _currentWeightText;
    private TextView _currentCount;
    private String _unit;
    private String _count_label = "box";


    public SettingsFragment()
    {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);
        _liveValue = view.findViewById(R.id.liveValue);
        _currentWeightText = view.findViewById(R.id.currWeight);
        _currentCount = view.findViewById(R.id.curCount);
        return view;
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
        _currentWeightText.setText("");
        _currentCount.setText("");
    }

    @Override
    public void onNewMeasure(YMeasure measure)
    {
        super.onNewMeasure(measure);
        final double weight = measure.get_averageValue();
        _liveValue.setText(String.format(Locale.US, "%.1f%s", weight, _unit));
    }

}
