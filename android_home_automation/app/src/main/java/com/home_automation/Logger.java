package com.home_automation;

import android.util.Log;

/**
 * Utility for logging to Android logcat
 */
public final class Logger
{
    private static final String TAG = "HOME_AUTOMATION";

    public static void log(Object o, String format, Object... args)
    {
        String className;
        if (o instanceof Class)
        {
            className = ((Class) o).getSimpleName();
        }
        else
        {
            className = o.getClass().getSimpleName();
        }

        Log.v(TAG, String.format(String.format("%s %s", className, format), args));
    }

    private Logger()
    {
    }
}
