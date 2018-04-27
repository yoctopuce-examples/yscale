package com.yoctopuce.examples.yscale;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YModule;
import com.yoctopuce.YoctoAPI.YWeighScale;

import java.util.Locale;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
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
    private final Handler mHideHandler = new Handler();
    private TextView mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private Button _tare_button;
    private Button _calibrate_button;
    private String _unit;
    private String _serialNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_current_value);
        _tare_button = findViewById(R.id.tare_button);
        _tare_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tare();
            }
        });
        _calibrate_button = findViewById(R.id.calibrate_button);
        _calibrate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrate();
            }
        });

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.tare_button).setOnTouchListener(mDelayHideTouchListener);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    @Override
    protected void onStart() {
        super.onStart();
        try {
            YAPI.EnableUSBHost(this);
            YAPI.RegisterHub("usb");
            YAPI.RegisterDeviceArrivalCallback(new YAPI.DeviceArrivalCallback() {
                @Override
                public void yDeviceArrival(YModule module) {
                    arrival(module);
                }
            });
            YAPI.RegisterDeviceRemovalCallback(new YAPI.DeviceRemovalCallback() {
                @Override
                public void yDeviceRemoval(YModule module) {
                    removal(module);
                }
            });
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            //todo: better error handling
            /*
            Snackbar.make(mContentView,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();
                    */
        }
        mHideHandler.postDelayed(_periodicUpdate, 100);
    }


    @Override
    protected void onStop() {
        mHideHandler.removeCallbacks(_periodicUpdate);
        YAPI.FreeAPI();
        super.onStop();
    }


    private double _hardwaredetect;
    private YWeighScale _yWeighScale;


    private void arrival(YModule module) {
        try {
            String serialNumber = module.get_serialNumber();
            if (!serialNumber.startsWith("YWBRIDG1")) {
                return;
            }
            if (_serialNumber != null && !_serialNumber.equals(serialNumber)) {
                removal(YModule.FindModule(_serialNumber + ".module"));
            }
            _serialNumber = serialNumber;
            _yWeighScale = YWeighScale.FindWeighScale(_serialNumber + ".weighScale1");
            _yWeighScale.registerValueCallback(new YWeighScale.UpdateCallback() {
                @Override
                public void yNewValue(YWeighScale function, String functionValue) {
                    newWeight(functionValue);
                }
            });
            _unit = _yWeighScale.get_unit();

        } catch (YAPI_Exception e) {
            e.printStackTrace();
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
        }

    }

    private void newWeight(String value) {
        final String text = String.format(Locale.US, "%s %s",
                value, _unit);
        mContentView.setText(text);
    }

    private void calibrate() {
        if (_yWeighScale != null) {
            try {
                //fixme: add popup to get right parameter
                _yWeighScale.setupSpan(200, 25000);
            } catch (YAPI_Exception e) {
                e.printStackTrace();
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

    private Runnable _periodicUpdate = new Runnable() {
        @Override
        public void run() {
            try {
                if (_hardwaredetect++ == 0) {
                    YAPI.UpdateDeviceList();
                }
                YAPI.HandleEvents();
                if (_hardwaredetect > 20) {
                    _hardwaredetect = 0;
                }

            } catch (YAPI_Exception e) {
                e.printStackTrace();
                //fixme: better error handling
                /*Snackbar.make(mContentView,
                        "Error:" + e.getLocalizedMessage(),
                        Snackbar.LENGTH_INDEFINITE).show(); */
            }
            mHideHandler.postDelayed(_periodicUpdate, 500);
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
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
