package com.home_automation.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.home_automation.BluetoothCommunicator;
import com.home_automation.Logger;
import com.home_automation.app.R;

import java.util.HashMap;
import java.util.Map;

public class BluetoothActivity extends Activity
{
    static void requestEnableBluetooth(Activity activity)
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    public BluetoothActivity()
    {
        m_callback = new BluetoothCommunicatorCallback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_communicator);
        m_btCommunicator = new BluetoothCommunicator(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        m_btCommunicator.open(m_callback);

        if (!m_btCommunicator.isBluetoothSupported())
        {
            new BluetoothNotSupportedDialog().show(getFragmentManager(), null);
        }
        else if (!m_btCommunicator.isBluetoothEnabled())
        {
            requestEnableBluetooth(this);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        m_btCommunicator.open(m_callback);

        if (m_btCommunicator.isBluetoothEnabled())
        {
            showContainerFragment(HOME_FRAGMENT_TAG);
        }
        else
        {
            showContainerFragment(NO_BLUETOOTH_FRAGMENT_TAG);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        m_btCommunicator.close();
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

    private class BluetoothCommunicatorCallback extends BluetoothCommunicator.BluetoothCommunicatorCallback
    {
        @Override
        protected void onBluetoothEnabled()
        {
            showContainerFragment(HOME_FRAGMENT_TAG);
        }

        @Override
        protected void onBluetoothDisabled()
        {
            showContainerFragment(NO_BLUETOOTH_FRAGMENT_TAG);
        }
    }

    private static final int                REQUEST_ENABLE_BT         = 0;
    private static final String             HOME_FRAGMENT_TAG         = "home_fragment";
    private static final String             NO_BLUETOOTH_FRAGMENT_TAG = "no_bluetooth_fragment";
    private static final Map<String, Class> FRAGMENT_CLASSES          = new HashMap<String, Class>();

    static
    {
        FRAGMENT_CLASSES.put(HOME_FRAGMENT_TAG, BluetoothDeviceNamesFragment.class);
        FRAGMENT_CLASSES.put(NO_BLUETOOTH_FRAGMENT_TAG, BluetoothNotEnabledFragment.class);
    }

    private final BluetoothCommunicatorCallback m_callback;
    private       BluetoothCommunicator         m_btCommunicator;
}
