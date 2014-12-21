package com.home_automation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.home_automation.app.R;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BluetoothActivity extends Activity
{
    public BluetoothActivity()
    {
        m_btReceiver = new BluetoothStateChangeReceiver();
    }

    public static class BluetoothDeviceNamesFragment extends Fragment
    {
        public BluetoothDeviceNamesFragment()
        {
            m_btDeviceNames = new LinkedList<String>();
            m_deviceScanCallback = new BluetoothCommunicator.DeviceScanCallback()
            {
                @Override
                public void onDeviceDiscovered(BluetoothDevice device)
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
                public void onScanFinished()
                {
                    m_refreshButton.setEnabled(true);
                }
            };
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            View view = inflater.inflate(R.layout.fragment_bluetooth_communicator, container, false);
            ListView deviceNameList = (ListView) view.findViewById(R.id.bt_device_names);
            m_deviceNamesAdapter = new ArrayAdapter<String>(getActivity(),
                                                            android.R.layout.simple_list_item_1,
                                                            m_btDeviceNames);
            deviceNameList.setAdapter(m_deviceNamesAdapter);

            m_refreshButton = (Button) view.findViewById(R.id.refresh_button);
            m_refreshButton.setOnClickListener(getRefreshButtonOnClickListener());

            Button exitButton = (Button) view.findViewById(R.id.exit_button);
            exitButton.setOnClickListener(getExitOnClickListener());

            return view;
        }

        @Override
        public void onResume()
        {
            super.onResume();
            BluetoothCommunicator.getInstance(getActivity()).scanForDevices(m_deviceScanCallback);
        }

        @Override
        public void onPause()
        {
            super.onPause();
            BluetoothCommunicator.getInstance(getActivity()).cancelScan();
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
                    BluetoothCommunicator.getInstance(getActivity()).scanForDevices(m_deviceScanCallback);
                }
            };
        }

        private View.OnClickListener getExitOnClickListener()
        {
            return new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    android.os.Process.killProcess(Process.myPid());
                }
            };
        }

        private static String deviceToString(BluetoothDevice device)
        {
            return String.format("Name(%s) Address(%s)", device.getName(), device.getAddress());
        }

        private final List<String>                             m_btDeviceNames;
        private final BluetoothCommunicator.DeviceScanCallback m_deviceScanCallback;
        private       ArrayAdapter<String>                     m_deviceNamesAdapter;
        private       Button                                   m_refreshButton;
    }

    public static class BluetoothNotSupportedDialog extends DialogFragment
    {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(MESSAGE);
            builder.setPositiveButton(OKAY, getOkayClickListener());
            return builder.create();
        }

        private DialogInterface.OnClickListener getOkayClickListener()
        {
            return new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    goToHome(getActivity());
                }
            };
        }

        private static final String MESSAGE = "Bluetooth is not supported on this device. Exiting...";
        private static final String OKAY    = "OK";
    }

    public static class BluetoothNotEnabledFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
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
                    requestEnableBluetooth(getActivity());
                }
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_communicator);
        m_btCommunicator = BluetoothCommunicator.getInstance(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // check if bluetooth hardware is available
        if (!m_btCommunicator.isBluetoothSupported())
        {
            new BluetoothNotSupportedDialog().show(getFragmentManager(), null);
        }
        else
        {
            // check if bluetooth is on
            if (!m_btCommunicator.isBluetoothEnabled())
            {
                requestEnableBluetooth(this);
            }
            else
            {
                showContainerFragment(HOME_FRAGMENT_TAG);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(m_btReceiver, BLUETOOTH_INTENT_FILTER);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(m_btReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar
        getMenuInflater().inflate(R.menu.menu_bluetooth_communicator, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // check result if the activity was started to enable bluetooth
        switch (requestCode)
        {
        case REQUEST_ENABLE_BT:
            handleBluetoothEnableRequestResultCode(resultCode);
            break;
        default:
            Logger.log(this, "Unknown request code %d", requestCode);
        }
    }

    private void handleBluetoothEnableRequestResultCode(int resultCode)
    {
        switch (resultCode)
        {
        case RESULT_OK:
            Logger.log(this, "Bluetooth was successfully activated");
            showContainerFragment(HOME_FRAGMENT_TAG);
            break;
        case RESULT_CANCELED:
            Logger.log(this, "Bluetooth failed to be activated");
            showContainerFragment(NO_BLUETOOTH_FRAGMENT_TAG);
            break;
        default:
            Logger.log(this, "Unknown result code %d", resultCode);
        }
    }

    private void showContainerFragment(String tag)
    {
        // first check if the fragment is already being show and if not, clear all fragments and show fragment
        FragmentManager fragMan = getFragmentManager();
        if (fragMan.findFragmentByTag(tag) == null)
        {
            clearFragments();
            Class fragClass = FRAGMENT_CLASSES.get(tag);
            try
            {
                getFragmentManager().beginTransaction().add(R.id.container,
                                                            (Fragment) fragClass.newInstance(),
                                                            tag).commit();
            }
            catch (Exception e)
            {
                Logger.log(this, "Failed to create instantiation of fragment %s", fragClass.getSimpleName());
            }
        }
    }

    private void clearFragments()
    {
        FragmentManager fragManager = getFragmentManager();
        FragmentTransaction transaction = fragManager.beginTransaction();
        for (String fragTag : FRAGMENT_CLASSES.keySet())
        {
            Fragment frag = fragManager.findFragmentByTag(fragTag);
            if (frag != null)
            {
                transaction.remove(frag);
            }
        }
        transaction.commit();
    }

    private class BluetoothStateChangeReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, UNKNOWN_STATE);
            Logger.log(this, "Received intent %s with state %d", intent, state);
            switch (state)
            {
            case BluetoothAdapter.STATE_ON:
                Logger.log(this, "Bluetooth has been connected");
                showContainerFragment(HOME_FRAGMENT_TAG);
                break;
            case BluetoothAdapter.STATE_OFF:
                Logger.log(this, "Bluetooth has been disconnected");
                showContainerFragment(NO_BLUETOOTH_FRAGMENT_TAG);
                break;
            }
        }

        private static final int UNKNOWN_STATE = -1;
    }

    private static void requestEnableBluetooth(Activity activity)
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private static void goToHome(Activity activity)
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    private static final IntentFilter       BLUETOOTH_INTENT_FILTER   = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    private static final int                REQUEST_ENABLE_BT         = 0;
    private static final Map<String, Class> FRAGMENT_CLASSES          = new HashMap<String, Class>();
    private static final String             HOME_FRAGMENT_TAG         = "home_fragment";
    private static final String             NO_BLUETOOTH_FRAGMENT_TAG = "no_bluetooth_fragment";

    static
    {
        FRAGMENT_CLASSES.put(HOME_FRAGMENT_TAG, BluetoothDeviceNamesFragment.class);
        FRAGMENT_CLASSES.put(NO_BLUETOOTH_FRAGMENT_TAG, BluetoothNotEnabledFragment.class);
    }

    private final BluetoothStateChangeReceiver m_btReceiver;
    private       BluetoothCommunicator        m_btCommunicator;
}
