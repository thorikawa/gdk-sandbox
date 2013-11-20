package com.polysfactory.gdksandbox.headgesture;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class HeadGestureDetector implements SensorEventListener {
    private static final int MATRIX_SIZE = 16;
    private float[] inR = new float[MATRIX_SIZE];
    private float[] outR = new float[MATRIX_SIZE];
    private float[] I = new float[MATRIX_SIZE];

    private float[] orientationValues = new float[3];
    private float[] magneticValues = new float[3];
    private float[] accelerometerValues = new float[3];

    private SensorManager mSensorManager;

    private OnHeadGestureListener mListener;
    private OrientationEvent mPreviousStableOrientation;
    private OrientationEvent mPreviousNodOrientation;

    private static class OrientationEvent {
        float[] orientationValues;
        long timestamp;

        public OrientationEvent(float[] orientationValues, long timestamp) {
            this.orientationValues = orientationValues;
            this.timestamp = timestamp;
        }
    }

    public HeadGestureDetector(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void start() {
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : sensors) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            }

            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    public void setHeadGestureListener(OnHeadGestureListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
            return;

        switch (event.sensor.getType()) {
        case Sensor.TYPE_MAGNETIC_FIELD:
            magneticValues = event.values.clone();
            break;
        case Sensor.TYPE_ACCELEROMETER:
            accelerometerValues = event.values.clone();
            break;
        }

        if (magneticValues != null && accelerometerValues != null) {

            SensorManager.getRotationMatrix(inR, I, accelerometerValues, magneticValues);

            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
            SensorManager.getOrientation(outR, orientationValues);

            Log.v(Constants.TAG, Arrays.toString(orientationValues));

            if (!isPutOn(orientationValues)) {
                Log.d(Constants.TAG, "Looks like glass is off?");
                mPreviousStableOrientation = null;
                mPreviousNodOrientation = null;
            }

            if (isStable(orientationValues)) {
                mPreviousStableOrientation = new OrientationEvent(orientationValues, event.timestamp);
            } else if (isNod(orientationValues)) {
                mPreviousNodOrientation = new OrientationEvent(orientationValues, event.timestamp);
            }

            OrientationEvent currentOrientation = new OrientationEvent(orientationValues, event.timestamp);
            if (isConsideredNod(currentOrientation, mPreviousStableOrientation, mPreviousNodOrientation)) {
                // TODO prevent duplicate detection
                mPreviousNodOrientation = null;
                Log.d(Constants.TAG, "Detect Nod!");
                if (mListener != null) {
                    mListener.onNod();
                }
            }
        }
    }

    private static final float maxStableRadian = 0.10F;

    private static final float nodBorderRadian = 0.25F;

    private static final float maxPutOnPitchRadian = 0.35F;

    private static final float maxPutOnRollRadian = 0.75F;

    // private

    private static boolean isStable(float[] orientationValues) {
        if (Math.abs(orientationValues[1]) < maxStableRadian) {
            return true;
        }
        return false;
    }

    private static boolean isNod(float[] orientationValues) {
        if (orientationValues[1] > nodBorderRadian) {
            return true;
        }
        return false;
    }

    private static boolean isPutOn(float[] orientationValues) {
        if (orientationValues[1] < maxPutOnPitchRadian && Math.abs(orientationValues[2]) < maxPutOnRollRadian) {
            return true;
        }
        return false;
    }

    private boolean isConsideredNod(OrientationEvent currentOrientation, OrientationEvent previousStable,
            OrientationEvent previousNod) {
        if (currentOrientation == null || previousStable == null || previousNod == null) {
            return false;
        }
        if (previousNod.timestamp < previousStable.timestamp || currentOrientation.timestamp < previousStable.timestamp
                || currentOrientation.timestamp < previousNod.timestamp) {
            return false;
        }
        if (currentOrientation.timestamp - previousStable.timestamp > 300000000) {
            Log.d(Constants.TAG, "timeout:" + currentOrientation.timestamp + "," + previousStable.timestamp + ", "
                    + (currentOrientation.timestamp - previousStable.timestamp) + " nanosecs ellapsed.");
            return false;
        }
        return true;
    }
}
