package com.example.worldmap;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.util.Log;


public class MapAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // This method is called when an accessibility event occurs
        Log.d("EEEEEEE", "EVENTSSSS "+event);
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED  || event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED) {
            String windowTitle = event.getClassName().toString();
            String packageName = event.getPackageName().toString();
            // Check if the window is MainActivity
            if ("com.example.worldmap".equals(packageName)) {
                Log.d("AccessibilityService2", "PACKAGENAME " + packageName + "WINDOW TITLE " + windowTitle);
                MainActivity.updateWindowState(windowTitle, packageName);
            }
        }
    }



    @Override
    public void onInterrupt() {
        // This method is called when the service is interrupted
        // Implement logic to handle interruptions, if needed
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        // Set this service in MainActivity for use
        MainActivity.set_accessibility_service(this);

        // Notify user that the accessibility service is activated
        Toast.makeText(this, "Accessibility Service Activated", Toast.LENGTH_SHORT).show();
    }
}
