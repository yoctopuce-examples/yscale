package com.yoctopuce.examples.yscale;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

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


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        _toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(_toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.hide();
        }

        _viewPager = findViewById(R.id.viewpager);
        setupViewPager(_viewPager);

        _tabLayout = findViewById(R.id.tabs);
        _tabLayout.setupWithViewPager(_viewPager);

    }

    private void setupViewPager(ViewPager viewPager)
    {
        _viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        _viewPagerAdapter.addFragment( new GraphScaleFragment(), "Graph");
        _viewPagerAdapter.addFragment(new CountScaleFragment(), "Count");
        viewPager.setAdapter(_viewPagerAdapter);
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
            _yWeighScale.set_zeroTracking(0.5);
            _yWeighScale.set_excitation(YWeighScale.EXCITATION_AC);
            _yWeighScale.set_resolution(1);
            _yWeighScale.set_reportFrequency("4/s");
            _yWeighScale.registerTimedReportCallback(new YWeighScale.TimedReportCallback()
            {
                @Override
                public void timedReportCallback(YWeighScale function, YMeasure measure)
                {
                    getDiplayedFragment().onNewMeasure(measure);
                }
            });

            for (BasicScaleFragment fragment :_viewPagerAdapter.mFragmentList){
                fragment.onNewDeviceArrival(_serialNumber,_unit);
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
                for (BasicScaleFragment fragment :_viewPagerAdapter.mFragmentList){
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


}
