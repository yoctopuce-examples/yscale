package com.yoctopuce.examples.yscale;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class CountScaleFragment extends BasicScaleFragment
{

    private TextView _currentCountText;
    private TextView _currentWeightText;
    private TextView _currentRatioText;
    private String _unit;
    private double _weight_ref = 1;
    private long _count_ref = 1;
    private String _count_label = "";


    public CountScaleFragment()
    {
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_count_scale, container, false);
        _currentCountText = view.findViewById(R.id.current_count);
        _currentWeightText = view.findViewById(R.id.current_weight);
        _currentRatioText = view.findViewById(R.id.current_ratio);
        updateUI();
        return view;
    }


    private String formatRatio()
    {
        return String.format(Locale.US, "%s %s = %d %s", _weight_ref, _unit, _count_ref, _count_label);
    }


    private String formatCount(int count)
    {
        return String.format(Locale.US, "%d %s", count, _count_label);
    }

    private String formatWeight(double weight)
    {
        return String.format(Locale.US, "(%s%s)", weight, _unit);
    }


    @Override
    public void onNewDeviceArrival(String serialNumber, String unit)
    {
        super.onNewDeviceArrival(serialNumber, unit);
        _unit = unit;
        updateUI();
    }

    private void updateUI()
    {

        if (_currentCountText != null) {
            if (_devicePresent) {
                _currentCountText.setText(formatCount(0));
            } else {
                _currentCountText.setText(R.string.dummy_content);
            }
        }
        if (_currentWeightText != null) {
            if (_devicePresent) {
                _currentWeightText.setText(formatWeight(0));
            } else {
                _currentWeightText.setText("");
            }
        }
        if (_currentRatioText != null) {
            if (_devicePresent) {
                _currentRatioText.setText(formatRatio());
            } else {
                _currentRatioText.setText("");
            }
        }
    }

    @Override
    public void onNewDeviceRemoval(String serialNumber)
    {
        super.onNewDeviceRemoval(serialNumber);
    }

    @Override
    public void onNewMeasure(double weight)
    {
        super.onNewMeasure(weight);
        double count;
        if (_weight_ref != 0) {
            count = weight * _count_ref / _weight_ref;
        } else {
            count = 0;
        }
        if (_currentCountText != null) {
            _currentCountText.setText(formatCount((int) count));
        }
        if (_currentWeightText != null) {
            _currentWeightText.setText(formatWeight(weight));
        }
    }

    @Override
    public void onCountChanges(double weight, long count, String countUnit)
    {
        _weight_ref = weight;
        _count_ref = count;
        _count_label = countUnit;
        updateUI();
    }
}
