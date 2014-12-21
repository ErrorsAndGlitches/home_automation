package com.home_automation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.home_automation.app.R;

import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends Activity
{
    public static class PlaceholderFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.fragment_bluetooth_communicator, container, false);
        }
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
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // check if bluetooth hardware is available
        m_btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (m_btAdapter == null)
        {
            new BluetoothNotSupportedDialog().show(getFragmentManager(), null);
        }
        else
        {
            // check if bluetooth is on
            if (!m_btAdapter.isEnabled())
            {
                requestEnableBluetooth(this);
            }
            else
            {
                showContainerFragment(new PlaceholderFragment(), HOME_FRAGMENT_TAG);
            }
        }
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
            showContainerFragment(new PlaceholderFragment(), HOME_FRAGMENT_TAG);
            break;
        case RESULT_CANCELED:
            Logger.log(this, "Bluetooth failed to be activated");
            showContainerFragment(new BluetoothNotEnabledFragment(), NO_BLUETOOTH_FRAGMENT_TAG);
            break;
        default:
            Logger.log(this, "Unknown result code %d", resultCode);
        }
    }

    private void showContainerFragment(Fragment frag, String tag)
    {
        // first check if the fragment is already being show and if not, clear all fragments and show fragment
        FragmentManager fragMan = getFragmentManager();
        if (fragMan.findFragmentByTag(tag) == null)
        {
            clearFragments();
            getFragmentManager().beginTransaction().add(R.id.container, frag, tag).commit();
        }
    }

    private void clearFragments()
    {
        FragmentManager fragManager = getFragmentManager();
        FragmentTransaction transaction = fragManager.beginTransaction();
        for (String fragTag : FRAGMENT_TAGS)
        {
            Fragment frag = fragManager.findFragmentByTag(fragTag);
            if (frag != null)
            {
                transaction.remove(frag);
            }
        }
        transaction.commit();
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

    private static final int          REQUEST_ENABLE_BT         = 0;
    private static final String       HOME_FRAGMENT_TAG         = "home_fragment";
    private static final String       NO_BLUETOOTH_FRAGMENT_TAG = "no_bluetooth_fragment";
    private static final List<String> FRAGMENT_TAGS             = new ArrayList<String>(2);
    static
    {
        FRAGMENT_TAGS.add(HOME_FRAGMENT_TAG);
        FRAGMENT_TAGS.add(NO_BLUETOOTH_FRAGMENT_TAG);
    }

    private BluetoothAdapter m_btAdapter;
}
