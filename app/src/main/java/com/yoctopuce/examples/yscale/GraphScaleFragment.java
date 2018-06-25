package com.yoctopuce.examples.yscale;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.yoctopuce.YoctoAPI.YMeasure;

import java.util.Locale;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class GraphScaleFragment extends BasicScaleFragment
{
    private TextView _textView;
    private LineGraphSeries<DataPoint> _series;
    private double _lastXValue = 0;
    private double _maxY;
    private Viewport _viewport;
    private static final int MAX_X = 100;
    private String _unit = "";


    public GraphScaleFragment()
    {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_graph_scale, container, false);
        _textView = view.findViewById(R.id.fullscreen_current_value);
        GraphView graph = view.findViewById(R.id.graph);
        _series = new LineGraphSeries<>();
        _series.setDrawBackground(true);
        _series.setThickness(10);
        _series.setColor(getResources().getColor(R.color.colorAccent));
        _series.setBackgroundColor(getResources().getColor(R.color.primary));
        graph.addSeries(_series);
        _viewport = graph.getViewport();
        _viewport.setXAxisBoundsManual(true);
        _viewport.setMinX(0);
        _viewport.setMaxX(MAX_X);
        _viewport.setMinY(0);
        _maxY = 1000;
        _viewport.setMaxY(_maxY);
        _viewport.setYAxisBoundsManual(true);
        final GridLabelRenderer gridLabelRenderer = graph.getGridLabelRenderer();
        gridLabelRenderer.setLabelFormatter(new DefaultLabelFormatter()
        {
            @Override
            public String formatLabel(double value, boolean isValueX)
            {
                if (isValueX) {
                    return "";//super.formatLabel(value, true);
                } else {
                    // show currency for y values
                    String label = super.formatLabel(value, false);
                    if (_unit != null) {
                        label += " " + _unit;
                    }
                    return label;
                }
            }
        });

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
        _textView.setText(R.string.dummy_content);
    }

    @Override
    public void onNewMeasure(YMeasure measure)
    {
        final double averageValue = measure.get_averageValue();
        final String text;

        if (averageValue > 1000) {
            text = String.format(Locale.US, "%.3f %s", averageValue / 1000, "kg");
        } else {
            text = String.format(Locale.US, "%d %s", (int) averageValue, _unit);
        }
        _textView.setText(text);
        if (averageValue > _maxY) {
            _maxY = averageValue;
            _viewport.setMaxY(_maxY);
        }
        _series.appendData(new DataPoint(_lastXValue, averageValue), true, MAX_X);
        _lastXValue += 1d;
    }


}
