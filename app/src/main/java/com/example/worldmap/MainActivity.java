package com.example.worldmap;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.speech.tts.TextToSpeech;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WebView myWeb;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWeb = findViewById(R.id.myWeb);
        myWeb.getSettings().setJavaScriptEnabled(true);
        myWeb.setWebViewClient(new WebViewClient());
        myWeb.loadUrl("https://map.zendalona.com/");

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        // Initialize GestureListener
        GestureListener gestureListener = new GestureListener(myWeb, this::handleGestureAction);
        myWeb.setOnTouchListener(gestureListener);
    }

    private void handleGestureAction(GestureListener.GestureAction action) {
        switch (action) {
            case SWIPE_LEFT:
                speak("Swiping left");
                break;
            case SWIPE_RIGHT:
                speak("Swiping right");
                break;
            case SWIPE_UP:
                speak("Swiping up");
                break;
            case SWIPE_DOWN:
                speak("Swiping down");
                break;
            case DOUBLE_TAP:
                speak("Double tapping");
                break;
            case TRIPLE_TAP:
                speak("Triple tapping");
                break;
            case LONG_PRESS:
                speak("Long press detected");
                break;
        }
    }

    private void speak(String text) {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
    private static MapAccessibilityService appAccessibilityService;

    public static void set_accessibility_service(MapAccessibilityService myAccessibilityService) {
        appAccessibilityService = myAccessibilityService;
    }
}