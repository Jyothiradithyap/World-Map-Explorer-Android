package com.example.worldmap;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;


public class GestureListener implements View.OnTouchListener, GestureDetector.OnGestureListener {
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
    private GestureListener listener;
    private GestureDetector gestureDetector;
    MainActivity mainActivity = new MainActivity();
    private HashMap<GestureAction, Runnable> gestureActions;
    private float startX1, startY1, startX2, startY2,startX3,startY3;
    private boolean isTwoFingerGesture = false;
    private boolean isThreeFingerGesture = false;
    private boolean multiFingerGestureDetected = false;
    private static final int SWIPE_THRESHOLD = 100; // Minimum distance to qualify as a swipe
    private static final int DOUBLE_TAP_TIMEOUT = 300; // Max time between taps in milliseconds
    private Handler doubleTapHandler = new Handler();
    private int tapCount = 0; // Track the number of taps
    private long lastTapTime = 0;
    private static final int HOLD_LONG_PRESS_THRESHOLD = 500; // Duration in ms to detect long press
    private static final float MOVE_THRESHOLD = 20; // Movement tolerance in pixels
    private boolean isHoldLongPress = false; // Track if HOLD_LONG_PRESS was triggered
    private Handler holdLongPressHandler = new Handler();
    private Runnable holdLongPressRunnable;
    private boolean isTwoFingerDoubleTap = false;
    private boolean isScrolling = false; // Flag to check if we're scrolling
    private boolean wasScrolling = false; // Tracks if scrolling was detected
    private long scrollStartTime = 0; // Time when the scroll starts
    private boolean isDoubleTapDetected = false;
    private static final int SCROLL_TIME_THRESHOLD = 350; // Milliseconds to differentiate swipe/scroll
    private static final float SCROLL_THRESHOLD = 150; // Minimum distance for scroll detection
    private long startTime = 0; // Track the time when a gesture starts
    private static final long TWO_SCROLL_TIME_THRESHOLD = 500; // Time threshold in milliseconds for distinguishing a scroll (500ms)
    private static final float TWO_FINGER_SWIPE_THRESHOLD = 50.0f;
    private boolean scrollDetected = false; // Flag to track if scroll is detected
    private final android.os.Handler handler = new android.os.Handler();
    private Runnable singleTapRunnable;
    private boolean isGestureInProgress = false;
    private boolean isDoubleTapInProgress = false;

