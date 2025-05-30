package com.example.worldmap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import android.util.Log;

public class AccessibilityUtils {

    private static final String TAG = "AccessibilityUtils";

    public boolean isSystemExploreByTouchEnabled(Context context) {
        try {
            AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (accessibilityManager != null) {
                boolean isEnabled = accessibilityManager.isEnabled();
                boolean isTouchExplorationEnabled = accessibilityManager.isTouchExplorationEnabled();
                Log.d(TAG, "Accessibility enabled: " + isEnabled + ", Touch exploration enabled: " + isTouchExplorationEnabled);
                return isEnabled && isTouchExplorationEnabled;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking touch exploration state: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isMapAccessibilityServiceRunning(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (service != null && service.service != null) {
                        String serviceName = service.service.getClassName();
                        if (MapAccessibilityService.class.getName().equals(serviceName)) {
                            Log.d(TAG, "MapAccessibilityService is running");
                            return true;
                        }
                    }
                }
            }
            Log.d(TAG, "MapAccessibilityService is not running");
        } catch (Exception e) {
            Log.e(TAG, "Error checking if accessibility service is running: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static void redirectToAccessibilitySettings(Context context) {
        try {
            Toast.makeText(context, "Please enable Map accessibility service", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Redirected to accessibility settings");
        } catch (Exception e) {
            Log.e(TAG, "Error redirecting to accessibility settings: " + e.getMessage());
            e.printStackTrace();

            // Fallback: try to open general settings
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallbackIntent);
                Toast.makeText(context, "Please navigate to Accessibility settings manually", Toast.LENGTH_LONG).show();
            } catch (Exception fallbackException) {
                Log.e(TAG, "Fallback settings intent also failed: " + fallbackException.getMessage());
                Toast.makeText(context, "Unable to open settings. Please enable accessibility service manually.", Toast.LENGTH_LONG).show();
            }
        }
    }
}