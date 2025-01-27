package com.example.worldmap;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class MapAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Handle Accessibility Events (e.g., gestures, spoken feedback)
    }

    @Override
    public void onInterrupt() {
        // Handle interruptions (like calls or notifications)
    }

    protected void onServiceConnected(){
        super.onServiceConnected();

        MainActivity.set_accessibility_service(this);
        Toast.makeText(this,"Accessibility Service Activated",Toast.LENGTH_SHORT).show();
    }
}
