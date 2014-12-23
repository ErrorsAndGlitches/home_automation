package com.home_automation.ui;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.home_automation.BluetoothCommunicator;
import com.home_automation.app.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Fragment that displays a list of Bluetooth devices
 */
public class BluetoothDeviceNamesFragment extends Fragment
{
    public BluetoothDeviceNamesFragment()
    {
        m_btDeviceNames = new LinkedList<String>();
        m_bluetoothCommunicatorCallback = new BluetoothBluetoothCommunicatorCallback();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_btCommunicator = new BluetoothCommunicator(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_bluetooth_communicator, container, false);
        ListView deviceNameList = (ListView) view.findViewById(R.id.bt_device_names);
        m_deviceNamesAdapter = new ArrayAdapter<String>(getActivity(),
                                                        android.R.layout.simple_list_item_1,
                                                        m_btDeviceNames);
        deviceNameList.setAdapter(m_deviceNamesAdapter);

        m_refreshButton = (Button) view.findViewById(R.id.refresh_button);
        m_refreshButton.setOnClickListener(getRefreshButtonOnClickListener());

        Button cancelButton = (Button) view.findViewById(R.id.cancel_scan);
        cancelButton.setOnClickListener(getCancelScanOnClickListener());

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        m_btDeviceNames.clear();
        m_btCommunicator.open(m_bluetoothCommunicatorCallback);
        m_btCommunicator.scanForDevices();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        m_btCommunicator.close();
    }

    private View.OnClickListener getRefreshButtonOnClickListener()
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                m_refreshButton.setEnabled(false);
                m_btDeviceNames.clear();
                m_btCommunicator.scanForDevices();
            }
        };
    }

    private View.OnClickListener getCancelScanOnClickListener()
    {
        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                m_btCommunicator.cancelScan();
            }
        };
    }

    private static String deviceToString(BluetoothDevice device)
    {
        return String.format("Name(%s) Address(%s)", device.getName(), device.getAddress());
    }

    private class BluetoothBluetoothCommunicatorCallback extends BluetoothCommunicator.BluetoothCommunicatorCallback
    {
        @Override
        protected void onDeviceDiscovered(BluetoothDevice device)
        {
            m_btDeviceNames.add(deviceToString(device));
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    m_deviceNamesAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        protected void onScanFinished()
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    m_refreshButton.setEnabled(true);
                }
            });
        }
    }

    private final List<String>                                        m_btDeviceNames;
    private final BluetoothCommunicator.BluetoothCommunicatorCallback m_bluetoothCommunicatorCallback;
    private       ArrayAdapter<String>                                m_deviceNamesAdapter;
    private       Button                                              m_refreshButton;
    private       BluetoothCommunicator                               m_btCommunicator;
}

