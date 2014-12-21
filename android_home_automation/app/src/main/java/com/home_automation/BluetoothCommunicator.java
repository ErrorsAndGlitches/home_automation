package com.home_automation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Handles the discovery, connection, and communication with a bluetooth access point
 */
class BluetoothCommunicator
{
    interface DeviceScanCallback
    {
        void onDeviceDiscovered(BluetoothDevice device);

        void onScanFinished();
    }

    static synchronized BluetoothCommunicator getInstance(Context context)
    {
        if (s_btCommunicator == null)
        {
            s_btCommunicator = new BluetoothCommunicator(context);
        }

        return s_btCommunicator;
    }

    boolean isBluetoothSupported()
    {
        return m_btAdapter != null;
    }

    boolean isBluetoothEnabled()
    {
        return m_btAdapter.isEnabled();
    }

    void scanForDevices(DeviceScanCallback callback)
    {
        m_callback = callback;
        if (!m_isDiscovering && isBluetoothEnabled())
        {
            if (!m_btAdapter.startDiscovery())
            {
                Logger.log(this, "Failed to start Bluetooth discovery");
            }
            else
            {
                Logger.log(this, "Starting discovery of Bluetooth devices");
                m_isDiscovering = true;
            }
        }
    }

    void cancelScan()
    {
        m_callback = null;
        if (m_isDiscovering)
        {
            m_btAdapter.cancelDiscovery();
        }
    }

    private BluetoothCommunicator(Context context)
    {
        m_context = context;
        m_btAdapter = BluetoothAdapter.getDefaultAdapter();
        registerBluetoothScanReceiver();
    }

    private void registerBluetoothScanReceiver()
    {
        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (m_callback != null)
                    {
                        Logger.log(BluetoothCommunicator.class,
                                   "Found Bluetooth device with name %s and address %s",
                                   device.getName(),
                                   device.getAddress());
                        m_callback.onDeviceDiscovered(device);
                    }
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
                {
                    Logger.log(BluetoothCommunicator.class, "Bluetooth device scan finished");
                    m_isDiscovering = false;
                    if (m_callback != null)
                    {
                        m_callback.onScanFinished();
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        m_context.registerReceiver(receiver, filter);
    }

    private static BluetoothCommunicator s_btCommunicator;

    private final    Context            m_context;
    private final    BluetoothAdapter   m_btAdapter;
    private          DeviceScanCallback m_callback;
    // Android's BT stack is simply god awful and the BluetoothAdapter.isDiscovering() doesn't work
    private volatile boolean            m_isDiscovering;
}
