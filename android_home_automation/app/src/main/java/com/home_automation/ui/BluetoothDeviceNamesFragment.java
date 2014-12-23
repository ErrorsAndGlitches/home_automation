package com.home_automation.ui;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.home_automation.BluetoothCommunicator;
import com.home_automation.Logger;
import com.home_automation.app.R;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Fragment that displays a list of Bluetooth devices
 */
public class BluetoothDeviceNamesFragment extends Fragment
{
    public BluetoothDeviceNamesFragment()
    {
        m_unpairedDeviceNames = new LinkedList<String>();
        m_pairedDeviceNames = new LinkedList<String>();
        m_bluetoothCommunicatorCallback = new BluetoothBluetoothCommunicatorCallback();
        m_btDevices = new HashMap<String, BluetoothDevice>();

        Method tmpMethod = null;
        try
        {
            tmpMethod = BluetoothDevice.class.getMethod("removeBond", (Class[]) null);
        }
        catch (NoSuchMethodException e)
        {
            Logger.log(this, "Could not find BluetoothDevice.removeBond() method");
        }
        m_unpairDeviceMethod = tmpMethod;
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

        // paired devices
        ListView pairedDeviceNameList = (ListView) view.findViewById(R.id.paired_devices_list_view);
        m_pairedDevicesAdapter = new ArrayAdapter<String>(getActivity(),
                                                          android.R.layout.simple_list_item_1,
                                                          m_pairedDeviceNames);
        pairedDeviceNameList.setAdapter(m_pairedDevicesAdapter);
        pairedDeviceNameList.setOnItemClickListener(getOnItemClickListener());

        // unpaired devices
        ListView unpairedDeviceNameList = (ListView) view.findViewById(R.id.unpaired_devices_list_view);
        m_unpairedDevicesAdapter = new ArrayAdapter<String>(getActivity(),
                                                            android.R.layout.simple_list_item_1,
                                                            m_unpairedDeviceNames);
        unpairedDeviceNameList.setAdapter(m_unpairedDevicesAdapter);
        unpairedDeviceNameList.setOnItemClickListener(getOnItemClickListener());

        // refresh button
        m_refreshButton = (Button) view.findViewById(R.id.refresh_button);
        m_refreshButton.setOnClickListener(getRefreshButtonOnClickListener());

        // cancel scan button
        Button cancelButton = (Button) view.findViewById(R.id.cancel_scan);
        cancelButton.setOnClickListener(getCancelScanOnClickListener());

        return view;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        m_btCommunicator.open(m_bluetoothCommunicatorCallback);

        // if the scan fails, then set the refresh button as enabled
        // only performing paired device scan, so only clear the paired devices list
        m_pairedDeviceNames.clear();
        m_pairedDevicesAdapter.notifyDataSetChanged();
        if (!m_btCommunicator.scanForDevices(false))
        {
            m_refreshButton.setEnabled(true);
        }
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
                m_unpairedDeviceNames.clear();
                m_pairedDeviceNames.clear();
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        m_unpairedDevicesAdapter.notifyDataSetChanged();
                        m_pairedDevicesAdapter.notifyDataSetChanged();
                    }
                });
                m_btCommunicator.scanForDevices(true);
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

    private AdapterView.OnItemClickListener getOnItemClickListener()
    {
        return new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String deviceToString = ((TextView) view).getText().toString();
                Logger.log(BluetoothDeviceNamesFragment.class,
                           "Item %s at position %d was clicked",
                           deviceToString,
                           position);

                BluetoothDevice device = m_btDevices.get(deviceToString);
                if (device != null)
                {
                    switch (device.getBondState())
                    {
                    case BluetoothDevice.BOND_BONDED:
                    case BluetoothDevice.BOND_BONDING:
                        try
                        {
                            m_unpairDeviceMethod.invoke(device, (Object[]) null);
                        }
                        catch (Exception e)
                        {
                            Logger.log(BluetoothDeviceNamesFragment.class,
                                       "Unable to unpair device %s due to exception %s",
                                       deviceToString,
                                       Log.getStackTraceString(e));
                        }
                        break;
                    case BluetoothDevice.BOND_NONE:
                        device.createBond();
                        break;
                    }
                }
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
            switch (device.getBondState())
            {
            case BluetoothDevice.BOND_BONDED:
            case BluetoothDevice.BOND_BONDING:
                addDeviceToList(device, m_pairedDeviceNames, m_pairedDevicesAdapter);
                break;
            case BluetoothDevice.BOND_NONE:
                addDeviceToList(device, m_unpairedDeviceNames, m_unpairedDevicesAdapter);
                break;
            }
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

        @Override
        protected void onDeviceStateChange(BluetoothDevice device, int newState)
        {
            switch (newState)
            {
            case BluetoothDevice.BOND_BONDED:
                addDeviceToList(device, m_pairedDeviceNames, m_pairedDevicesAdapter);
                removeDeviceFromList(device, m_unpairedDeviceNames, m_unpairedDevicesAdapter);
                break;
            case BluetoothDevice.BOND_BONDING:
            case BluetoothDevice.BOND_NONE:
                addDeviceToList(device, m_unpairedDeviceNames, m_unpairedDevicesAdapter);
                removeDeviceFromList(device, m_pairedDeviceNames, m_pairedDevicesAdapter);
                break;
            }
        }

        private void addDeviceToList(BluetoothDevice device,
                                     List<String> deviceNames,
                                     final ArrayAdapter<String> deviceAdapter)
        {
            String deviceToString = deviceToString(device);
            if (!deviceNames.contains(deviceToString))
            {
                deviceNames.add(deviceToString);
                m_btDevices.put(deviceToString, device);
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        deviceAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        private void removeDeviceFromList(BluetoothDevice device,
                                          List<String> deviceNames,
                                          final ArrayAdapter<String> deviceAdapter)
        {
            String deviceToString = deviceToString(device);
            if (deviceNames.contains(deviceToString))
            {
                deviceNames.remove(deviceToString);
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        deviceAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    private final BluetoothCommunicator.BluetoothCommunicatorCallback m_bluetoothCommunicatorCallback;
    private final Map<String, BluetoothDevice>                        m_btDevices;
    private final Method                                              m_unpairDeviceMethod;

    // paired devices
    private       ArrayAdapter<String> m_pairedDevicesAdapter;
    private final List<String>         m_pairedDeviceNames;
    // unpaired devices
    private       ArrayAdapter<String> m_unpairedDevicesAdapter;
    private final List<String>         m_unpairedDeviceNames;

    private Button                m_refreshButton;
    private BluetoothCommunicator m_btCommunicator;
}

