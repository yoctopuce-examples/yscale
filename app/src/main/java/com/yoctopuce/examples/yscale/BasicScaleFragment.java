package com.yoctopuce.examples.yscale;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class BasicScaleFragment extends Fragment
{
    protected OnFragmentAction _activity;
    protected boolean _devicePresent;
    protected String _unit = "";

    public void onCountChanges(double weight, long count, String countUnit)
    {

    }


    // Container Activity must implement this interface
    public interface OnFragmentAction
    {
        public void onTare();

        public void goToSettings();

        public void onCalibrate();

        void onCountSettingsChange(double weight, long count, String countUnit);
    }


    public BasicScaleFragment()
    {
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            _activity = (OnFragmentAction) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentAction");
        }

    }


    public void onNewDeviceArrival(String serialNumber, String unit)
    {
        Log.d("Basic", "arrival of " + serialNumber);
        _devicePresent = true;
        _unit = unit;

    }

    public void onNewDeviceRemoval(String serialNumber)
    {
        Log.d("Basic", "removal of " + serialNumber);
        _devicePresent = false;

    }

    public void onNewMeasure(double measure)
    {

    }


}
