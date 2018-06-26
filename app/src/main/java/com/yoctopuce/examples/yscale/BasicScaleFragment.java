package com.yoctopuce.examples.yscale;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yoctopuce.YoctoAPI.YMeasure;

/**
 * Created by Yoctopuce on 25.06.2018.
 */
public class BasicScaleFragment extends Fragment
{
    protected OnFragmentAction _activity;


    // Container Activity must implement this interface
    public interface OnFragmentAction
    {
        public void onTare();

        public void goToSettings();
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
    }

    public void onNewDeviceRemoval(String serialNumber)
    {
        Log.d("Basic", "removal of " + serialNumber);

    }

    public void onNewMeasure(YMeasure measure)
    {

    }


}
