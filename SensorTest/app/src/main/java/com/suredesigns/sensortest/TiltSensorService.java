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

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

public class TiltSensorService extends InputMethodService implements SensorEventListener {
    private static final String TAG = TiltSensorService.class.getSimpleName();
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;
    private boolean mCreated = false;
    private long T = 67;

    @Override
    public void onInitializeInterface () {
        super.onInitializeInterface();
        TiltSensorEvent tiltSensorEvent = TiltSensorEvent.getInstance();
        mSensorManager.unregisterListener(this);
        mSensorManager.registerListener(this, mGravitySensor, tiltSensorEvent.getDelayRate());
        T = (tiltSensorEvent.getDelayTime() + 500)/1000;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TiltSensorEvent tiltSensorEvent = TiltSensorEvent.getInstance();
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorManager.registerListener(this, mGravitySensor, tiltSensorEvent.getDelayRate());
        T = (tiltSensorEvent.getDelayTime() + 500)/1000;
        long downTime = SystemClock.uptimeMillis();
        tiltSensorEvent.setMotionEvent(downTime, downTime + T,
                MotionEvent.ACTION_CANCEL, tiltSensorEvent.getDisplayWidth()/2,
                tiltSensorEvent.getDisplayHeight()/2, 0);
        mCreated = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (mCreated) {
            mCreated = false;
            TiltSensorEvent.getInstance().updateGravitation(sensorEvent.values);
            TiltSensorEvent.getInstance().changeReferencePoint();
            return;
        }
        if (!TiltSensorEvent.getInstance().scrollableViewExists()) return;
        if (TiltSensorEvent.getInstance().getPrevTimestamp() == -1) {
            TiltSensorEvent.getInstance().setTimestamp(sensorEvent.timestamp);
            return;
        }
        if (TiltSensorEvent.getInstance().getStopDispatchMotionEventFlag()) {
            TiltSensorEvent.getInstance().updateGravitation(sensorEvent.values);
            TiltSensorEvent.getInstance().changeReferencePoint();
        }
        if (TiltSensorEvent.getInstance().referenceHasChanged()) {
            TiltSensorEvent.getInstance().setVelocity(0.f, 0.f);
            TiltSensorEvent.getInstance().setReferenceState(false);
            return;
        }
        TiltSensorEvent tiltSensorEvent = TiltSensorEvent.getInstance();
        long eventTime = sensorEvent.timestamp;
        MotionEvent motionEvent = tiltSensorEvent.getMotionEvent();
        int action = motionEvent.getAction();
        long downTime = motionEvent.getDownTime();
        long motionEventTime;

        tiltSensorEvent.updateGravitation(sensorEvent.values);

        if (motionEvent.getDeviceId() != 0) {
            // if prev motion event caused by physical touch screen operation, then interrupt sequence of the motion events.
            if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_CANCEL;
            }
        }

        final float tiltVector[] = tiltSensorEvent.getTiltVector();
        final int direction = TiltSensorEvent.getTiltDirection(tiltVector);

        tiltSensorEvent.detectFlingAndShake(tiltVector);
        tiltSensorEvent.updatePointerPosition(eventTime);

        float x = tiltSensorEvent.getX(), y = tiltSensorEvent.getY();

