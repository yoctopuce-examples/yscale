package com.yoctopuce.examples.yscale;

import android.os.Bundle;
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
public class CountScaleFragment extends BasicScaleFragment
{

    private TextView _currentCountText;
    private TextView _currentWeightText;
    private TextView _currentRatioText;
    private Button _setRatioButton;
    private String _unit;
    private double _weight_ref = 22.2;
    private double _count_ref = 5;
    private String _count_label = "box";


    public CountScaleFragment()
    {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_basic_scale, container, false);
        _currentCountText = view.findViewById(R.id.current_count);
        _currentWeightText = view.findViewById(R.id.current_weight);
        _currentRatioText = view.findViewById(R.id.current_ratio);
        _setRatioButton = view.findViewById(R.id.setRatio);
        _setRatioButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

            }
        });
        return view;
    }


    private String formatRatio()
    {
        return String.format(Locale.US, "%f %s = %f %s", _weight_ref, _unit, _count_ref, _count_label);
    }


    private String formatCount(int count)
    {
        return String.format(Locale.US, "%d %s", count, _count_label);
    }

    private String formatWeight(double weight)
    {
        return String.format(Locale.US, "(%.1f%s)", weight, _unit);
    }


    @Override
    public void onNewDeviceArrival(String serialNumber, String unit)
    {
        super.onNewDeviceArrival(serialNumber, unit);
        _unit = unit;
        _currentCountText.setText(formatCount(0));
        _currentWeightText.setText(formatWeight(0));
        _currentRatioText.setText(formatRatio());
        _setRatioButton.setEnabled(true);
    }

    @Override
    public void onNewDeviceRemoval(String serialNumber)
    {
        super.onNewDeviceRemoval(serialNumber);
        _currentCountText.setText(R.string.dummy_content);
        _currentWeightText.setText("");
        _currentRatioText.setText("");
        _setRatioButton.setEnabled(false);
    }

    @Override
    public void onNewMeasure(YMeasure measure)
    {
        super.onNewMeasure(measure);
        final double weight = measure.get_averageValue();
        double count = weight * _count_ref / _weight_ref;
        _currentCountText.setText(formatCount((int) count));
        _currentWeightText.setText(formatWeight(weight));
    }

}
