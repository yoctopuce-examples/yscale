package com.yoctopuce.examples.yscale;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPIContext;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YMeasure;
import com.yoctopuce.YoctoAPI.YModule;
import com.yoctopuce.YoctoAPI.YWeighScale;

import java.util.Date;
import java.util.Locale;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements CalibrateDialogFragment.CalibrateDialogListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    public static final int DELAY_MILLIS = 100;
    public static final int MAX_X = 100;
    private final Handler mHideHandler = new Handler();
    private TextView mContentView;
    private View mControlsView;
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private String _unit;
    private String _serialNumber;
    private YAPIContext _yctx;
    private LineGraphSeries<DataPoint> _series;
    private double graph2LastXValue = 0;
    private double _maxY;
    private Viewport _viewport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_current_value);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
        Button _tare_button = findViewById(R.id.tare_button);
        _tare_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tare();
                hide();
            }
        });
        Button _calibrate_button = findViewById(R.id.calibrate_button);
        _calibrate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new CalibrateDialogFragment();
                newFragment.show(getSupportFragmentManager(), "calibration");
                hide();
            }
        });

        GraphView graph = findViewById(R.id.graph);
        _series = new LineGraphSeries<>();
        _series.setDrawBackground(true);
        _series.setThickness(10);
        graph.addSeries(_series);
        _viewport = graph.getViewport();
        _viewport.setXAxisBoundsManual(true);
        _viewport.setMinX(0);
        _viewport.setMaxX(40);
        _viewport.setMinY(0);
        _maxY = 1000;
        _viewport.setMaxY(_maxY);
        _viewport.setYAxisBoundsManual(true);
        final GridLabelRenderer gridLabelRenderer = graph.getGridLabelRenderer();
        gridLabelRenderer.setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
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


        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.
                            show();
                        } else {
                            // TODO: The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                        }
                    }
                });
        hide();
    }


    @Override
    protected void onStart() {
        super.onStart();
        try {
            _yctx = new YAPIContext();
            _yctx.EnableUSBHost(this);
            _yctx.RegisterHub("usb");
            _yctx.RegisterDeviceArrivalCallback(new YAPI.DeviceArrivalCallback() {
                @Override
                public void yDeviceArrival(YModule module) {
                    arrival(module);
                }
            });
            _yctx.RegisterDeviceRemovalCallback(new YAPI.DeviceRemovalCallback() {
                @Override
                public void yDeviceRemoval(YModule module) {
                    removal(module);
                }
            });
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            Snackbar.make(mContentView,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();

        }
        mHideHandler.postDelayed(_periodicUpdate, DELAY_MILLIS);
    }


    @Override
    protected void onStop() {
        mHideHandler.removeCallbacks(_periodicUpdate);
        _yctx.FreeAPI();
        super.onStop();
    }


    private double _hardware_detect;
    private YWeighScale _yWeighScale;


    private void arrival(YModule module) {
        try {
            String serialNumber = module.get_serialNumber();
            if (!serialNumber.startsWith("YWBRIDG1")) {
                return;
            }
            if (_serialNumber != null && !_serialNumber.equals(serialNumber)) {
                removal(YModule.FindModuleInContext(_yctx, _serialNumber + ".module"));
            }
            _serialNumber = serialNumber;
            _yWeighScale = YWeighScale.FindWeighScaleInContext(_yctx, _serialNumber + ".weighScale1");
            _unit = _yWeighScale.get_unit();
            _yWeighScale.set_zeroTracking(0.5);
            _yWeighScale.set_excitation(YWeighScale.EXCITATION_AC);
            _yWeighScale.set_resolution(1);
            _yWeighScale.set_reportFrequency("4/s");
            _yWeighScale.registerTimedReportCallback(new YWeighScale.TimedReportCallback() {
                @Override
                public void timedReportCallback(YWeighScale function, YMeasure measure) {
                    newWeight(measure);
                }
            });

        } catch (YAPI_Exception e) {
            e.printStackTrace();
            Snackbar.make(mContentView,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();

        }
    }

    private void removal(YModule module) {
        try {
            String serialNumber = module.get_serialNumber();
            if (serialNumber.equals(_serialNumber)) {
                _yWeighScale.registerValueCallback((YWeighScale.UpdateCallback) null);
                _yWeighScale = null;
                _serialNumber = null;
                mContentView.setText(R.string.dummy_content);
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            Snackbar.make(mContentView,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();

        }

    }

    private void newWeight(YMeasure value) {
        final double averageValue = value.get_averageValue();
        final Date utc = value.get_startTimeUTC_asDate();
        final String text = String.format(Locale.US, "%.1f %s", averageValue, _unit);
        mContentView.setText(text);
        if (averageValue > _maxY) {
            _maxY = averageValue;
            _viewport.setMaxY(_maxY);
        }
        _series.appendData(new DataPoint(graph2LastXValue, averageValue), true, 100);
        graph2LastXValue += 1d;
    }

    private void calibrate(long value, long maxValue) {
        if (_yWeighScale != null) {
            try {
                _yWeighScale.setupSpan(value, maxValue);
                _yWeighScale.module().saveToFlash();
                Snackbar.make(mContentView,
                        String.format(Locale.US, "Calibrated for %dg (max %dg)", value, maxValue),
                        Snackbar.LENGTH_LONG).show();
            } catch (YAPI_Exception e) {
                e.printStackTrace();
                Snackbar.make(mContentView,
                        "Error:" + e.getLocalizedMessage(),
                        Snackbar.LENGTH_INDEFINITE).show();
            }
        }
    }

    private void tare() {
        if (_yWeighScale != null) {
            try {
                _yWeighScale.tare();
            } catch (YAPI_Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final Runnable _periodicUpdate = new Runnable() {
        @Override
        public void run() {
            try {
                if (_hardware_detect++ == 0) {
                    _yctx.UpdateDeviceList();
                }
                _yctx.HandleEvents();
                if (_hardware_detect > 40) {
                    _hardware_detect = 0;
                }

            } catch (YAPI_Exception e) {
                e.printStackTrace();
                Snackbar.make(mContentView,
                        "Error:" + e.getLocalizedMessage(),
                        Snackbar.LENGTH_INDEFINITE).show();
            }
            mHideHandler.postDelayed(_periodicUpdate, DELAY_MILLIS);
        }
    };


    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }


    private void hide() {

        hideAndroidStuff();
        mControlsView.setVisibility(View.GONE);
        mVisible = false;
    }

    private void show() {

        //hideAndroidStuff();
        mControlsView.setVisibility(View.VISIBLE);
        mVisible = true;
    }

    private void hideAndroidStuff() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }


    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, long value, long maxValue) {
        calibrate(value, maxValue);
    }


}