        if (action == MotionEvent.ACTION_CANCEL) {
            if (direction != TiltSensorEvent.TILT_NONE) {
                action = MotionEvent.ACTION_DOWN;
                downTime = SystemClock.uptimeMillis();
                motionEventTime = downTime;
                sendMotionEvent(tiltSensorEvent.getView(), downTime, motionEventTime, action, x, y, 0);
            } else {
                final int flingState = tiltSensorEvent.getFlingState();
                if (!tiltSensorEvent.isPhysicallyTouched() || (flingState & TiltSensorEvent.FLING_MASK) == 0) {
                    tiltSensorEvent.setVelocity(0.f, 0.f);
                    tiltSensorEvent.setFlingState(TiltSensorEvent.FLING_NONE);
                }
                motionEvent.setLocation(x, y);
                motionEvent.setAction(action);
                tiltSensorEvent.setMotionEvent(motionEvent);
            }
            return;
        } else if (direction != TiltSensorEvent.TILT_NONE) {
            action = MotionEvent.ACTION_MOVE;
        } else {
            final int flingState = tiltSensorEvent.getFlingState();
            if (!tiltSensorEvent.isPhysicallyTouched() || (flingState & TiltSensorEvent.FLING_MASK) == 0) {
                tiltSensorEvent.setVelocity(0.f, 0.f);
                tiltSensorEvent.setFlingState(TiltSensorEvent.FLING_NONE);
            }
            if (action == MotionEvent.ACTION_DOWN) {
                action = MotionEvent.ACTION_MOVE;
            } else {
                action = MotionEvent.ACTION_CANCEL;
                tiltSensorEvent.setFlingState(TiltSensorEvent.FLING_NONE);
            }
        }
        motionEventTime = SystemClock.uptimeMillis();
        motionEvent = checkBoundaryCondition(motionEvent, downTime, motionEventTime, action, x, y);
        sendMotionEvent(TiltSensorEvent.getInstance().getView(), motionEvent);
    }

    private void sendMotionEvent(View view, long downTime, long eventTime, int action, float x, float y, int metaState) {
        sendMotionEvent(view, MotionEvent.obtain(downTime, eventTime, action, x, y, metaState));
    }
    private void sendMotionEvent(View view, MotionEvent motionEvent) {
        if (view == null || motionEvent == null) return;
        motionEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        TiltSensorEvent.getInstance().setMotionEvent(motionEvent);
        view.dispatchTouchEvent(motionEvent);
        motionEvent.recycle();
    }

    private MotionEvent checkBoundaryCondition(MotionEvent motionEvent, long downTime, long motionEventTime, int action, float x, float y) {
        final TiltSensorEvent tiltSensorEvent = TiltSensorEvent.getInstance();
        final float maxX = tiltSensorEvent.getViewRight(), minX = tiltSensorEvent.getViewLeft();
        final float maxY = tiltSensorEvent.getViewBottom(), minY = tiltSensorEvent.getViewTop();

        if (x < minX || maxX < x || y < minY || maxY < y) {
            float oldX = motionEvent.getX(), oldY = motionEvent.getY();
            float oppositeBoundaryX, oppositeBoundaryY;
            float boundaryX, boundaryY;
            if (x > oldX) {
                boundaryX = maxX;
                oppositeBoundaryX = minX;
            } else {
                boundaryX = minX;
                oppositeBoundaryX = maxX;
            }
            if (y > oldY) {
                boundaryY = maxY;
                oppositeBoundaryY = minY;
            } else {
                boundaryY = minY;
                oppositeBoundaryY = maxY;
            }

            final View view = TiltSensorEvent.getInstance().getView();
            for (;;) {
                final float dX = x - oldX;
                final float dY = y - oldY;
                final float cX = (Math.abs(dX) < 0.5f) ? 1.f : (boundaryX - oldX)/dX;
                final float cY = (Math.abs(dY) < 0.5f) ? 1.f : (boundaryY - oldY)/dY;
                if (cX < 1.f && cY < 1.f) {
                    if (cX < cY) {
                        oldY += cX * dY;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_MOVE, boundaryX, oldY, 0);
                        motionEventTime += T;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_CANCEL, boundaryX, oldY, 0);
                        x += oppositeBoundaryX - boundaryX;
                        oldX = oppositeBoundaryX;
                        downTime = motionEventTime + T;
                        motionEventTime = downTime;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_DOWN, oldX, oldY, 0);
                        motionEventTime += T;
                    } else if (cX > cY) {
                        oldX += cY * dX;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_MOVE, oldX, boundaryY, 0);
                        motionEventTime += T;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_CANCEL, oldX, boundaryY, 0);
                        y += oppositeBoundaryY - boundaryY;
                        oldY = oppositeBoundaryY;
                        downTime = motionEventTime + T;
                        motionEventTime = downTime;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_DOWN, oldX, oldY, 0);
                        motionEventTime += T;
                    } else {
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_MOVE, boundaryX, boundaryY, 0);
                        motionEventTime += T;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_CANCEL, boundaryX, boundaryY, 0);
                        x += oppositeBoundaryX - boundaryX;
                        oldX = oppositeBoundaryX;
                        y += oppositeBoundaryY - boundaryY;
                        oldY = oppositeBoundaryY;
                        downTime = motionEventTime + T;
                        motionEventTime = downTime;
                        sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_DOWN, oldX, oldY, 0);
                        motionEventTime += T;
                    }
                } else if (cX < 1.f && cY >= 1.f) {
                    oldY += cX * dY;
                    sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_MOVE, boundaryX, oldY, 0);
                    motionEventTime += T;
                    sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_CANCEL, boundaryX, oldY, 0);
                    x += oppositeBoundaryX - boundaryX;
                    oldX = oppositeBoundaryX;
                    downTime = motionEventTime + T;
                    motionEventTime = downTime;
                    sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_DOWN, oldX, oldY, 0);
                    motionEventTime += T;
                } else if (cX >= 1.f && cY < 1.f) {
                    oldX += cY * dX;
                    sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_MOVE, oldX, boundaryY, 0);
                    motionEventTime += T;
                    sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_CANCEL, oldX, boundaryY, 0);
                    y += oppositeBoundaryY - boundaryY;
                    oldY = oppositeBoundaryY;
                    downTime = motionEventTime + T;
                    motionEventTime = downTime;
                    sendMotionEvent(view, downTime, motionEventTime, MotionEvent.ACTION_DOWN, oldX, oldY, 0);
                    motionEventTime += T;
                } else {
                    break;
                }
            }
        }
        motionEvent.recycle();
        return MotionEvent.obtain(downTime, motionEventTime, action, x, y, 0);
    }
}