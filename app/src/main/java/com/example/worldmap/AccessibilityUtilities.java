package com.example.worldmap;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

public class AccessibilityUtilities {
    public boolean isSystemExploreByTouchEnabled(Context context){
        AccessibilityManager accessibilityManager=(AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(accessibilityManager != null){
            return accessibilityManager.isEnabled() &&accessibilityManager.isTouchExplorationEnabled();
        }
        return false;
    }

    public static boolean isMapAccessibilityServiceRunning(Context context){
        ActivityManager manager=(ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(MapAccessibilityService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void redirectToAccessibilitySettings(Context context){
        Toast.makeText(context,"Please enable Map accessibility service", Toast.LENGTH_SHORT).show();
        Intent intent=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
