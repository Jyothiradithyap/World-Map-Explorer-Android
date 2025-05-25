

package com.example.worldmap;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.os.Handler;
import android.view.ViewConfiguration;

import java.util.HashMap;

public class GestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {

    public enum GestureAction {
        SWIPE_LEFT,
        SWIPE_RIGHT,
        SWIPE_UP,
        SWIPE_DOWN,
        SINGLE_TAP,
        DOUBLE_TAP,
        TRIPLE_TAP,
        LONG_PRESS,
        TWO_FINGER_TAP,
        THREE_FINGER_TAP,
        TWO_FINGER_DOUBLE_TAP,
        TWO_FINGER_SWIPE_UP,
        TWO_FINGER_SWIPE_DOWN,
        TWO_FINGER_SWIPE_RIGHT,
        TWO_FINGER_SWIPE_LEFT,
        PINCH_IN,
        PINCH_OUT,
        SWIPE_UP_DOWN,
        SWIPE_DOWN_UP,
        SWIPE_LEFT_RIGHT,
        SWIPE_RIGHT_LEFT,
        SWIPE_CIRCLE,
        FOUR_FINGER_TAP,
        FOUR_FINGER_DOUBLE_TAP
    }

    private static final int SWIPE_THRESHOLD = 500;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private GestureListener listener;
    private GestureDetector gestureDetector;
    private HashMap<GestureAction, Runnable> gestureActions;
    private GestureCallback callback;
    private final Handler handler = new Handler();
    private int numberOfTaps = 0;
    private long lastTapTimeMs = 0;
    private long touchDownMs = 0;
    private float tapTimeoutMultiplier = 1.0f;
    private boolean manageInActionDown = false;

    private Context context; // Add a field to store the Context
    public void setGestureActionListeners(HashMap<GestureAction, Runnable> gestureActionMap) {
        this.gestureActions = gestureActionMap;
    }
    public GestureListener(Context context) {
        this.context = context; // Save the Context
        gestureDetector = new GestureDetector(context, this);
    }

    public interface GestureCallback {
        void onGestureDetected(GestureAction action);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Ignore multi-touch events (pinch gestures)
        if (event.getPointerCount() > 1) {
            return true; // Consume the event, disabling pinch detection
        }
        if (manageInActionDown) {
            handleTouchDownManagement(v, event);
        } else {
            handleTouchUpManagement(v, event);
        }
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                callback.onGestureDetected(diffX > 0 ? GestureAction.SWIPE_RIGHT : GestureAction.SWIPE_LEFT);
                return true;
            }
        } else {
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                callback.onGestureDetected(diffY > 0 ? GestureAction.SWIPE_DOWN : GestureAction.SWIPE_UP);
                return true;
            }
        }
        return false;
    }


    private void handleTouchDownManagement(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownMs = System.currentTimeMillis();
            handler.removeCallbacksAndMessages(null);

            if (numberOfTaps > 0 && (System.currentTimeMillis() - lastTapTimeMs) < ViewConfiguration.getTapTimeout() * tapTimeoutMultiplier) {
                numberOfTaps++;
            } else {
                numberOfTaps = 1;
            }

            lastTapTimeMs = System.currentTimeMillis();

            if (numberOfTaps > 0) {
                MotionEvent finalMotionEvent = MotionEvent.obtain(event);
                handler.postDelayed(() -> detectMultipleTaps(finalMotionEvent, numberOfTaps),
                        (long) (ViewConfiguration.getDoubleTapTimeout() * tapTimeoutMultiplier));
            }
        }
    }

    private void handleTouchUpManagement(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownMs = System.currentTimeMillis();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            handler.removeCallbacksAndMessages(null);

            if ((System.currentTimeMillis() - touchDownMs) > ViewConfiguration.getTapTimeout()) {
                numberOfTaps = 0;
                lastTapTimeMs = 0;
                return;
            }

            if (numberOfTaps > 0 && (System.currentTimeMillis() - lastTapTimeMs) < ViewConfiguration.getDoubleTapTimeout()) {
                numberOfTaps++;
            } else {
                numberOfTaps = 1;
            }

            lastTapTimeMs = System.currentTimeMillis();

            if (numberOfTaps > 0) {
                MotionEvent finalMotionEvent = MotionEvent.obtain(event);
                handler.postDelayed(() -> detectMultipleTaps(finalMotionEvent, numberOfTaps),
                        ViewConfiguration.getDoubleTapTimeout());
            }
        }
    }

    private void detectMultipleTaps(MotionEvent event, int tapCount) {
        switch (tapCount) {
            case 2:
                callback.onGestureDetected(GestureAction.DOUBLE_TAP);
                break;
            case 3:
                callback.onGestureDetected(GestureAction.TRIPLE_TAP);
                break;
            default:
                break;
        }
        numberOfTaps = 0;
    }
}




//public class GestureListner implements View.OnTouchListener, GestureDetector.OnGestureListener {
//
//    private void detectOneFingerSwipe(MotionEvent event){
//        float diffX = event.getX() - startX1;
//        float diffY = event.getY() - startY1;
//
//        if(Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(diffX) > Math.abs(diffY)){
//            if (diffX > 0){
//                runGestureAction(GestureAction.SWIPE_RIGHT);
//            } else {
//                runGestureAction(GestureAction.SWIPE_LEFT);
//            }
//        } else if (Math.abs(diffY) > SWIPE_THREASHOLD && Math.abs(diffY) > Math.abs(diffX)){
//            if (diffY>0){
//                runGestureAction(GestureAction.SWIPE_DOWN);
//            } else if (diffY<0) {
//                runGestureAction(GestureAction.SWIPE_UP)
//            }
//        }
//    }
//}
