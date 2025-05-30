package com.example.worldmap;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.util.Log;

public class MapAccessibilityService extends AccessibilityService {

    private static final String TAG = "MapAccessibilityService";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            // This method is called when an accessibility event occurs
            Log.d(TAG, "Accessibility event: " + event.toString());

            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED) {

                CharSequence className = event.getClassName();
                CharSequence packageName = event.getPackageName();

                if (className != null && packageName != null) {
                    String windowTitle = className.toString();
                    String packageNameStr = packageName.toString();

                    // Check if the window is from our app
                    if ("com.example.worldmap".equals(packageNameStr)) {
                        Log.d(TAG, "App window event - Package: " + packageNameStr + ", Class: " + windowTitle);
                        MainActivity.updateWindowState(windowTitle, packageNameStr);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onAccessibilityEvent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {
        // This method is called when the service is interrupted
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        try {
            // Set this service in MainActivity for use
            MainActivity.set_accessibility_service(this);

            // Notify user that the accessibility service is activated
            Toast.makeText(this, "Map Accessibility Service Activated", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Accessibility service connected and activated");
        } catch (Exception e) {
            Log.e(TAG, "Error in onServiceConnected: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Accessibility service destroyed");
    }
}