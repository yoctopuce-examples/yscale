package com.yoctopuce.examples.yscale;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPIContext;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YMeasure;
import com.yoctopuce.YoctoAPI.YModule;
import com.yoctopuce.YoctoAPI.YWeighScale;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FullscreenActivity extends AppCompatActivity implements CalibrateDialogFragment.CalibrateDialogListener, BasicScaleFragment.OnFragmentAction
{

    private static final int DELAY_MILLIS = 100;
    private final Handler _handler = new Handler();
    private String _unit;
    private String _serialNumber;
    private YAPIContext _yctx;
    private Toolbar _toolbar;
    private ViewPager _viewPager;
    private TabLayout _tabLayout;
    private ViewPagerAdapter _viewPagerAdapter;
    private double _resolution;
    private double _zeroTracking;
    private int _exitation;
    private long _iresol;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        _viewPager = findViewById(R.id.viewpager);
        setupViewPager(_viewPager);


        _tabLayout = findViewById(R.id.tabs);
        _tabLayout.setupWithViewPager(_viewPager);

    }

    private void setupViewPager(ViewPager viewPager)
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String strWeight = sharedPref.getString(getString(R.string.saved_ref_weight), "0.0");
        double weight = Double.valueOf(strWeight);
        long count = sharedPref.getLong(getString(R.string.saved_ref_count), 0);
        String countUnit = sharedPref.getString(getString(R.string.saved_ref_unit), "Item(s)");
        String lastPannel = sharedPref.getString(getString(R.string.saved_last_pannel), "Graph");

        _viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        _viewPagerAdapter.addFragment(new GraphScaleFragment(), "Graph");
        _viewPagerAdapter.addFragment(new CountScaleFragment(), "Count");
        _viewPagerAdapter.addFragment(new SettingsFragment(), "Settings");
        viewPager.setAdapter(_viewPagerAdapter);
        for (BasicScaleFragment fragment : _viewPagerAdapter.mFragmentList) {
            fragment.onCountChanges(weight, count, countUnit);
        }
        if (lastPannel.equals("Count")) {
            _viewPager.setCurrentItem(1);
        }

    }

    class ViewPagerAdapter extends FragmentPagerAdapter
    {
        final List<BasicScaleFragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager)
        {
            super(manager);
        }

        @Override
        public BasicScaleFragment getItem(int position)
        {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount()
        {
            return mFragmentList.size();
        }

        void addFragment(BasicScaleFragment fragment, String title)
        {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            return mFragmentTitleList.get(position);
        }

    }


    @Override
    protected void onStart()
    {
        super.onStart();
        try {
            _yctx = new YAPIContext();
            _yctx.EnableUSBHost(this);
            _yctx.RegisterHub("usb");
            _yctx.RegisterDeviceArrivalCallback(new YAPI.DeviceArrivalCallback()
            {
                @Override
                public void yDeviceArrival(YModule module)
                {
                    arrival(module);
                }
            });
            _yctx.RegisterDeviceRemovalCallback(new YAPI.DeviceRemovalCallback()
            {
                @Override
                public void yDeviceRemoval(YModule module)
                {
                    removal(module);
                }
            });
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            Snackbar.make(_viewPager,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();

        }
        _handler.postDelayed(_periodicUpdate, DELAY_MILLIS);
    }


    @Override
    protected void onStop()
    {

        // save current panel to restart the application in the same state
        final int curTabIdx = _viewPager.getCurrentItem();
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(getString(R.string.saved_last_pannel), _viewPagerAdapter.mFragmentTitleList.get(curTabIdx))
                .apply();

        _handler.removeCallbacks(_periodicUpdate);
        _yctx.FreeAPI();
        super.onStop();
    }


    private double _hardware_detect;
    private YWeighScale _yWeighScale;


    private void arrival(YModule module)
    {
        try {
            String serial = module.get_serialNumber();
            if (!serial.startsWith("YWBRIDG1")) {
                return;
            }
            if (_serialNumber != null && !_serialNumber.equals(serial)) {
                removal(YModule.FindModuleInContext(_yctx,
                        _serialNumber + ".module"));
            }
            _serialNumber = serial;
            _yWeighScale = YWeighScale.FindWeighScaleInContext(_yctx,
                    _serialNumber + ".weighScale1");
            _unit = _yWeighScale.get_unit();
            _resolution = _yWeighScale.get_resolution();
            _iresol = Math.round(1.0 / _resolution);
            _zeroTracking = _yWeighScale.get_zeroTracking();
            _exitation = _yWeighScale.get_excitation();

            //_yWeighScale.set_zeroTracking(0.02);
            //_yWeighScale.set_excitation(YWeighScale.EXCITATION_AC);
            //_yWeighScale.set_resolution(0.01);
            _yWeighScale.set_reportFrequency("4/s");
            _yWeighScale.registerTimedReportCallback(new YWeighScale.TimedReportCallback()
            {
                @Override
                public void timedReportCallback(YWeighScale function, YMeasure measure)
                {
                    double roundvalue = measure.get_averageValue() * _iresol;
                    final double v = ((double) Math.round(roundvalue)) / _iresol;
                    getDiplayedFragment().onNewMeasure(v);
                }
            });

            for (BasicScaleFragment fragment : _viewPagerAdapter.mFragmentList) {
                fragment.onNewDeviceArrival(_serialNumber, _unit);

            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            Snackbar.make(_viewPager,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();

        }
    }

    private BasicScaleFragment getDiplayedFragment()
    {
        return _viewPagerAdapter.getItem(_viewPager.getCurrentItem());
    }

    private void removal(YModule module)
    {
        try {
            String serialNumber = module.get_serialNumber();
            if (serialNumber.equals(_serialNumber)) {
                _yWeighScale.registerValueCallback((YWeighScale.UpdateCallback) null);
                _yWeighScale = null;
                _serialNumber = null;
                for (BasicScaleFragment fragment : _viewPagerAdapter.mFragmentList) {
                    fragment.onNewDeviceRemoval(serialNumber);
                }
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
            Snackbar.make(_viewPager,
                    "Error:" + e.getLocalizedMessage(),
                    Snackbar.LENGTH_INDEFINITE).show();

        }

    }


    private void calibrate(long value, long maxValue)
    {
        if (_yWeighScale != null) {
            try {
                _yWeighScale.setupSpan(value, maxValue);
                _yWeighScale.module().saveToFlash();
                Snackbar.make(_viewPager,
                        String.format(Locale.US, "Calibrated for %dg (max %dg)", value, maxValue),
                        Snackbar.LENGTH_LONG).show();
            } catch (YAPI_Exception e) {
                e.printStackTrace();
                Snackbar.make(_viewPager,
                        "Error:" + e.getLocalizedMessage(),
                        Snackbar.LENGTH_INDEFINITE).show();
            }
        }
    }

    private void tare()
    {
        if (_yWeighScale != null) {
            try {
                _yWeighScale.tare();
            } catch (YAPI_Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final Runnable _periodicUpdate = new Runnable()
    {
        @Override
        public void run()
        {
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
                Snackbar.make(_viewPager,
                        "Error:" + e.getLocalizedMessage(),
                        Snackbar.LENGTH_INDEFINITE).show();
            }
            _handler.postDelayed(_periodicUpdate, DELAY_MILLIS);
        }
    };


    @Override
    public void onDialogPositiveClick(DialogFragment dialog, long value, long maxValue)
    {
        calibrate(value, maxValue);
    }

    @Override
    public void onTare()
    {
        tare();
    }

    @Override
    public void goToSettings()
    {
        _viewPager.setCurrentItem(2, true);
    }

    @Override
    public void onCalibrate()
    {
        DialogFragment newFragment = new CalibrateDialogFragment();
        newFragment.show(getSupportFragmentManager(), "calibration");
    }

    @Override
    public void onCountSettingsChange(double weight, long count, String countUnit)
    {

        String doubleStr = Double.toString(weight);
        // first save values in preferences
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(getString(R.string.saved_ref_weight), doubleStr)
                .putLong(getString(R.string.saved_ref_count), count)
                .putString(getString(R.string.saved_ref_unit), countUnit)
                .apply();
        // then update all fragments
        for (BasicScaleFragment fragment : _viewPagerAdapter.mFragmentList) {
            fragment.onCountChanges(weight, count, countUnit);
        }

    }


}
