package com.home_automation.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * Dialog to show when Bluetooth is not supported by the device's hardware
 */
public class BluetoothNotSupportedDialog extends DialogFragment
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
                goToHome();
            }
        };
    }

    private void goToHome()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(intent);
    }

    private static final String MESSAGE = "Bluetooth is not supported on this device. Exiting...";
    private static final String OKAY    = "OK";
}

