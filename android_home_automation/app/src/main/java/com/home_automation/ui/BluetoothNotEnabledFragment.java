package com.home_automation.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.home_automation.app.R;

/**
 * Fragment shown when Bluetooth is not enabled
 */
public class BluetoothNotEnabledFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_bluetooth_not_available, container, false);

        Button enableBluetoothButton = (Button) view.findViewById(R.id.enable_bt_button);
        enableBluetoothButton.setOnClickListener(getEnableBtOnClickListener());

        return view;
    }

    private View.OnClickListener getEnableBtOnClickListener()
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                BluetoothActivity.requestEnableBluetooth(getActivity());
            }
        };
    }
}

