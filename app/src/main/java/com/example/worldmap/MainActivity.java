package com.example.worldmap;

import android.os.Looper;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static MainActivity instance; // Static reference to the MainActivity instance
    private String TAG = MainActivity.class.getSimpleName();
    String currentUrl;
    //  Variable to check if explorebytouch is enabled
    boolean isSystemExploreByTouchEnabled;

    private static MapAccessibilityService appAccessibilityService;

    Region region = new Region();

    private static AccessibilityUtils accessibilityUtils = new AccessibilityUtils();

    private Toast backToast; // To display the warning notification

    private WebView webView;
    private TextToSpeech textToSpeech;
    GestureListener gestureListener;

    //Set up our own accessibility service
    public static void set_accessibility_service(MapAccessibilityService myAccessibilityService) {
        appAccessibilityService = myAccessibilityService;
    }

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        //If explore by touch is on and our accessibility service is not running, redirect to settings
        isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());
        setupWebView();
        setupWebViewClient();

        if (!AccessibilityUtils.isMapAccessibilityServiceRunning(getApplicationContext()) && isSystemExploreByTouchEnabled) {
            AccessibilityUtils.redirectToAccessibilitySettings(getApplicationContext());
        }
        Log.e(TAG, "Test:  onCreate....");

        setupTouchListeners();
        // Start monitoring accessibility changes
        monitorAccessibilityChanges();
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public static void updateWindowState(String windowTitle, String packageName) {
        // Perform the necessary actions with the window state data
        Log.d("MainActivity0", "Window state changed - Title: " + windowTitle + ", Package: " + packageName);
        MainActivity activityInstance = MainActivity.getInstance();
        if (activityInstance != null) { // Ensure the instance is not null
            Log.e("MainActivity1", "MainActivity1 instance is not null!");
            activityInstance.setAccessibility();
        } else {
            Log.e("MainActivity3", "MainActivity3 instance is null!");
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        webView = findViewById(R.id.myWeb);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 14;WME-ANDROID)");
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.loadUrl("https://test.zendalona.com/");
        webView.setWebChromeClient(new webViewChromeClient(this));
    }

    private void setupWebViewClient() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.e(TAG, "onPageFinished: " + url);
                handleWebPageLoad();
                isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());
                if (!AccessibilityUtils.isMapAccessibilityServiceRunning(getApplicationContext()) && isSystemExploreByTouchEnabled) {
                    AccessibilityUtils.redirectToAccessibilitySettings(getApplicationContext());
                }
            }
        });
    }

    private void monitorAccessibilityChanges() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            accessibilityManager.addTouchExplorationStateChangeListener(new AccessibilityManager.TouchExplorationStateChangeListener() {
                @Override
                public void onTouchExplorationStateChanged(boolean enabled) {
                    if (enabled) {
                        Log.d(TAG, "Touch exploration is enabled (TalkBack is ON)");
                        handleTouchExplorationEnabled();
                    } else {
                        Log.d(TAG, "Touch exploration is disabled (TalkBack is OFF)");
                        handleTouchExplorationDisabled();
                    }
                }
            });
        }
    }

    private void handleTouchExplorationEnabled() {
        isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());
        if (!AccessibilityUtils.isMapAccessibilityServiceRunning(getApplicationContext()) && isSystemExploreByTouchEnabled) {
            AccessibilityUtils.redirectToAccessibilitySettings(getApplicationContext());
        }
    }

    private void handleTouchExplorationDisabled() {
        // Switch to Normal Mode: Revert the UI and controls to the regular mode
        Log.d(TAG, "Switching to Normal Mode...");
        if (webView != null) {
            webView.evaluateJavascript("var isAndroidScreenReaderOn = false;", null);
            webView.setOnTouchListener(null);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListeners() {
        try {
            HashMap<GestureListener.GestureAction, Runnable> gestureActionMap = new HashMap<>();
            gestureActionMap.put(GestureListener.GestureAction.SINGLE_TAP, this::dummyFunforSingleTap);
            gestureActionMap.put(GestureListener.GestureAction.SCROLL_UP, this::moveCursorUp);
            gestureActionMap.put(GestureListener.GestureAction.SCROLL_DOWN, this::moveCursorDown);
            gestureActionMap.put(GestureListener.GestureAction.SCROLL_LEFT, this::moveCursorLeft);
            gestureActionMap.put(GestureListener.GestureAction.SCROLL_RIGHT, this::moveCursorRight);
            gestureActionMap.put(GestureListener.GestureAction.DOUBLE_TAP, this::announceCurrentLocation);

            gestureListener = new GestureListener(getApplicationContext());
            gestureListener.setGestureActionListeners(gestureActionMap);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up touch listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void dummyFunforSingleTap() {
        Log.d(TAG, "Single tap detected");
    }

    private class webViewChromeClient extends WebChromeClient {
        private final Activity activity;

        public webViewChromeClient(Activity activity) {
            this.activity = activity;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                    .setOnCancelListener(dialog -> result.cancel()) // Ensure result is canceled on dialog cancel
                    .show();
            return true;
        }
    }

    private void handleWebPageLoad() {
        Log.d("handleWebPageLoad", "handleWebPageLoad ");
        setAccessibility();
        clickEventListener();
    }

    private void setAccessibility() {
        try {
            // Determine if TalkBack or another accessibility service is enabled
            isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());

            // Safely get current URL
            if (webView != null) {
                webView.setOnTouchListener(null);

                if (isSystemExploreByTouchEnabled) {
                    webView.evaluateJavascript("var isAndroidScreenReaderOn = true;", null);
                    disableExploreByTouch();
                    webView.setOnTouchListener(gestureListener);
                    disableClickEvent();

                    // 🔒 Disable default map gestures
                    String jsDisableMapGestures =
                            "javascript:(function() {" +
                                    "  try {" +
                                    "    if (window.map && typeof window.map.dragging !== 'undefined') window.map.dragging.disable();" +
                                    "    if (window.map && typeof window.map.touchZoom !== 'undefined') window.map.touchZoom.disable();" +
                                    "    if (window.map && typeof window.map.scrollWheelZoom !== 'undefined') window.map.scrollWheelZoom.disable();" +
                                    "    if (window.map && typeof window.map.doubleClickZoom !== 'undefined') window.map.doubleClickZoom.disable();" +
                                    "    if (window.map && typeof window.map.boxZoom !== 'undefined') window.map.boxZoom.disable();" +
                                    "    if (window.map && typeof window.map.keyboard !== 'undefined') window.map.keyboard.disable();" +
                                    "    console.log('Map normal gestures disabled');" +
                                    "  } catch (e) { console.log('Error disabling map gestures: ' + e); }" +
                                    "})();";
                    webView.evaluateJavascript(jsDisableMapGestures, null);

                } else {
                    webView.evaluateJavascript("var isAndroidScreenReaderOn = false;", null);
                    webView.setOnTouchListener(null);
                    enableClickEvent();

                    // ✅ Re-enable map gestures when TalkBack is OFF
                    String jsEnableMapGestures =
                            "javascript:(function() {" +
                                    "  try {" +
                                    "    if (window.map && typeof window.map.dragging !== 'undefined') window.map.dragging.enable();" +
                                    "    if (window.map && typeof window.map.touchZoom !== 'undefined') window.map.touchZoom.enable();" +
                                    "    if (window.map && typeof window.map.scrollWheelZoom !== 'undefined') window.map.scrollWheelZoom.enable();" +
                                    "    if (window.map && typeof window.map.doubleClickZoom !== 'undefined') window.map.doubleClickZoom.enable();" +
                                    "    if (window.map && typeof window.map.boxZoom !== 'undefined') window.map.boxZoom.enable();" +
                                    "    if (window.map && typeof window.map.keyboard !== 'undefined') window.map.keyboard.enable();" +
                                    "    console.log('Map gestures re-enabled');" +
                                    "  } catch (e) { console.log('Error enabling map gestures: ' + e); }" +
                                    "})();";
                    webView.evaluateJavascript(jsEnableMapGestures, null);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in setAccessibility: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void enableExploreByTouch() {
        try {
            // Check if the Android version is R (API 30) or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && appAccessibilityService != null) {
                // Clear the touch exploration and gesture detection region
                region.setEmpty();
                appAccessibilityService.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, region);
                appAccessibilityService.setGestureDetectionPassthroughRegion(Display.DEFAULT_DISPLAY, region);
                Log.e(TAG, "Explore by Touch: enabled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling explore by touch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disableExploreByTouch() {
        try {
            // Check if the Android version is R (API 30) or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && appAccessibilityService != null) {
                // Set the touch exploration and gesture detection regions to cover the full screen
                Region fullScreenRegion = getRegionOfFullScreen(getApplicationContext());
                if (fullScreenRegion != null && !fullScreenRegion.isEmpty()) {
                    appAccessibilityService.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, fullScreenRegion);
                    appAccessibilityService.setGestureDetectionPassthroughRegion(Display.DEFAULT_DISPLAY, fullScreenRegion);
                    Log.e(TAG, "Explore by Touch disabled");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling explore by touch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disableClickEvent() {
        try {
            String jsCode = "javascript:(function() { " +
                    "function disableClick(event) {" +
                    "    event.preventDefault();" +
                    "    event.stopPropagation();" +
                    "}" +
                    "var elements = [" +
                    "    'help-btn'," +
                    "    'search-input'," +
                    "    'searchbutton'" +
                    "];" +
                    "elements.forEach(function(id) {" +
                    "    var element = document.getElementById(id);" +
                    "    if (element) {" +
                    "        element.addEventListener('click', disableClick, true);" +
                    "    }" +
                    "});" +
                    "var controlsBox = document.querySelector('#controls-box');" +
                    "if (controlsBox) controlsBox.addEventListener('click', disableClick, true);" +
                    "var leafletLink = document.querySelector('a[href=\"https://leafletjs.com\"]');" +
                    "if (leafletLink) leafletLink.addEventListener('click', disableClick, true);" +
                    "var osmLink = document.querySelector('a[href=\"https://openstreetmap.org/copyright\"]');" +
                    "if (osmLink) osmLink.addEventListener('click', disableClick, true);" +
                    "window.disableClick = disableClick; " +
                    "})()";

            if (webView != null) {
                webView.evaluateJavascript(jsCode, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error disabling click events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enableClickEvent() {
        try {
            String jsCode = "javascript:(function() { " +
                    "if (typeof window.disableClick === 'function') {" +
                    "    var elements = [" +
                    "        'help-btn'," +
                    "        'search-input'," +
                    "        'searchbutton'" +
                    "    ];" +
                    "    elements.forEach(function(id) {" +
                    "        var element = document.getElementById(id);" +
                    "        if (element) {" +
                    "            element.removeEventListener('click', window.disableClick, true);" +
                    "        }" +
                    "    });" +
                    "    var controlsBox = document.querySelector('#controls-box');" +
                    "    if (controlsBox) controlsBox.removeEventListener('click', window.disableClick, true);" +
                    "    var leafletLink = document.querySelector('a[href=\"https://leafletjs.com\"]');" +
                    "    if (leafletLink) leafletLink.removeEventListener('click', window.disableClick, true);" +
                    "    var osmLink = document.querySelector('a[href=\"https://openstreetmap.org/copyright\"]');" +
                    "    if (osmLink) osmLink.removeEventListener('click', window.disableClick, true);" +
                    "}" +
                    "})()";

            if (webView != null) {
                webView.evaluateJavascript(jsCode, null);
                Log.e(TAG, "EnableClickEvent executed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enabling click events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clickEventListener() {
        try {
            if (webView == null) {
                Log.e(TAG, "WebView is null. Cannot set up click event listener.");
                return;
            }

            String javascript = "javascript:(function() {" +
                    "    if (!document.body.hasAttribute('click-listener-added')) {" +
                    "        document.body.addEventListener('click', function(event) {" +
                    "            var element = event.target;" +
                    "            var elementId = element.id;" +
                    "            if (elementId && typeof Android !== 'undefined' && Android.onElementClicked) {" +
                    "                Android.onElementClicked(elementId);" +
                    "            }" +
                    "        }, false);" +
                    "        document.body.setAttribute('click-listener-added', 'true');" +
                    "    }" +
                    "})()";

            webView.evaluateJavascript(javascript, null);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click event listener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Region getRegionOfFullScreen(Context context) {
        try {
            DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            if (displayManager == null) {
                Log.e(TAG, "DisplayManager is null");
                return new Region(); // Return an empty region if DisplayManager is not available
            }

            Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
            if (display == null) {
                Log.e(TAG, "Display is null");
                return new Region(); // Return an empty region if the display is not available
            }

            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);

            return new Region(0, 0, metrics.widthPixels, metrics.heightPixels);
        } catch (Exception e) {
            Log.e(TAG, "Error getting full screen region: " + e.getMessage());
            e.printStackTrace();
            return new Region();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        Log.e(TAG, "Test:  onPause....");

        try {
            // If TalkBack is enabled, enable explore by touch
            if (isSystemExploreByTouchEnabled) {
                enableExploreByTouch();
            }

            // Enable click events on the WebView
            enableClickEvent();

            // Allow the screen to turn off again
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "Test:  onStop....");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "Test:  onRestart....");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "Test:  onDestroy....");

        // Clean up
        if (webView != null) {
            webView.setOnTouchListener(null);
        }
        gestureListener = null;
        instance = null;
    }

    // Inject JavaScript to simulate "ArrowUp" key press
    private void moveCursor(String direction) {
        try {
            String jsCode = "javascript:(function() {" +
                    "  try {" +
                    "    const event = new KeyboardEvent('keydown', {" +
                    "      key: '" + direction + "'," +
                    "      code: '" + direction + "'," +
                    "      shiftKey: false," +
                    "      bubbles: true," +
                    "      cancelable: true" +
                    "    });" +
                    "    " +
                    "    const container = document.querySelector('.leaflet-container');" +
                    "    if (container) {" +
                    "      container.dispatchEvent(event);" +
                    "      return 'success';" +
                    "    }" +
                    "    return 'container_not_found';" +
                    "  } catch(e) { " +
                    "    return 'error: ' + e.message;" +
                    "  }" +
                    "})();";

            if (webView != null) {
                webView.evaluateJavascript(jsCode, value -> {
                    if (value != null) {
                        Log.d(TAG, "Move result: " + value);
                        if (value.contains("container_not_found")) {
                            Log.e(TAG, "Map container not found");
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in moveCursor: " + e.getMessage());
        }
    }

    private void moveCursorUp() {
        moveCursor("ArrowUp");
    }

    private void moveCursorDown() {
        moveCursor("ArrowDown");
    }

    private void moveCursorLeft() {
        moveCursor("ArrowLeft");
    }

    private void moveCursorRight() {
        moveCursor("ArrowRight");
    }


    public void announceCurrentLocation() {
        String jsCode = "javascript:(function() {" +
                "  try {" +
                "    const event = new KeyboardEvent('keydown', {" +
                "      key: 'f'," +
                "      code: 'KeyF'," +
                "      keyCode: 70," +
                "      which: 70," +
                "      shiftKey: false," +
                "      bubbles: true," +
                "      cancelable: true" +
                "    });" +
                "    const container = document.querySelector('.leaflet-container');" +
                "    if (container) {" +
                "      container.dispatchEvent(event);" +
                "      return 'success';" +
                "    }" +
                "    return 'container_not_found';" +
                "  } catch(e) { " +
                "    return 'error: ' + e.message;" +
                "  }" +
                "})();";

        if (webView != null) {
            webView.evaluateJavascript(jsCode, value -> {
                if (value != null) {
                    Log.d("WebView", "Announce result: " + value);
                    if (value.contains("container_not_found")) {
                        Log.e("WebView", "Map container not found");
                    } else if (value.contains("error")) {
                        Log.e("WebView", "JS error occurred: " + value);
                    }
                }
            });
        }
    }






}