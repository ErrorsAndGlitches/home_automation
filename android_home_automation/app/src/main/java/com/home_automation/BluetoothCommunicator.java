package com.home_automation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the discovery, connection, and communication with a bluetooth access point
 */
public class BluetoothCommunicator
{
    /**
     * Class to provide a callback mechanism to users of the Bluetooth communicator
     */
    public static abstract class BluetoothCommunicatorCallback
    {
        protected void onBluetoothEnabled()
        {
        }

        protected void onBluetoothDisabled()
        {
        }

        protected void onDeviceDiscovered(BluetoothDevice device)
        {
        }

        protected void onScanFinished()
        {
        }
    }

    public BluetoothCommunicator(Context context)
    {
        m_context = context;
        m_receivers = new ArrayList<BluetoothCommunicatorBroadcastReceiver>(2);
        m_receivers.add(new BluetoothStateChangeReceiver());
        m_receivers.add(new BluetoothScanBroadcastReceiver());
    }

    public void open(BluetoothCommunicatorCallback callback)
    {
        m_callback = callback;
        if (m_btAdapter == null)
        {
            m_btAdapter = BluetoothAdapter.getDefaultAdapter();
            registerReceivers();
        }
    }

    public void close()
    {
        deregisterReceivers();
        cancelScan();
        m_btAdapter = null;
        m_callback = null;
    }

    public boolean isBluetoothSupported()
    {
        return m_btAdapter != null;
    }

    public boolean isBluetoothEnabled()
    {
        return isBluetoothSupported() && m_btAdapter.isEnabled();
    }

    public boolean scanForDevices()
    {
        if (!isBluetoothEnabled())
        {
            Logger.log(this, "Bluetooth is not enabled, unable to scan for devices");
            return false;
        }
        else if (m_btAdapter.startDiscovery())
        {
            Logger.log(this, "Starting discovery of Bluetooth devices");
            return true;
        }
        else
        {
            Logger.log(this, "Failed to start Bluetooth discovery");
            return false;
        }
    }

    public boolean cancelScan()
    {
        if (!isBluetoothEnabled())
        {
            Logger.log(this, "Bluetooth is not enabled, unable to cancel scan for devices");
            return false;
        }
        else if (!m_btAdapter.isDiscovering())
        {
            Logger.log(this, "Bluetooth is not discovering devices, unable to cancel scan for devices");
            return false;
        }
        else if (m_btAdapter.cancelDiscovery())
        {
            Logger.log(this, "Successfully canceled discovery of Bluetooth devices");
            return true;
        }
        else
        {
            Logger.log(this, "Failed to cancel the discovery of Bluetooth devices");
            return false;
        }
    }

    private void registerReceivers()
    {
        for (BluetoothCommunicatorBroadcastReceiver receiver : m_receivers)
        {
            IntentFilter filter = new IntentFilter();
            for (String action : receiver.getActions())
            {
                filter.addAction(action);
            }
            m_context.registerReceiver(receiver, filter);
        }
    }

    private void deregisterReceivers()
    {
        for (BluetoothCommunicatorBroadcastReceiver receiver : m_receivers)
        {
            m_context.unregisterReceiver(receiver);
        }
    }

    private abstract class BluetoothCommunicatorBroadcastReceiver extends BroadcastReceiver
    {
        protected BluetoothCommunicatorBroadcastReceiver(String... actions)
        {
            m_actions = new ArrayList<String>(actions.length);
            for (String action : actions)
            {
                m_actions.add(action);
            }
        }

        List<String> getActions()
        {
            return m_actions;
        }

        private final List<String> m_actions;
    }

    private class BluetoothScanBroadcastReceiver extends BluetoothCommunicatorBroadcastReceiver
    {
        BluetoothScanBroadcastReceiver()
        {
            super(BluetoothDevice.ACTION_FOUND, BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Logger.log(BluetoothCommunicator.class,
                           "Found Bluetooth device with name %s and address %s",
                           device.getName(),
                           device.getAddress());
                if (m_callback != null)
                {
                    m_callback.onDeviceDiscovered(device);
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction()))
            {
                Logger.log(BluetoothCommunicator.class, "Bluetooth device scan finished");
                if (m_callback != null)
                {
                    m_callback.onScanFinished();
                }
            }
        }
    }

    private class BluetoothStateChangeReceiver extends BluetoothCommunicatorBroadcastReceiver
    {
        BluetoothStateChangeReceiver()
        {
            super(BluetoothAdapter.ACTION_STATE_CHANGED);
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, UNKNOWN_STATE);
            Logger.log(this, "Received intent %s with state %d", intent, state);
            switch (state)
            {
            case BluetoothAdapter.STATE_ON:
                Logger.log(this, "Bluetooth has been connected");
                if (m_callback != null)
                {
                    m_callback.onBluetoothEnabled();
                }
                break;
            case BluetoothAdapter.STATE_OFF:
                Logger.log(this, "Bluetooth has been disconnected");
                if (m_callback != null)
                {
                    m_callback.onBluetoothDisabled();
                }
                break;
            }
        }

        private static final int UNKNOWN_STATE = -1;
    }

    private final Context                                      m_context;
    private final List<BluetoothCommunicatorBroadcastReceiver> m_receivers;
    private       BluetoothAdapter                             m_btAdapter;
    private       BluetoothCommunicatorCallback                m_callback;
}
