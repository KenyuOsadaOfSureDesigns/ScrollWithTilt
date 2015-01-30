/****************************************************************************
 *
 * Copyright 2015 Kenyu Osada
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ****************************************************************************/

package com.suredesigns.sensortest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class TiltSensorEvent {
/**
 * <p>This class is used for sharing objects between TiltSensorService and your Activity.</p>
 *
 * <p>TiltSensorEvent provides some constants, parameters, and methods.
 * e.g. TILT_NONE, TILT_LEFT, TILT_RIGHT, TILT_FORWARD, TILT_BACK are constants
 * that indicates whether device is tilted or not, and which direction to tilt.
 * If the device tilt around X-axis (equivalently, in YZ-plane),
 * then set tilt direction of the device to TILT_LEFT, or TILT_RIGHT.
 * these states can be mixed, i.e., if device is tilted to left and forward,
 * then tilt state of the device is represented as (TILT_LEFT | TILT_FORWARD).
 * If you desire to know current tilt direction of device, call getTiltDirection() method
 * in onSensorEvent().
 * </p>
 *
 * <p>mSlidingCoefficientX and mSlidingCoefficientY are coefficients of acceleration of x- or y-component.
 * </p>
 */
    public final static String TAG = TiltSensorEvent.class.getSimpleName();
    /**
     * <p>TILT_NONE, TILT_LEFT, TILT_RIGHT, TILT_FORWARD, TILT_BACK used to indicate tilt direction.
     * </p>
     */
    public static final int TILT_NONE = 0;
    public static final int TILT_LEFT = 1;
    public static final int TILT_RIGHT = 1 << 1;
    public static final int TILT_FORWARD = 1 << 2;
    public static final int TILT_BACK = 1 << 3;
    /**
     * <p>threshold used to detect tilt or shake motion. if rotation angle of device is greater than
     * the threshold, then detect tilt or shake motion. </p>
     */
    private static final float TILT_THRESHOLD_LEFT = 0.18f;
    private static final float TILT_THRESHOLD_RIGHT = 0.18f;
    private static final float TILT_THRESHOLD_FORWARD = 0.09f;
    private static final float TILT_THRESHOLD_BACK = 0.08f;
    private static final float SHAKE_THRESHOLD_LEFT = 0.35f;
    private static final float SHAKE_THRESHOLD_RIGHT = 0.35f;
    private static final float SHAKE_THRESHOLD_FORWARD = 0.24f;
    private static final float SHAKE_THRESHOLD_BACK = 0.21f;

    /**
     * <p>viscosity coefficient used to determine scroll velocity of current view.
     * scroll velocity obeys the following equation.
     * <dl><dd>"alteration of velocity" = ("sliding acceleration" - "viscosity" * "previous velocity") * "time interval".</dd></dl>
     * symbolic representation of the above equation is,
     * <dl><dd>dv = (a - g * v) * dt.</dd></dl>
     * more conveniently, this equation can be interpreted as follows,
     * <dl><dd>v += (a - g * v) * dt,</dd></dl>
     * or
     * <dl><dd>v = a * dt + (1.f - g * dt) * v.</dd></dl>
     * if coefficient of v in rhs is negative (i.e. in case of g > 1/dt, or dt > 1/g),
     * then sequence of v diverges (vibrates). thus, time interval dt and viscosity g must be sufficiently small.
     * dt can be estimated from the delay rate of sensor event, in advance.
     * maybe maximum delay rate is smaller than reciprocal of viscosity (= 0.8 s = 800,000,000 ns).
     * </p>
     */
    private static final float DISPLAY_VISCOSITY = 1.25f;
    private MotionEvent mMotionEvent = null;
    private float mReference[] = {0.0f, 0.0f, -1.0f};
    private float mGravitation[] = {0.0f, 0.0f, 0.0f};
    private float mVelocity[] = {0.0f, 0.0f};
    private float mSlidingCoefficientX = 0.25e3f;
    private float mSlidingCoefficientY = 0.375e3f;
    private static final float THRESHOLD_VELOCITY_X = 1.e3f;
    private static final float INV_THRESHOLD_VELOCITY_X = 1.f/THRESHOLD_VELOCITY_X;
    private static final float THRESHOLD_VELOCITY_Y = 1.4e3f;
    private static final float INV_THRESHOLD_VELOCITY_Y = 1.f/THRESHOLD_VELOCITY_Y;
    private static final float SLIDING_COEFFICIENT_MIN_MAX_RATIO_X = 0.53125f;
    private static final float SLIDING_COEFFICIENT_MIN_MAX_RATIO_Y = 0.53125f;

    private boolean mReferenceChanged = false;
    private boolean mPrevReferenceState = false;
    private long mPrevTimestamp = -1;
    private int mDisplayWidth = -1;
    private int mDisplayHeight = -1;
    private int mViewWidth = -1;
    private int mViewHeight = -1;
    private int mViewLeft = -1;
    private int mViewRight = -1;
    private int mViewTop = -1;
    private int mViewBottom = -1;
    private int mViewScrollX = 0;
    private int mViewScrollY = 0;
    private int mPrevViewScrollX = 0;
    private int mPrevViewScrollY = 0;
    private int mRateUs = SensorManager.SENSOR_DELAY_UI;
    private View mView = null;
    private View.OnTouchListener mOnTouchListener = null;
    private View.OnLongClickListener mOnLongClickListener = null;
    private Context mContext = null;
    private float mPrevTilt[] = {0.f, 0.f};
    public static final int FLING_NONE = 0;
    public static final int FLING_LEFT = 1;
    public static final int FLING_RIGHT = 1 << 1;
    public static final int FLING_FORWARD = 1 << 2;
    public static final int FLING_BACK = 1 << 3;
    public static final int FLING_MASK = FLING_LEFT | FLING_RIGHT | FLING_FORWARD | FLING_BACK;
    public static final int SHAKE_LEFT = 1 << 4;
    public static final int SHAKE_RIGHT = 1 << 5;
    public static final int SHAKE_FORWARD = 1 << 6;
    public static final int SHAKE_BACK = 1 << 7;
    public static final int SHAKE_MASK = SHAKE_LEFT | SHAKE_RIGHT | SHAKE_FORWARD | SHAKE_BACK;
    private int mFlingState = FLING_NONE;
    private float mFlingVelocity[] = {0.f, 0.f};
    private float mMeanFlingSpeed = 7200.f;
    private boolean mIsPhysicallyTouched = false;
    private boolean mStopDispatchMotionEvent = false;
    private Intent mServiceIntent = null;
    private static TiltSensorEvent sTiltSensorEvent = new TiltSensorEvent();

    private TiltSensorEvent(){}
    public static TiltSensorEvent getInstance(){
        return sTiltSensorEvent;
    }

    public void initializeTiltService(final View view) {
        initializeTiltService(view, null, null, SensorManager.SENSOR_DELAY_UI);
    }
    public void initializeTiltService(final View view, final int delayRate) {
        initializeTiltService(view, null, null, delayRate);
    }
    public void initializeTiltService(final View view, final View.OnTouchListener onTouchListener) {
        initializeTiltService(view, onTouchListener, null, SensorManager.SENSOR_DELAY_UI);
    }
    public void initializeTiltService(final View view, final View.OnTouchListener onTouchListener, final int delayRate) {
        initializeTiltService(view, onTouchListener, null, delayRate);
    }
    public void initializeTiltService(final View view, final View.OnLongClickListener onLongClickListener) {
        initializeTiltService(view, null, onLongClickListener, SensorManager.SENSOR_DELAY_UI);
    }
    public void initializeTiltService(final View view, final View.OnLongClickListener onLongClickListener, final int delayRate) {
        initializeTiltService(view, null, onLongClickListener, delayRate);
    }
    public void initializeTiltService(final View view, final View.OnTouchListener onTouchListener, final View.OnLongClickListener onLongClickListener) {
        initializeTiltService(view, onTouchListener, onLongClickListener, SensorManager.SENSOR_DELAY_UI);
    }
    public void initializeTiltService(final View view, final View.OnTouchListener onTouchListener, final View.OnLongClickListener onLongClickListener, final int delayRate) {
        setDelayRate(delayRate);
        setView(view, onTouchListener, onLongClickListener, true);
        if(mContext != null) mServiceIntent = new Intent(mContext, TiltSensorService.class);
    }

    public ComponentName startTiltSensorService() {
        if (mContext != null && mServiceIntent != null) return mContext.startService(mServiceIntent);
        else return null;
    }
    public boolean stopTiltSensorService() {
        if (mContext != null && mServiceIntent != null) return mContext.stopService(mServiceIntent);
        else return false;
    }
    public void setDisplaySize() {
        if (mContext != null) {
            setDisplaySize((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE));
        }
    }
    public void setDisplaySize(WindowManager windowManager) {
        Display defaultDisplay = windowManager.getDefaultDisplay();
        Point size = new Point();
        if (defaultDisplay != null) {
            defaultDisplay.getSize(size);
            mDisplayWidth = size.x;
            mDisplayHeight = size.y;
        }
    }
    public int getDisplayWidth() {
        return mDisplayWidth;
    }
    public int getDisplayHeight() {
        return mDisplayHeight;
    }

    public void setDelayRate(int delayRate) {
        mRateUs = (delayRate < (int)(0.75e9f/DISPLAY_VISCOSITY)) ? delayRate : (int)(0.75e9f/DISPLAY_VISCOSITY);
    }
    public int getDelayRate() {
        return mRateUs;
    }
    public int getDelayTime() {
        // These constants are copied from SensorManager#getDelay.
        // Since, values of the constants may be changed in future,
        // do not depend on these constants.
        switch (mRateUs) {
            case SensorManager.SENSOR_DELAY_FASTEST:
                return 20000;
            case SensorManager.SENSOR_DELAY_GAME:
                return 20000;
            case SensorManager.SENSOR_DELAY_UI:
                return 66667;
            case SensorManager.SENSOR_DELAY_NORMAL:
                return 200000;
            default:
                return (mRateUs > 20000) ? mRateUs : 20000;
        }
    }

    public boolean scrollableViewExists() {
        if (mView == null || !mView.isEnabled()) {
            return false;
        }
        return mView.canScrollHorizontally(1)
                || mView.canScrollHorizontally(-1)
                || mView.canScrollVertically(1)
                || mView.canScrollVertically(-1);
    }
    private boolean setTouchEventListenerTo(View view) {
        return setTouchEventListenerTo(view, null, null);
    }
    private boolean setOnTouchListenerTo(View view, final View.OnTouchListener onTouchListener) {
        return setTouchEventListenerTo(view, onTouchListener, null);
    }
    private boolean setTouchEventListenerTo(View view, final View.OnLongClickListener onLongClickListener) {
        return setTouchEventListenerTo(view, null, onLongClickListener);
    }
    private boolean setTouchEventListenerTo(View view, final View.OnTouchListener onTouchListener, final View.OnLongClickListener onLongClickListener) {
        if (view == null || view.getContext() == null) return false;

        final GestureDetector gestureDetector = new GestureDetector(view.getContext(), new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }
            @Override
            public void onShowPress(MotionEvent e) { defaultOnShowPress(e);}
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                defaultOnScroll(e1, e2, distanceX, distanceY);
                return false;
            }
            @Override
            public void onLongPress(MotionEvent e) {
                defaultOnLongPress(e);
            }
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                defaultOnFling(e1, e2, velocityX, velocityY);
                return false;
            }
        });
        if (onTouchListener != null) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!gestureDetector.onTouchEvent(event)) {
                        if (!defaultOnTouch(v, event)) {
                            return onTouchListener.onTouch(v, event);
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            });
        } else if (mOnTouchListener != null) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!gestureDetector.onTouchEvent(event)) {
                        if (!defaultOnTouch(v, event)) {
                            return mOnTouchListener.onTouch(v, event);
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            });
        } else {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!gestureDetector.onTouchEvent(event)) {
                        return defaultOnTouch(v, event);
                    } else {
                        return true;
                    }
                }
            });
        }
        if (onLongClickListener != null) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!defaultOnLongClick(v)) {
                        return onLongClickListener.onLongClick(v);
                    } else {
                        return false;
                    }
                }
            });
        } else if (mOnLongClickListener != null) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!defaultOnLongClick(v)) {
                        return mOnLongClickListener.onLongClick(v);
                    } else {
                        return false;
                    }
                }
            });
        } else {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return !defaultOnLongClick(v);
                }
            });
        }
        return true;
    }

    public void setView(View view) {
        setView(view, null, null, true);
    }
    public void setView(View view, boolean hasFocus) {
        setView(view, null, null, hasFocus);
    }
    public void setView(View view, final View.OnTouchListener onTouchListener) {
        setView(view, onTouchListener, null, true);
    }
    public void setView(View view, final View.OnLongClickListener onLongClickListener) {
        setView(view, null, onLongClickListener, true);
    }
    public void setView(View view, final View.OnTouchListener onTouchListener, final View.OnLongClickListener onLongClickListener) {
        setView(view, onTouchListener, onLongClickListener, true);
    }
    public void setView(View view, final View.OnTouchListener onTouchListener, final View.OnLongClickListener onLongClickListener, boolean hasFocus) {
        if (view != null) {
            mView = view;
            if (onTouchListener != null) {
                mOnTouchListener = onTouchListener;
            }
            if (onLongClickListener != null) {
                mOnLongClickListener = onLongClickListener;
            }
            setTouchEventListenerTo(view, onTouchListener, onLongClickListener);
            mContext = view.getContext();
            setDisplaySize();
            setViewSize(view, hasFocus);
        }
    }
    public View getView() {
        return mView;
    }

    public void setViewSize(View view) {
        setViewSize(view, true);
    }
    public void setViewSize(View view, boolean hasFocus) {
        if (hasFocus) {
            mViewWidth = view.getWidth();
            mViewHeight = view.getHeight();
            mViewLeft = view.getLeft();
            mViewRight = view.getRight();
            mViewTop = view.getTop();
            mViewBottom = view.getBottom();
            mPrevViewScrollX = mViewScrollX = view.getScrollX();
            mPrevViewScrollY = mViewScrollY = view.getScrollY();
            if (mViewWidth == 0 || mViewHeight == 0) {
                mViewWidth = mDisplayWidth;
                mViewHeight = mDisplayHeight;
            }
        }
    }
    public int getViewWidth() {
        return (mViewWidth >= 0) ? mViewWidth : 0;
    }
    public int getViewHeight() {
        return (mViewHeight >= 0) ? mViewHeight : 0;
    }
    public int getViewTop() {
        return (mViewTop >= 0) ? mViewTop : 0;
    }
    public int getViewBottom() {
        return (mViewBottom >= 0) ? mViewBottom : 0;
    }
    public int getViewLeft() {
        return (mViewLeft >= 0) ? mViewLeft : 0;
    }
    public int getViewRight() {
        return (mViewRight >= 0) ? mViewRight : 0;
    }
    public void setPrevViewScrollLocation() {
        mPrevViewScrollX = mViewScrollX;
        mPrevViewScrollY = mViewScrollY;
    }
    public void setViewScrollLocation() {
        getViewScrollX();
        getViewScrollY();
    }
    public void updateViewScrollLocation() {
        setPrevViewScrollLocation();
        setViewScrollLocation();
    }
    public int getViewScrollX() {
        return mViewScrollX = mView.getScrollX();
    }
    public int getPrevViewScrollX() { return mPrevViewScrollX; }
    public int getViewScrollY() {
        return mViewScrollY = mView.getScrollY();
    }
    public int getPrevViewScrollY() { return mPrevViewScrollY; }

    public float getX() {
        return mMotionEvent.getX();
    }
    public void setX(float x) {
        mMotionEvent.offsetLocation(x - mMotionEvent.getX(), 0.f);
    }
    public float getY() {
        return mMotionEvent.getY();
    }
    public void setY(float y) {
        mMotionEvent.offsetLocation(0.f, y - mMotionEvent.getY());
    }

    public MotionEvent getMotionEvent() {
        return MotionEvent.obtain(mMotionEvent);
    }

    public void setMotionEvent(MotionEvent motionEvent) {
        mMotionEvent = MotionEvent.obtain(motionEvent);
    }
    public void setMotionEvent(long downTime, long eventTime, int action, float x, float y, int metaState) {
        mMotionEvent = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
    }

    public void setTimestamp(long timestamp) {
        mPrevTimestamp = timestamp;
    }
    public long getPrevTimestamp() {
        return mPrevTimestamp;
    }

    public void detectFlingAndShake() {
        detectFlingAndShake(getTiltVector());
    }
    public void detectFlingAndShake(final float tiltVector[]) {
        // tiltVariationX is variation of x-component of tilt vector. x-component of tilt vector
        // corresponds to rotation of device around x-axis.
        final int direction = getTiltDirection(tiltVector);
        float tiltVariationY;
        float tiltVariationX;
        float squareRotationAngleY;
        float squareRotationAngleX;

        if (Math.abs(tiltVector[0]) >= 0.99999f) {
            tiltVariationX = 0.99999f * (tiltVector[0]/Math.abs(tiltVector[0])) - mPrevTilt[0];
            squareRotationAngleX = tiltVariationX * tiltVariationX / (1.f - 0.99999f * 0.99999f);
        } else {
            tiltVariationX = tiltVector[0] - mPrevTilt[0];
            squareRotationAngleX = tiltVariationX * tiltVariationX / (1.f - tiltVector[0] * tiltVector[0]);
        }
        if (Math.abs(tiltVector[1]) >= 0.99999f) {
            tiltVariationY = 0.99999f * (tiltVector[1]/Math.abs(tiltVector[1])) - mPrevTilt[1];
            squareRotationAngleY = tiltVariationY * tiltVariationY / (1.f - 0.99999f * 0.99999f);
        } else {
            tiltVariationY = tiltVector[1] - mPrevTilt[1];
            squareRotationAngleY = tiltVariationY * tiltVariationY / (1.f - tiltVector[1] * tiltVector[1]);
        }
        if (       (tiltVariationY < 0.f && squareRotationAngleY > SHAKE_THRESHOLD_LEFT * SHAKE_THRESHOLD_LEFT)
                || (tiltVariationY > 0.f && squareRotationAngleY > SHAKE_THRESHOLD_RIGHT * SHAKE_THRESHOLD_RIGHT)
                || (tiltVariationX < 0.f && squareRotationAngleX > SHAKE_THRESHOLD_FORWARD * SHAKE_THRESHOLD_FORWARD)
                || (tiltVariationX > 0.f && squareRotationAngleX > SHAKE_THRESHOLD_BACK * SHAKE_THRESHOLD_BACK)
        ) {
            if ((mFlingState & FLING_MASK) == 0) {
                if ((direction & TILT_LEFT) != 0 && tiltVariationY < 0.f) {
                    mFlingState |= SHAKE_LEFT;
                } else if ((direction & TILT_RIGHT) != 0 && tiltVariationY > 0.f) {
                    mFlingState |= SHAKE_RIGHT;
                }
                if ((direction & TILT_FORWARD) != 0 && tiltVariationX < 0.f) {
                    mFlingState |= SHAKE_FORWARD;
                } else if ((direction & TILT_BACK) != 0 && tiltVariationX > 0.f) {
                    mFlingState |= SHAKE_BACK;
                }
            }
        } else {
            if (direction != TILT_NONE) {
                if ((mFlingState & (FLING_LEFT | SHAKE_LEFT)) != 0 && (direction & TILT_LEFT) == 0) {
                    mFlingState &= ~(FLING_LEFT | SHAKE_LEFT);
                } else if ((mFlingState & (FLING_RIGHT | SHAKE_RIGHT)) != 0 && (direction & TILT_RIGHT) == 0) {
                    mFlingState &= ~(FLING_RIGHT | SHAKE_RIGHT);
                }
                if ((mFlingState & (FLING_FORWARD | SHAKE_FORWARD)) != 0 && (direction & TILT_FORWARD) == 0) {
                    mFlingState &= ~(FLING_FORWARD | SHAKE_FORWARD);
                } else if ((mFlingState & (FLING_BACK | SHAKE_BACK)) != 0 && (direction & TILT_BACK) == 0) {
                    mFlingState &= ~(FLING_BACK | SHAKE_BACK);
                }
            }
        }
        mPrevTilt[0] = tiltVector[0];
        mPrevTilt[1] = tiltVector[1];
        reportFlingState();
    }

    public void setFlingState(int flingState) {
        if ((flingState & FLING_MASK) != 0 && (flingState & SHAKE_MASK) != 0) {
            mFlingState = FLING_NONE;
        } else {
            mFlingState = flingState;
        }
    }
    public int getFlingState() {
        return mFlingState;
    }

    public void updateMeanFlingSpeed(final float flingSpeed) {
        if (flingSpeed >= 2.5f * mMeanFlingSpeed) {
            mMeanFlingSpeed += (flingSpeed * 0.25f - mMeanFlingSpeed) * 0.25f;
        } else if (flingSpeed >= 1.75f * mMeanFlingSpeed) {
            mMeanFlingSpeed += (flingSpeed * 0.5f - mMeanFlingSpeed) * 0.25f;
        } else if (flingSpeed >= 0.625f * mMeanFlingSpeed) {
            mMeanFlingSpeed += (flingSpeed - mMeanFlingSpeed) * 0.25f;
        } else {
            mMeanFlingSpeed += (flingSpeed * 2.f - mMeanFlingSpeed) * 0.25f;
        }
    }

    /**
     * Callback methods.
     */
    private boolean defaultOnTouch(View view, MotionEvent motionEvent) {
        if (view == null || motionEvent == null) {
            return true;
        }
        final int action = motionEvent.getAction();
        if (motionEvent.getDeviceId() != 0) {
            view.setKeepScreenOn(false);
            view.setFocusable(true);
            if(!mIsPhysicallyTouched) {
                mIsPhysicallyTouched = true;
                setFlingState(mFlingState & FLING_MASK);
            }
            if(action == MotionEvent.ACTION_DOWN) {
                mStopDispatchMotionEvent = true;
            } else if (action != MotionEvent.ACTION_MOVE) {
                mStopDispatchMotionEvent = false;
            }
            setMotionEvent(motionEvent);
        } else {
            if (action == MotionEvent.ACTION_CANCEL) {
                view.setKeepScreenOn(false);
                view.setFocusable(true);
            } else {
                view.setKeepScreenOn(true);
                view.setFocusable(false);
            }
            if(mIsPhysicallyTouched) {
                mIsPhysicallyTouched = false;
            }
        }
        Log.d(TAG, "enabled : " + view.isEnabled()
                        + ", inTouchMode : " + view.isInTouchMode()
                        + ", clickable : " + view.isClickable()
                        + ", longClickable : " + view.isLongClickable()
                        + ",\nfocusable : " + view.isFocusable()
                        + ", focusableInTouchMode : " + view.isFocusableInTouchMode()
                        + ", focused : " + view.isFocused()
                        + ", keepScreen : " + view.getKeepScreenOn()
        );
        return false;
    }
    private boolean defaultOnLongClick(View v) {
        return false;
    }
    private void defaultOnShowPress(MotionEvent e) {
    }
    private boolean defaultOnScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (e1 == null || e2 == null) {
            return true;
        }
        return false;
    }
    private void defaultOnLongPress(MotionEvent e) {
    }
    private boolean defaultOnFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) {
            return true;
        }
        if (e2.getDeviceId() != 0) {
            final float invDt = 1.0e6f/getDelayTime();
            final float flingSpeed = (float) Math.sqrt((double)(velocityX * velocityX + velocityY * velocityY));

            updateMeanFlingSpeed(flingSpeed);
            updateFlingState(flingSpeed, velocityX, velocityY);

            float Vx = 0.f, Vy = 0.f;
            if ((mFlingState & FLING_MASK) != 0) {
                mFlingState &= ~SHAKE_MASK;
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (flingSpeed > 2.f * mMeanFlingSpeed) {
                        Vx = getViewWidth() * invDt * 4.f;
                    } else if (flingSpeed > mMeanFlingSpeed) {
                        Vx = getViewWidth() * invDt * 3.f;
                    } else {
                        Vx = getViewWidth() * invDt * 2.f;
                    }
                    if ((mFlingState & FLING_RIGHT) != 0) Vx = -Vx;
                } else {
                    if (flingSpeed > 2.f * mMeanFlingSpeed) {
                        Vy = getViewHeight() * invDt * 4.f;
                    } else if (flingSpeed > mMeanFlingSpeed) {
                        Vy = getViewHeight() * invDt * 3.f;
                    } else {
                        Vy = getViewHeight() * invDt * 2.f;
                    }
                    if ((mFlingState & FLING_FORWARD) != 0) Vy = -Vy;
                }
            }
            mFlingVelocity[0] = Vx;
            mFlingVelocity[1] = Vy;
        }
        return false;
    }

    public void defaultOnWindowFocusChanged(boolean hasFocus) {
        defaultOnWindowFocusChanged(mView, hasFocus);
    }
    public void defaultOnWindowFocusChanged(View currentView, boolean hasFocus) {
        setView(currentView, hasFocus);
    }

    private void updateFlingState(final float flingSpeed, final float velocityX, final float velocityY) {
        if (flingSpeed > 0.375f * mMeanFlingSpeed) {
            if (velocityX > 0.5f * flingSpeed) {
                mFlingState = (mFlingState & ~FLING_LEFT) | FLING_RIGHT;
            } else if (velocityX < -0.5f * flingSpeed) {
                mFlingState = (mFlingState & ~FLING_RIGHT) | FLING_LEFT;
            } else {
                mFlingState &= ~(FLING_LEFT | FLING_RIGHT);
            }
            if (velocityY > 0.5f * flingSpeed) {
                mFlingState = (mFlingState & ~FLING_FORWARD) | FLING_BACK;
            } else if (velocityY < -0.5f * flingSpeed) {
                mFlingState = (mFlingState & ~FLING_BACK) | FLING_FORWARD;
            } else {
                mFlingState &= ~(FLING_FORWARD | FLING_BACK);
            }
        } else {
            mFlingState &= ~FLING_MASK;
        }
    }
    public boolean getStopDispatchMotionEventFlag(){return mStopDispatchMotionEvent;}
    public boolean isPhysicallyTouched() {return mIsPhysicallyTouched; }
    public void setIsPhysicallyTouched(boolean touchState) {mIsPhysicallyTouched = touchState; }
    public boolean referenceHasChanged() {
        return mReferenceChanged;
    }
    public boolean referenceHasChangedPrev() {
        return mPrevReferenceState;
    }
    public void setReferenceState(boolean state){
        mPrevReferenceState = mReferenceChanged;
        mReferenceChanged = state;
    }

    public void reportFlingState() {
        reportFlingState(TAG);
    }
    public void reportFlingState(String tag) {
        reportFlingState(tag, mFlingState);
    }
    public static void reportFlingState(String tag, int flingState) {
        String message = "";
        if ((flingState & FLING_LEFT) != 0) {
            message += "FLING_LEFT ";
        }
        if ((flingState & FLING_RIGHT) != 0) {
            message += "FLING_RIGHT ";
        }
        if ((flingState & FLING_FORWARD) != 0) {
            message += "FLING_FORWARD ";
        }
        if ((flingState & FLING_BACK) != 0) {
            message += "FLING_BACK ";
        }
        if ((flingState & SHAKE_LEFT) != 0) {
            message += "SHAKE_LEFT ";
        }
        if ((flingState & SHAKE_RIGHT) != 0) {
            message += "SHAKE_RIGHT ";
        }
        if ((flingState & SHAKE_FORWARD) != 0) {
            message += "SHAKE_FORWARD ";
        }
        if ((flingState & SHAKE_BACK) != 0) {
            message += "SHAKE_BACK ";
        }
        if (flingState == FLING_NONE) {
            message = "FLING_NONE";
        }
        Log.d(tag + ".reportFlingState", message);
    }

    public boolean changeReferencePoint() {
        if (mReferenceChanged) return false;
        mReference[0] = mGravitation[0];
        mReference[1] = mGravitation[1];
        mReference[2] = mGravitation[2];
        mPrevTilt[0] = 0.f;
        mPrevTilt[1] = 0.f;
        mReferenceChanged = true;
        return true;
    }

    public void copyReferenceTo(float dist[]) {
        dist[0] = mReference[0];
        dist[1] = mReference[1];
        dist[2] = mReference[2];
    }

    public void copyGravitationTo(float dist[]) {
        dist[0] = mGravitation[0];
        dist[1] = mGravitation[1];
        dist[2] = mGravitation[2];
    }

    public void updateGravitation(final float sensorEventValues[]) {
        float normEventVal = normIn3D(sensorEventValues);

        if (normEventVal == 0.f) {
            mGravitation[0] -= 0.8f * mGravitation[0];
            mGravitation[1] -= 0.8f * mGravitation[1];
            mGravitation[2] -= 0.8f * mGravitation[2];
        } else {
            final float invNormEventVal = 1.0f/normIn3D(sensorEventValues);
            mGravitation[0] += 0.8f * (sensorEventValues[0] * invNormEventVal - mGravitation[0]);
            mGravitation[1] += 0.8f * (sensorEventValues[1] * invNormEventVal - mGravitation[1]);
            mGravitation[2] += 0.8f * (sensorEventValues[2] * invNormEventVal - mGravitation[2]);
        }
        normalizeIn3D(mGravitation);
    }

    public void setVelocity(final float Vx, final float Vy) {
        mVelocity[0] = Vx;
        mVelocity[1] = Vy;
    }

    public void updateVelocity(final float dt) {
        final float product = scalarProduct(mGravitation, mReference, 3);
        updateViewScrollLocation();

        if (isPhysicallyTouched()) {
            // get current scroll velocity of View, if device is physically touched.
            mVelocity[0] = -(mViewScrollX - mPrevViewScrollX)/dt;
            mVelocity[1] = -(mViewScrollY - mPrevViewScrollY)/dt;
        }
        if (Math.abs(mVelocity[0]) < 1.f && (mFlingState & (FLING_LEFT | FLING_RIGHT | SHAKE_LEFT | SHAKE_RIGHT)) != 0) {
            mFlingState &= ~(FLING_LEFT | FLING_RIGHT | SHAKE_LEFT | SHAKE_RIGHT);
            mVelocity[0] = 0.f;
            mVelocity[1] = 0.f;
        }
        if (Math.abs(mVelocity[1]) < 1.f && (mFlingState & (FLING_FORWARD | FLING_BACK | SHAKE_FORWARD | SHAKE_BACK)) != 0) {
            mFlingState &= ~(FLING_FORWARD | FLING_BACK | SHAKE_FORWARD | SHAKE_BACK);
            mVelocity[0] = 0.f;
            mVelocity[1] = 0.f;
        }

        if((mFlingState & FLING_MASK) != 0) {
            mVelocity[0] += 0.0625f * (mFlingVelocity[0] - mVelocity[0]);
            mVelocity[1] += 0.0625f * (mFlingVelocity[1] - mVelocity[1]);
        } else {
            if ((mFlingState & (SHAKE_LEFT | SHAKE_RIGHT)) != 0) {
                mVelocity[0] += ((mGravitation[0] - product * mReference[0])
                        * SensorManager.GRAVITY_EARTH * calcSlidingCoefficientX() * 8.f
                        - mVelocity[0] * DISPLAY_VISCOSITY) * dt;
            } else {
                mVelocity[0] += ((mGravitation[0] - product * mReference[0])
                        * SensorManager.GRAVITY_EARTH * calcSlidingCoefficientX()
                        - mVelocity[0] * DISPLAY_VISCOSITY) * dt;
            }
            if ((mFlingState & (SHAKE_FORWARD | SHAKE_BACK)) != 0) {
                mVelocity[1] += ((mGravitation[1] - product * mReference[1])
                        * SensorManager.GRAVITY_EARTH * calcSlidingCoefficientY() * 8.f
                        - mVelocity[1] * DISPLAY_VISCOSITY) * dt;
            } else {
                mVelocity[1] += ((mGravitation[1] - product * mReference[1])
                        * SensorManager.GRAVITY_EARTH * calcSlidingCoefficientY()
                        - mVelocity[1] * DISPLAY_VISCOSITY) * dt;
            }
        }
    }
    private float calcSlidingCoefficientX() {
        return calcSlidingCoefficient(mSlidingCoefficientX, mVelocity[0], INV_THRESHOLD_VELOCITY_X, SLIDING_COEFFICIENT_MIN_MAX_RATIO_X);
    }
    private float calcSlidingCoefficientY() {
        return calcSlidingCoefficient(mSlidingCoefficientY, mVelocity[1], INV_THRESHOLD_VELOCITY_Y, SLIDING_COEFFICIENT_MIN_MAX_RATIO_Y);
    }
    private static float calcSlidingCoefficient(final float maxCoefficient, final float velocity, final float inverseThresholdVelocity, final float minMaxRatio) {
        final float coefficient =  maxCoefficient * (minMaxRatio + (1.f - minMaxRatio) * Math.abs(velocity) * inverseThresholdVelocity);
        Log.d(TAG, "coefficientRatio = " + String.valueOf(((coefficient < maxCoefficient) ? coefficient : maxCoefficient)/maxCoefficient));
        return (coefficient < maxCoefficient) ? coefficient : maxCoefficient;
    }

    public void updatePointerPosition(final long sensorEventTimestamp) {
        final float dt = (sensorEventTimestamp - mPrevTimestamp) * 1.0e-9f; // nsec -> sec

        updateVelocity(dt);
        final float minX = getViewLeft(), maxX = getViewRight();
        final float minY = getViewTop(), maxY = getViewBottom();
        float x = mMotionEvent.getX(), y = mMotionEvent.getY();
        if (x < minX || maxX < x || y < minY || maxY < y) {
            // periodic boundary condition. location of pointer must be in visible region.
            x -= (maxX - minX) * (int)((x - minX)/(maxX - minX));
            y -= (maxY - minY) * (int)((y - minY)/(maxY - minY));
            if(x < minX) x += (maxX - minX);
            if(y < minY) y += (maxY - minY);
            mMotionEvent.setLocation(x, y);
        }
        mMotionEvent.offsetLocation(-dt * mVelocity[0], dt * mVelocity[1]);
        mPrevTimestamp = sensorEventTimestamp;
    }

    public void setSlidingCoefficients(float newCoefficientX, float newCoefficientY) {
        mSlidingCoefficientX = newCoefficientX;
        mSlidingCoefficientY = newCoefficientY;
    }
    public void setSlidingCoefficientX(float newCoefficientX) {
        mSlidingCoefficientX = newCoefficientX;
    }
    public void setSlidingCoefficientY(float newCoefficientY) {
        mSlidingCoefficientY = newCoefficientY;
    }
    public float getSlidingCoefficientX() {
        return mSlidingCoefficientX;
    }
    public float getSlidingCoefficientY() {
        return mSlidingCoefficientY;
    }

    public float[] getTiltVector() {
        return getTiltVector(mReference, mGravitation);
    }
    public static float[] getTiltVector(final float reference[], final float gravitation[]) {
        final float refSquaredOnYZ = reference[1]*reference[1] + reference[2]*reference[2];
        final float refSquaredOnZX = reference[2]*reference[2] + reference[0]*reference[0];
        final float targetSquaredOnYZ = gravitation[1]*gravitation[1] + gravitation[2]*gravitation[2];
        final float targetSquaredOnZX = gravitation[2]*gravitation[2] + gravitation[0]*gravitation[0];

        float[] tiltVector = vectorProductOnXY(gravitation, reference);
        if (targetSquaredOnYZ != 0.f && refSquaredOnYZ != 0.f) {
            tiltVector[0] /= (float) Math.sqrt(targetSquaredOnYZ * refSquaredOnYZ);
        } else {
            tiltVector[0] = 0.f;
        }
        if (targetSquaredOnZX != 0.f && refSquaredOnZX != 0.f) {
            tiltVector[1] /= (float) Math.sqrt(targetSquaredOnZX * refSquaredOnZX);
        } else {
            tiltVector[1] = 0.f;
        }
        return tiltVector;
    }

    public static int getTiltDirection(final float reference[], final float gravitation[]) {
        return getTiltDirection(reference, gravitation, TILT_THRESHOLD_LEFT, TILT_THRESHOLD_RIGHT, TILT_THRESHOLD_FORWARD, TILT_THRESHOLD_BACK);
    }
    public static int getTiltDirection(final float reference[], final float gravitation[], final float thresholdLeft, final float thresholdRight, final float thresholdForward, final float thresholdBack) {
        final float[] tiltVector = getTiltVector(reference, gravitation);
        return getTiltDirection(tiltVector, thresholdLeft, thresholdRight, thresholdForward, thresholdBack);
    }
    public static int getTiltDirection(final float tiltVector[]) {
        return getTiltDirection(tiltVector, TILT_THRESHOLD_LEFT, TILT_THRESHOLD_RIGHT, TILT_THRESHOLD_FORWARD, TILT_THRESHOLD_BACK);
    }
    public int getTiltDirection() {
        return getTiltDirection(getTiltVector(), TILT_THRESHOLD_LEFT, TILT_THRESHOLD_RIGHT, TILT_THRESHOLD_FORWARD, TILT_THRESHOLD_BACK);
    }
    public static int getTiltDirection(final float tiltVector[], final float thresholdLeft, final float thresholdRight, final float thresholdForward, final float thresholdBack) {
        int direction = TILT_NONE;

        if (tiltVector[0] >= thresholdBack) {
            direction |=TILT_BACK;
        } else if (tiltVector[0] <= -thresholdForward) {
            direction |= TILT_FORWARD;
        }
        if (tiltVector[1] >= thresholdRight) {
            direction |=TILT_RIGHT;
        } else if (tiltVector[1] <= -thresholdLeft) {
            direction |= TILT_LEFT;
        }
        reportTiltDirection(direction);

        return direction;
    }
    public static void reportTiltDirection(int direction) {
        reportTiltDirection(TAG, direction);
    }
    public static void reportTiltDirection(String tag, int direction) {
        String message = "";
        if ((direction & TILT_LEFT) != 0) {
            message += "TILT_LEFT ";
        }
        if ((direction & TILT_RIGHT) != 0) {
            message += "TILT_RIGHT ";
        }
        if ((direction & TILT_FORWARD) != 0) {
            message += "TILT_FORWARD ";
        }
        if ((direction & TILT_BACK) != 0) {
            message += "TILT_BACK ";
        }
        if (direction == TILT_NONE) {
            message = "TILT_NONE";
        }
        Log.d(tag + ".reportTiltDirection", message);
    }
    private static void normalize(float vector[]) {
        normalize(vector, vector.length);
    }
    private static void normalize(float vector[], final int dim) {
        final float norm = normOf(vector);
        if (norm != 0.f) {
            final float invNorm = 1.0f/norm;
            for (int i = 0; i < dim; i++) {
                vector[i] *= invNorm;
            }
        } else {
            for (int i = 0; i < dim; i++) {
                vector[i] = 0.f;
            }
        }
    }
    private static void normalizeIn3D(float vector[]) {
        final float norm = normIn3D(vector);
        if (norm != 0.f) {
            final float invNorm = 1.0f/norm;
            vector[0] *= invNorm;
            vector[1] *= invNorm;
            vector[2] *= invNorm;
        } else {
            vector[2] = vector[1] = vector[0] = 0.f;
        }
    }

    private static float normOf(final float vector[]) {
        return (float)Math.sqrt(scalarProduct(vector, vector));
    }
    private static float normOf(final float vector[], final int dim) {
        return (float)Math.sqrt(scalarProduct(vector, vector, dim));
    }
    private static float normIn3D(final float vector[]) {
        return (float)Math.sqrt(scalarProductIn3D(vector, vector));
    }
    private static float scalarProduct(final float u[], final float v[]) {
        return scalarProduct(u, v, u.length);
    }
    private static float scalarProduct(final float u[], final float v[], final int dim) {
        float product = u[0] * v[0];
        for (int i = 1; i < dim; i++) {
            product += u[i] * v[i];
        }
        return product;
    }
    private static float scalarProductIn3D(final float u[], final float v[]) {
        return u[0] * v[0] + u[1] * v[1] + u[2] * v[2];
    }
    private static float[] vectorProductOnXY(final float u[], final float v[]) {
        float product[] = new float[2];

        product[0] = u[1]*v[2] - u[2]*v[1];
        product[1] = u[2]*v[0] - u[0]*v[2];

        return product;
    }
}