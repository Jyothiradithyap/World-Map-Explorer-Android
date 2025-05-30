package com.example.worldmap;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.HashMap;

public class GestureListener implements View.OnTouchListener,
        GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    public enum GestureAction {
        SWIPE_UP,
        SWIPE_DOWN,
        SWIPE_RIGHT,
        SWIPE_LEFT,
        TWO_FINGER_SWIPE_RIGHT,
        TWO_FINGER_SWIPE_LEFT,
        TWO_FINGER_SWIPE_UP,
        TWO_FINGER_SWIPE_DOWN,
        SINGLE_TAP,
        DOUBLE_TAP,
        TWO_FINGER_SINGLE_TAP,
        TWO_FINGER_DOUBLE_TAP,
        HOLD_LONG_PRESS,
        RELEASE_LONG_PRESS,
        SCROLL_RIGHT,
        SCROLL_LEFT,
        SCROLL_DOWN,
        SCROLL_UP,
        TWO_FINGER_SCROLL_DOWN,
        TWO_FINGER_SCROLL_UP,
    }

    private static final String TAG = "GestureListener";
    private GestureDetector gestureDetector;
    private HashMap<GestureAction, Runnable> gestureActions;
    private float startX1, startY1, startX2, startY2;
    private boolean isTwoFingerGesture = false;
    private boolean multiFingerGestureDetected = false;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final int DOUBLE_TAP_TIMEOUT = 300;
    private Handler doubleTapHandler = new Handler();
    private boolean isHoldLongPress = false;
    private static final float MOVE_THRESHOLD = 20;
    private static final int SCROLL_TIME_THRESHOLD = 350;
    private static final float SCROLL_THRESHOLD = 150;
    private static final float TWO_FINGER_SWIPE_THRESHOLD = 50.0f;
    private boolean wasScrolling = false;
    private boolean isTwoFingerTapInProgress = false;
    private long lastTwoFingerTapTime = 0;
    private long startTime; // Track gesture start time

    public void setGestureActionListeners(HashMap<GestureAction, Runnable> gestureActionMap) {
        this.gestureActions = gestureActionMap;
    }

    public GestureListener(Context context) {
        gestureDetector = new GestureDetector(context, this);
        gestureDetector.setOnDoubleTapListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Pass single-finger events to gestureDetector
        if (event.getPointerCount() == 1) {
            gestureDetector.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchStart(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 1 && !multiFingerGestureDetected) {
                    detectHorizontalScroll(event);
                } else if (event.getPointerCount() == 2 && isTwoFingerGesture) {
                    detectTwoFingerScroll(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                handleMultiFingerTouchStart(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleTouchEnd(event);
                break;
            case MotionEvent.ACTION_CANCEL:
                resetGestureState();
                break;
        }

        return true;
    }

    private void handleTouchStart(MotionEvent event) {
        startTime = System.currentTimeMillis(); // Initialize start time
        if (event.getPointerCount() == 1) {
            startX1 = event.getX();
            startY1 = event.getY();
            Log.d(TAG, "Touch started at (" + startX1 + ", " + startY1 + ")");
        }
        isTwoFingerGesture = false;
        multiFingerGestureDetected = false;
        wasScrolling = false;
    }

    private void handleTouchEnd(MotionEvent event) {
        Log.d(TAG, "Touch ended. Pointer count: " + event.getPointerCount());

        if (isHoldLongPress) {
            Log.d(TAG, "Long press released");
            runGestureAction(GestureAction.RELEASE_LONG_PRESS);
            isHoldLongPress = false;
        }

        if (isTwoFingerGesture && event.getPointerCount() == 2) {
            handleTwoFingerTap(event);
        }

        resetGestureState();
    }

    private void detectHorizontalScroll(@NonNull MotionEvent event) {
        float diffX = event.getX() - startX1;
        float diffY = event.getY() - startY1;
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        if (Math.abs(diffX) > SCROLL_THRESHOLD && Math.abs(diffX) > Math.abs(diffY)) {
            if (elapsedTime > SCROLL_TIME_THRESHOLD) {
                wasScrolling = true;
                if (diffX > 0) {
                    Log.d(TAG, "One-finger horizontal scroll: RIGHT");
                    runGestureAction(GestureAction.SCROLL_RIGHT);
                } else {
                    Log.d(TAG, "One-finger horizontal scroll: LEFT");
                    runGestureAction(GestureAction.SCROLL_LEFT);
                }
                startX1 = event.getX();
            }
        } else if (Math.abs(diffY) > SCROLL_THRESHOLD && Math.abs(diffY) > Math.abs(diffX)) {
            if (elapsedTime > SCROLL_TIME_THRESHOLD) {
                wasScrolling = true;
                if (diffY > 0) {
                    Log.d(TAG, "One-finger vertical scroll: DOWN");
                    runGestureAction(GestureAction.SCROLL_DOWN);
                } else {
                    Log.d(TAG, "One-finger vertical scroll: UP");
                    runGestureAction(GestureAction.SCROLL_UP);
                }
                startY1 = event.getY();
            }
        }
    }

    private void detectTwoFingerScroll(MotionEvent event) {
        float diffX1 = event.getX(0) - startX1;
        float diffY1 = event.getY(0) - startY1;
        float diffX2 = event.getX(1) - startX2;
        float diffY2 = event.getY(1) - startY2;
        float avgDiffX = (diffX1 + diffX2) / 2;
        float avgDiffY = (diffY1 + diffY2) / 2;

        if (Math.abs(avgDiffX) > SCROLL_THRESHOLD || Math.abs(avgDiffY) > SCROLL_THRESHOLD) {
            wasScrolling = true;
            if (Math.abs(avgDiffX) > Math.abs(avgDiffY)) {
                if (avgDiffX > 0) {
                    Log.d(TAG, "Two-finger horizontal scroll: RIGHT");
                } else {
                    Log.d(TAG, "Two-finger horizontal scroll: LEFT");
                }
            } else {
                if (avgDiffY > 0) {
                    Log.d(TAG, "Two-finger vertical scroll: DOWN");
                    runGestureAction(GestureAction.TWO_FINGER_SCROLL_DOWN);
                } else {
                    Log.d(TAG, "Two-finger vertical scroll: UP");
                    runGestureAction(GestureAction.TWO_FINGER_SCROLL_UP);
                }
            }
        }
    }

    private void handleTwoFingerTap(MotionEvent event) {
        long currentTime = System.currentTimeMillis();

        // Check if fingers didn't move much
        float diffX1 = Math.abs(event.getX(0) - startX1);
        float diffY1 = Math.abs(event.getY(0) - startY1);
        float diffX2 = Math.abs(event.getX(1) - startX2);
        float diffY2 = Math.abs(event.getY(1) - startY2);

        if (diffX1 < MOVE_THRESHOLD && diffY1 < MOVE_THRESHOLD &&
                diffX2 < MOVE_THRESHOLD && diffY2 < MOVE_THRESHOLD) {

            if (isTwoFingerTapInProgress && (currentTime - lastTwoFingerTapTime < DOUBLE_TAP_TIMEOUT)) {
                Log.d(TAG, "Two-finger double tap detected");
                runGestureAction(GestureAction.TWO_FINGER_DOUBLE_TAP);
                isTwoFingerTapInProgress = false;
            } else {
                isTwoFingerTapInProgress = true;
                lastTwoFingerTapTime = currentTime;
                doubleTapHandler.postDelayed(() -> {
                    if (isTwoFingerTapInProgress) {
                        Log.d(TAG, "Two-finger single tap detected");
                        runGestureAction(GestureAction.TWO_FINGER_SINGLE_TAP);
                        isTwoFingerTapInProgress = false;
                    }
                }, DOUBLE_TAP_TIMEOUT);
            }
        }
    }

    private void handleMultiFingerTouchStart(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 2) {
            startX2 = event.getX(1);
            startY2 = event.getY(1);
            isTwoFingerGesture = true;
            Log.d(TAG, "Two-finger touch started at (" +
                    startX1 + ", " + startY1 + ") and (" +
                    startX2 + ", " + startY2 + ")");
        }
    }

    private void resetGestureState() {
        isTwoFingerGesture = false;
        multiFingerGestureDetected = false;
        wasScrolling = false;
    }

    // GestureDetector callbacks
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (e.getPointerCount() == 1) {
            Log.d(TAG, "One-finger long press detected");
            isHoldLongPress = true;
            runGestureAction(GestureAction.HOLD_LONG_PRESS);
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) return false;

        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    Log.d(TAG, "One-finger swipe: RIGHT");
                    runGestureAction(GestureAction.SWIPE_RIGHT);
                } else {
                    Log.d(TAG, "One-finger swipe: LEFT");
                    runGestureAction(GestureAction.SWIPE_LEFT);
                }
                return true;
            }
        } else {
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    Log.d(TAG, "One-finger swipe: DOWN");
                    runGestureAction(GestureAction.SWIPE_DOWN);
                } else {
                    Log.d(TAG, "One-finger swipe: UP");
                    runGestureAction(GestureAction.SWIPE_UP);
                }
                return true;
            }
        }
        return false;
    }

    // DoubleTapListener callbacks
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(TAG, "One-finger single tap confirmed");
        runGestureAction(GestureAction.SINGLE_TAP);
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "One-finger double tap detected");
        runGestureAction(GestureAction.DOUBLE_TAP);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    private void runGestureAction(GestureAction action) {
        if (gestureActions != null && gestureActions.containsKey(action)) {
            Log.d(TAG, "Executing gesture action: " + action.name());
            gestureActions.get(action).run();
        } else {
            Log.e(TAG, "No action defined for: " + action.name());
        }
    }
}