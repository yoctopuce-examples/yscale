package com.yoctopuce.examples.yscale;

import android.content.Context;
import androidx.fragment.app.Fragment;
import android.util.Log;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class BasicScaleFragment extends Fragment
{
    OnFragmentAction _activity;
    boolean _devicePresent;
    String _unit = "";

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
        void onUnitchange(String Unit);
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

    public void onUnitUpdate(String unit)
    {
        _unit = unit;
    }

    public void onNewMeasure(double measure)
    {

    }


}
