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

    private static MainActivity instance; // Static reference to the MainActivity instance hhh
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
        instance = this;//hh
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
            Log.e("MainActivity1", "MainActivity1 instance is null!");
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
        webView.loadUrl("https://map.zendalona.com/");
        webView.setWebChromeClient(new webViewChromeClient(this));
    }


    private void setupWebViewClient() {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.e(TAG, "onPageFinished: " + url);
                isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());
                if (!AccessibilityUtils.isMapAccessibilityServiceRunning(getApplicationContext()) && isSystemExploreByTouchEnabled) {
                    AccessibilityUtils.redirectToAccessibilitySettings(getApplicationContext());
                }
                //Injected Javascript to monitor disclaimer visibility
                webView.evaluateJavascript(
                        "(function() {" +
                                "let maxAttempts = 50;" +
                                "let interval = setInterval(() => {" +
                                "let btn = document.getElementById('close-button');" +
                                "if (btn) {" +
                                "btn.click();" +
                                "clearInterval(interval);" +
                                "console.log('[Disclaimer] Close button clicked');" +
                                "} else if (--maxAttempts <= 0) {" +
                                "clearInterval(interval);" +
                                "console.log('[Disclaimer] Close button not found');" +
                                "}" +
                                "}, 100);" +
                                "})();",
                        value->Log.e("Descmailmer","Desclaimer Closed")
                );

            }
        });
    }


    private void monitorAccessibilityChanges() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
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


    private void handleTouchExplorationEnabled() {
        isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());
        if (!AccessibilityUtils.isMapAccessibilityServiceRunning(getApplicationContext()) && isSystemExploreByTouchEnabled) {
            AccessibilityUtils.redirectToAccessibilitySettings(getApplicationContext());
        }
    }

    private void handleTouchExplorationDisabled() {
        // Switch to Normal Mode: Revert the UI and controls to the regular mode
        Log.d(TAG, "Switching to Normal Mode...");
        webView.evaluateJavascript("var isAndroidScreenReaderOn = false;", null);
        //enableClickEvent();
        webView.setOnTouchListener(null);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListeners() {

        HashMap<GestureListener.GestureAction, Runnable> gestureActionMap = new HashMap<>();
        gestureActionMap.put(GestureListener.GestureAction.SINGLE_TAP, this::dummyFunforSingleTap);

        //gestureActionMap.put(GestureListener.GestureAction.DOUBLE_TAP, this::moveCursorUp);
        gestureActionMap.put(GestureListener.GestureAction.SWIPE_UP, this::moveCursorUp);
        gestureActionMap.put(GestureListener.GestureAction.SWIPE_DOWN, this::moveCursorDown);
        //gestureActionMap.put(GestureListener.GestureAction.SWIPE_LEFT, this::announceLastGamePlayed);
        //gestureActionMap.put(GestureListener.GestureAction.SWIPE_RIGHT, this::sendVoiceRecording);


        gestureListener = new GestureListener(getApplicationContext());
        gestureListener.setGestureActionListeners(gestureActionMap);

    }
    private void dummyFunforSingleTap(){

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
        setAccessibility();
        clickEventListener();

    }


    private void setAccessibility() {
        try {
            // Determine if TalkBack or another accessibility service is enabled
            isSystemExploreByTouchEnabled = accessibilityUtils.isSystemExploreByTouchEnabled(getApplicationContext());
            // Retrieve the current URL from the WebView history
            WebBackForwardList webBackForwardList = webView.copyBackForwardList();
            currentUrl = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex()).getTitle();
            Log.e(TAG, "Current URL: " + currentUrl);
            Log.e("AAAAsetAccessibility", "setAccessibility" + currentUrl);

            webView.setOnTouchListener(null);

            // Update the JavaScript context and adjust touch exploration based on URL and TalkBack status
            if (isSystemExploreByTouchEnabled) {
                if (currentUrl.contains("map")) {
                    webView.evaluateJavascript("var isAndroidScreenReaderOn = true;", null);
                    disableExploreByTouch();
                    webView.setOnTouchListener(gestureListener);
                    disableClickEvent();
                    hideSystemUI();

                } else {
                    webView.setOnTouchListener(null);
                    enableExploreByTouch();
                    enableClickEvent();
                    showSystemUI();
                }
            } else {
                webView.evaluateJavascript("var isAndroidScreenReaderOn = false;", null);
                webView.setOnTouchListener(null);
                enableClickEvent();
                showSystemUI();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void enableExploreByTouch() {
        try {
            // Check if the Android version is R (API 30) or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Clear the touch exploration and gesture detection region
                region.setEmpty();
                appAccessibilityService.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, region);
                appAccessibilityService.setGestureDetectionPassthroughRegion(Display.DEFAULT_DISPLAY, region);
                Log.e(TAG, "Explore by Touch: false");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void disableExploreByTouch() {
        try {
            // Check if the Android version is R (API 30) or higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Set the touch exploration and gesture detection regions to cover the full screen
                appAccessibilityService.setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, getRegionOfFullScreen(getApplicationContext()));
                appAccessibilityService.setGestureDetectionPassthroughRegion(Display.DEFAULT_DISPLAY, getRegionOfFullScreen(getApplicationContext()));
                Log.e(TAG, "Explore by Touch disabled");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void disableClickEvent() {
        //Todo Write the javascript code to disable the search bar and all the buttons
        String jsCode = "javascript:(function() { " +
                "function disableClick(event) {" +
                "    event.preventDefault();" +
                "    event.stopPropagation();" +
                "}" +
                "document.getElementById('help-btn').addEventListener('click', enableClick, true); " +
                "document.getElementById('search-input').addEventListener('click', enableClick, true); " +
                "document.getElementById('searchbutton').addEventListener('click', enableClick, true); " +
                "document.getElementById('fas fa-directions').addEventListener('click', enableClick, true); " +
                "document.querySelector('#controls-box').addEventListener('click', enableClick, true); " +
                "document.querySelector('a[href=\\\"https://leafletjs.com\\\"]').addEventListener('click', enableClick, true); " +
                "document.querySelector('a[href=\\\"https://openstreetmap.org/copyright\\\"]').addEventListener('click', enableClick, true); " +
                "window.enableClick = enableClick; " +
                "})()";
    }


    private void enableClickEvent() {
        //Todo write the javascript code to enable the search bar and all the buttons.
        String jsCode = "javascript:(function() { " +
                "document.getElementById('help-btn').removeEventListener('click', window.disableClick, true); " +
                "document.getElementById('search-input').removeEventListener('click', window.disableClick, true); " +
                "document.getElementById('search-button').removeEventListener('click', window.disableClick, true); " +
                "document.getElementById('fas fa-directions').removeEventListener('click', window.disableClick, true); " +
                "document.querySelector('#controls-box').removeEventListener('click', window.disableClick, true); " +
                "document.querySelector('a[href=\"https://leafletjs.com\"]').removeEventListener('click', window.disableClick, true); " +
                "document.querySelector('a[href=\"https://openstreetmap.org/copyright\"]').removeEventListener('click', window.disableClick, true); " +
                "window.disableClick = disableClick; " +
                "})()";
        webView.evaluateJavascript(jsCode, null);
        Log.e(TAG, "EnableClickEvent: " );
    }


    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }


    private Region getRegionOfFullScreen(Context context) {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            return new Region(); // Return an empty region if DisplayManager is not available
        }
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) {
            return new Region(); // Return an empty region if the display is not available
        }

        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);

        return new Region(0, 0, metrics.widthPixels, metrics.heightPixels);
    }


    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        Log.e(TAG, "Test:  onPause....");

        // If TalkBack is enabled, disable explore by touch
        if (isSystemExploreByTouchEnabled) {
            enableExploreByTouch();
        }
//        // Enable click events on the WebView
        enableClickEvent();
        // Allow the screen to turn off again
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
    }



    // Inject JavaScript to simulate "ArrowUp" key press
    private void moveCursorUp() {
        String jsCode = "javascript:(function() {" +
                "  const event = { key: 'ArrowUp', code: 'ArrowUp', shiftKey: false };" +
                "  if (typeof currentMarker?.InboundMarkerMove === 'function') {" +
                "    currentMarker.InboundMarkerMove(event);" +
                "  } else if (typeof currentMarker?.markerMove === 'function') {" +
                "    currentMarker.markerMove(event);" +
                "  }" +
                "})();";
        webView.evaluateJavascript(jsCode, null);
    }

    private void moveCursorDown() {
        String jsCode = "javascript:(function() {" +
                "  const event = { key: 'ArrowDown', code: 'ArrowDown', shiftKey: false };" +
                "  if (typeof currentMarker?.InboundMarkerMove === 'function') {" +
                "    currentMarker.InboundMarkerMove(event);" +
                "  } else if (typeof currentMarker?.markerMove === 'function') {" +
                "    currentMarker.markerMove(event);" +
                "  }" +
                "})();";
        webView.evaluateJavascript(jsCode, null);
    }

    private void moveCursorLeft() {
        String jsCode = "javascript:(function() {" +
                " const event = { key: 'ArrowLeft', code: 'ArrowLeft', shiftKey: false}" +
                " if (typeof currentMarker?.InboundMarkerMove === 'function') {" +
                "   currentMarker.InboundMarkerMove(event);" +
                " } else if (typeof currentMarker?.markerMove === 'function') {" +
                "      currentMarker.markerMove(event);" +
                " }" +
                "})()";
    }

    private void moveCursorRight() {
        String jsCode = "javascript:(function() {" +
                "  const event = { key: 'ArrowRight', code: 'ArrowRight', shiftKey: false };" +
                "  if (typeof currentMarker?.InboundMarkerMove === 'function') {" +
                "    currentMarker.InboundMarkerMove(event);" +
                "  } else if (typeof currentMarker?.markerMove === 'function') {" +
                "    currentMarker.markerMove(event);" +
                "  }" +
                "})();";
        webView.evaluateJavascript(jsCode, null);
    }







}