    private Context context; // Add a field to store the Context
    public void setGestureActionListeners(HashMap<GestureAction, Runnable> gestureActionMap) {
        this.gestureActions = gestureActionMap;
    }
    public GestureListener(Context context){
        this.context = context; // Save the Context
        gestureDetector = new GestureDetector(context, this);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.e(TAG, "Events = " + event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                handleTouchStart(event);
                startTime = System.currentTimeMillis();
                scrollStartTime = System.currentTimeMillis(); // Capture the start time of the scroll
                isScrolling = false; // Reset scrolling flag
                break;

            case MotionEvent.ACTION_MOVE:

                if (event.getPointerCount() == 1 && !multiFingerGestureDetected) { // Only check for single finger
                    detectHorizontalScroll(event);
                    float diffX = Math.abs(event.getX() - startX1);
                    float diffY = Math.abs(event.getY() - startY1);
                    if (diffX > MOVE_THRESHOLD || diffY > MOVE_THRESHOLD) {
                        isHoldLongPress = false; // Cancel long press state
                        holdLongPressHandler.removeCallbacks(holdLongPressRunnable); // Cancel long press
                    }
                    isScrolling = false;

                }else if (event.getPointerCount() == 2 && isTwoFingerGesture==true) { // Two-finger gesture
                    detectTwoFingerScroll(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                handleMultiFingerTouchStart(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleTouchEnd(event);
                scrollDetected = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                holdLongPressHandler.removeCallbacks(holdLongPressRunnable); // Cleanup on cancel
                break;
        }

        return true;
    }

    private void handleTouchStart(MotionEvent event) {
        if (event.getPointerCount() == 1) { // Single finger
            startX1 = event.getX();
            startY1 = event.getY();
            isHoldLongPress = false;
            isScrolling = false; // Reset scrolling flag
            // Start a timer to detect HOLD_LONG_PRESS
            holdLongPressRunnable = () -> {
                if (event.getPointerCount() == 1) { // Ensure it's still a single finger
                    isHoldLongPress = true; // Mark HOLD_LONG_PRESS as detected
                    Log.d(TAG, "HOLD_LONG_PRESS detected");
                    runGestureAction(GestureAction.HOLD_LONG_PRESS);
                }
            };
            holdLongPressHandler.postDelayed(holdLongPressRunnable, HOLD_LONG_PRESS_THRESHOLD);
        }
        isTwoFingerGesture = false;
        isThreeFingerGesture = false;
        multiFingerGestureDetected = false;
    }
    private void handleTouchEnd(MotionEvent event) {
        Log.d(TAG, "Tap count: " + tapCount + " | Is Double Tap: " + isTwoFingerDoubleTap + " | Last Tap Time: " + lastTapTime);
        int pointerCount = event.getPointerCount();
        holdLongPressHandler.removeCallbacks(holdLongPressRunnable); // Stop long press detection

        if (isThreeFingerGesture && pointerCount == 3) {
//            detectThreeFingerSwipe(event);
        } else if (isTwoFingerGesture && pointerCount == 2) {
            handleTwoFingerTap(event);
            if(!isScrolling) {
                if (!wasScrolling) {
                    detectTwoFingerSwipe(event);
                }
            }

        }
        else if (!multiFingerGestureDetected && pointerCount == 1) {
            if(!isScrolling) {
                // Skip detection if scrolling happened
                if (!wasScrolling) {
                    detectOneFingerSwipe(event);
                }
                if(isHoldLongPress) {
                    Log.d(TAG, "HOLD_LONG_PRESS Release_LONG_PRESS detected"+"Release_LONG_PRESS");
                    runGestureAction(GestureAction.RELEASE_LONG_PRESS);
                }else{
                    if (!isScrolling && !wasScrolling) { // Avoid single-tap detection if scrolling occurred
                        detectSingleDoubleTap(event); // Double tap logic
                    }
                }
            }
        }
        if (pointerCount > 1) {
            multiFingerGestureDetected = true;
        }
        wasScrolling = false; // Reset the scroll flag after touch ends
        isTwoFingerGesture = false;
        isThreeFingerGesture = false;
        isScrolling = false; // Reset scrolling flag on touch end
        isGestureInProgress = false;
        isTwoFingerDoubleTap = false;

    }

    private void detectTwoFingerScroll(MotionEvent event) {
        // Calculate the movement for each finger
        float diffX1 = event.getX(0) - startX1;
        float diffY1 = event.getY(0) - startY1;
        float diffX2 = event.getX(1) - startX2;
        float diffY2 = event.getY(1) - startY2;

        // Average the movements of both fingers
        float avgDiffX = (diffX1 + diffX2) / 2;
        float avgDiffY = (diffY1 + diffY2) / 2;

        // Calculate the distance moved and the time elapsed
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - startTime;

        // Thresholds for distinguishing between scroll and swipe
        final float SCROLL_DISTANCE_THRESHOLD = 30f; // Minimum movement for a scroll
        final long SCROLL_TIME_THRESHOLD = 300; // Minimum time for a scroll (longer time than a swipe)

        if ((Math.abs(avgDiffX) > SCROLL_DISTANCE_THRESHOLD || Math.abs(avgDiffY) > SCROLL_DISTANCE_THRESHOLD)) {
            isGestureInProgress = true;
            if (timeElapsed > SCROLL_TIME_THRESHOLD && !scrollDetected) {
                // If movement is gradual (not a quick flick), treat as a scroll
                if (Math.abs(avgDiffX) > Math.abs(avgDiffY)) {
                    // Horizontal scroll
                    if (avgDiffX > 0) {
                        Log.d(TAG, "Two-finger scroll: RIGHT");
                    } else {
                        Log.d(TAG, "Two-finger scroll: LEFT");
                    }
                } else {
                    // Vertical scroll
                    if (avgDiffY > 0) {
                        Log.d(TAG, "Two-finger scroll: DOWN");
                        runGestureAction(GestureAction.TWO_FINGER_SCROLL_DOWN);
                    } else {
                        Log.d(TAG, "Two-finger scroll: UP");
                        runGestureAction(GestureAction.TWO_FINGER_SCROLL_UP);
                    }
                }
                scrollDetected = true; // Set the flag to prevent repeated detection

            }
        }


        multiFingerGestureDetected = true;
    }



    private void detectHorizontalScroll(@NonNull MotionEvent event) {
        if (event.getPointerCount() == 1 && !multiFingerGestureDetected) { // Only check for single finger
            float diff_X = event.getX() - startX1;
            float diff_Y = event.getY() - startY1;

            // Check for horizontal scrolling (left-right)
            if (Math.abs(diff_X) > SCROLL_THRESHOLD && Math.abs(diff_X) > Math.abs(diff_Y)) {
                Log.d(TAG, "scroll one-finger scroll:.AAAAA");
                long currentTime = System.currentTimeMillis();
                if (currentTime - scrollStartTime > SCROLL_TIME_THRESHOLD) {
                    Log.d(TAG, "scroll one-finger scroll: BBBBBB");
                    isScrolling = true;
                    wasScrolling = true; // Set the scroll flag
                    if(diff_X>0){
                        Log.d(TAG, "scroll one-finger scroll: CCCCCCCCCC");
                        runGestureAction(GestureAction.SCROLL_RIGHT);
                        startX1 = event.getX(); // Update startX1 for continuous scrolling
                    }else{
                        Log.d(TAG, "scroll one-finger scroll: DDDDDDD");
                        runGestureAction(GestureAction.SCROLL_LEFT);
                        startX1 = event.getX(); // Update startX1 for continuous scrolling
                    }
                    scrollStartTime = currentTime; // Update time for continuous scrolling
                }
            }
            // Check for vertical scrolling (up-down)
            else if (Math.abs(diff_Y) > SCROLL_THRESHOLD && Math.abs(diff_Y) > Math.abs(diff_X)) {
                Log.d(TAG, "scroll one-finger scroll: EEEEE");
                long currentTime = System.currentTimeMillis();
                if (currentTime - scrollStartTime > SCROLL_TIME_THRESHOLD) {
                    Log.d(TAG, "scroll one-finger scroll: FFFFFFFFF");
                    isScrolling = true;
                    wasScrolling = true; // Set the scroll flag
                    if (diff_Y > 0) {
                        Log.d(TAG, "scroll one-finger scroll: GGGGGGGGG");
                        runGestureAction(GestureAction.SCROLL_DOWN);
                    } else {
                        Log.d(TAG, "scroll one-finger scroll: HHHHHHHHHHHHHHHH");

                        runGestureAction(GestureAction.SCROLL_UP);
                    }
                    startY1 = event.getY(); // Update startY1 for continuous scrolling
                    scrollStartTime = currentTime; // Update time for continuous scrolling
                }
            }
            else {
                // Reset scrolling if more than one finger is detected
                isScrolling = false;
            }

        }
    }
    private void handleTwoFingerTap(MotionEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.getPointerCount() == 2) {
            if (isGestureInProgress) {
//                // If another gesture is in progress, ignore the tap
                Log.d(TAG, "Ignoring tap, as another gesture is in progress.");
                return;
            }
            if (isDoubleTapInProgress && (currentTime - lastTapTime <= DOUBLE_TAP_TIMEOUT)) {
                // Handle double-tap
                isDoubleTapInProgress = false;  // Reset
                Log.d(TAG, "2-finger double tap detected");
                runGestureAction(GestureAction.TWO_FINGER_DOUBLE_TAP);
            }
            else {
                // Handle single tap
                lastTapTime = currentTime;
                isDoubleTapInProgress = true;
                Log.d(TAG, "2-finger single tap detected");
                // Set a delayed task to reset after timeout if no second tap occurs
                doubleTapHandler.postDelayed(() -> {
                    if (isDoubleTapInProgress) {
                        Log.d(TAG, "2-finger single tap action after timeout");
                        runGestureAction(GestureAction.TWO_FINGER_SINGLE_TAP);
                        isDoubleTapInProgress = false;
                    }
                }, DOUBLE_TAP_TIMEOUT);
            }
        }

    }


    private void detectSingleDoubleTap(MotionEvent event) {
        // Check if the finger moved significantly; if not, it's a tap
        float diffX = event.getX() - startX1;
        float diffY = event.getY() - startY1;

        if (Math.abs(diffX) < SWIPE_THRESHOLD && Math.abs(diffY) < SWIPE_THRESHOLD) { // Tap detected
            long currentTime = System.currentTimeMillis(); // Get the current time in milliseconds

            if (currentTime - lastTapTime <= DOUBLE_TAP_TIMEOUT) {
                // Second tap detected within the timeout
                tapCount++;

                if (tapCount == 2) { // Confirmed double tap
                    isDoubleTapDetected = true;
                    handler.removeCallbacks(singleTapRunnable); // Cancel the single-tap action
                    runGestureAction(GestureAction.DOUBLE_TAP);
                    resetTapDetection();
                }
            } else {
                // Timeout exceeded; treat it as a single tap
                if (!isScrolling) {
                    tapCount = 1;
                    isDoubleTapDetected = false;
                    // Schedule single-tap detection after timeout to avoid conflict
                    singleTapRunnable = () -> {
                        if (!isDoubleTapDetected) { // Confirm no double-tap detected
                            runGestureAction(GestureAction.SINGLE_TAP);
                        }
                    };
                    handler.postDelayed(singleTapRunnable, DOUBLE_TAP_TIMEOUT);
                }
                lastTapTime = currentTime; // Update the last tap time
            }
        }

    }

    private void resetTapDetection() {
        tapCount = 0;
        lastTapTime = 0;
        isDoubleTapDetected = false;
        isTwoFingerDoubleTap=false;
    }
    private void handleMultiFingerTouchStart(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        if (pointerCount == 2) {
            startX2 = event.getX(1);
            startY2 = event.getY(1);
            isTwoFingerGesture = true;
        } else if (pointerCount == 3) {
            startX3 = event.getX(2);
            startY3 = event.getY(2);
            isThreeFingerGesture = true;
        }
    }

    private void detectOneFingerSwipe(MotionEvent event) {
        float diffX = event.getX() - startX1;
        float diffY = event.getY() - startY1;

        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(diffX) > Math.abs(diffY)) {
            if (diffX > 0) {
                runGestureAction(GestureAction.SWIPE_RIGHT);
            } else {
                runGestureAction(GestureAction.SWIPE_LEFT);
            }
        } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(diffY) > Math.abs(diffX)) {
            if (diffY > 0) {
                runGestureAction(GestureAction.SWIPE_DOWN);
            } else if(diffY < 0) {
                runGestureAction(GestureAction.SWIPE_UP);
            }
        }
    }
    private void detectTwoFingerSwipe(MotionEvent event) {
        // Calculate the movement for each finger
        float diffX1 = event.getX(0) - startX1;
        float diffY1 = event.getY(0) - startY1;
        float diffX2 = event.getX(1) - startX2;
        float diffY2 = event.getY(1) - startY2;

        // Average the movements of both fingers
        float avgDiffX = (diffX1 + diffX2) / 2;
        float avgDiffY = (diffY1 + diffY2) / 2;

        // Calculate the distance moved and the time elapsed
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - startTime;

        // Check if the movement is above the threshold and happened quickly (likely a swipe)
        if (Math.abs(avgDiffX) > TWO_FINGER_SWIPE_THRESHOLD || Math.abs(avgDiffY) > TWO_FINGER_SWIPE_THRESHOLD) {
            isGestureInProgress = true;
            if (timeElapsed < TWO_SCROLL_TIME_THRESHOLD) {
                // If movement is fast, treat as a swipe
                if (Math.abs(avgDiffX) > Math.abs(avgDiffY)) {
                    // Horizontal movement is dominant
                    if (avgDiffX > 0) {
                        runGestureAction(GestureAction.TWO_FINGER_SWIPE_RIGHT);
                    } else {
                        runGestureAction(GestureAction.TWO_FINGER_SWIPE_LEFT);
                    }
                } else {
                    // Vertical movement is dominant
                    if (avgDiffY > 0) {
                        runGestureAction(GestureAction.TWO_FINGER_SWIPE_DOWN);
                    } else {
                        runGestureAction(GestureAction.TWO_FINGER_SWIPE_UP);
                    }
                }
            }
        }

        multiFingerGestureDetected = true;
    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.e(TAG, "onFling: velocityX = " + velocityX + ", velocityY = " + velocityY);
        return false;
    }
    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    private void runGestureAction(GestureAction action) {
        if (gestureActions.containsKey(action)) {
            gestureActions.get(action).run();
            Log.e("GestureListener", "Gesture detected: " + action.name());
        }

    }
}
