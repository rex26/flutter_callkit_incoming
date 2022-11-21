package com.hiennv.flutter_callkit_incoming;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.List;

public class AppUtils {
    /**
     * 提交代码后，设置为false
     */
    private final static boolean isPrintLog = false;

    /**
     * Identify if the application is currently in a state where user interaction is possible. This
     * method is called when a remote message is received to determine how the incoming message should
     * be handled.
     *
     * @param context context.
     * @return True if the application is currently in a state where user interaction is possible,
     * false otherwise.
     */
    static boolean isApplicationForeground(Context context) {
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            return false;
        }

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }

        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }

        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isFlutterAppKilled() {
        FlutterCallkitIncomingPlugin plugin = FlutterCallkitIncomingPlugin.Companion.getInstance();
        if (null != plugin) {
            return plugin.isMainActivityKilled();
        }
        logger("null == plugin");
        return true;
    }

    static void logger(String msg) {
        if (isPrintLog) {
            Log.e("callkit", msg);
        }
    }

    static void logger(String msg, Throwable tr) {
        if (isPrintLog) {
            Log.e("callkit", msg, tr);
        }
    }

    public static boolean isPad(Context context) {
        boolean isPad = (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y);
        return isPad || screenInches >= 7.0;
    }

}
