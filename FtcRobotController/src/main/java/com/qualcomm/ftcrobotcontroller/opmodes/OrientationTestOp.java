/*

Based on original demonstration code by acharraggi.

See my_ftc_app/FtcRobotController/src/main/java/com/qualcomm/ftcrobotcontroller/opmodes/OrientOp.java

 */

package com.qualcomm.ftcrobotcontroller.opmodes;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.qualcomm.ftcrobotcontroller.FTCApplicationContextProvider;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An empty op mode serving as a template for custom OpModes
 */
public class OrientationTestOp extends OpMode implements SensorEventListener {
    private String startDate;
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    // orientation values
    private float azimut = 0.0f;       // value in radians
    private float pitch = 0.0f;        // value in radians
    private float roll = 0.0f;         // value in radians

    private float[] mGravity;       // latest sensor values
    private float[] mGeomagnetic;   // latest sensor values

    /*
    * Constructor
    */
    public OrientationTestOp() {

    }

    /*
    * Code to run when the op mode is first enabled goes here
    * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#start()
    */
    @Override
    public void start() {
        startDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());

        /* Changed from original version by acharraggi.  Original version used a static reference
        *  to the Activity context, which can theoretically memory leak.  See the comments in
        *  FTCApplicationContextProvider for more info.
        */
        mSensorManager = (SensorManager) FTCApplicationContextProvider.getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // delay value is SENSOR_DELAY_UI which is ok for telemetry, maybe not for actual robot use
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);

    }

    /*
    * This method will be called repeatedly in a loop
    * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#loop()
    */
    @Override
    public void loop() {
        telemetry.addData("1 Start", "OrientOp started at " + startDate);
        if (mGravity == null) {
            telemetry.addData("2 Gravity", "Gravity sensor values null ");
        } else {
            telemetry.addData("2 Gravity", "Gravity sensor returning values " );
        }
        if (mGravity == null) {
            telemetry.addData("3 Geomagnetic", "Geomagnetic sensor values null ");
        } else {
            telemetry.addData("3 Geomagnetic", "Geomagnetic sensor returning values " );
        }
        telemetry.addData("4 azimut", "azimut = "+Math.round(Math.toDegrees(azimut)));
        telemetry.addData("5 pitch", "pitch = "+Math.round(Math.toDegrees(pitch)));
        telemetry.addData("6 roll", "roll = "+Math.round(Math.toDegrees(roll)));
    }

    /*
    * Code to run when the op mode is first disabled goes here
    * @see com.qualcomm.robotcore.eventloop.opmode.OpMode#stop()
    */
    @Override
    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not sure if needed, placeholder just in case
    }

    public void onSensorChanged(SensorEvent event) {
        // we need both sensor values to calculate orientation
        // only one value will have changed when this method called, we assume we can still use the other value.
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        if (mGravity != null && mGeomagnetic != null) {  //make sure we have both before calling getRotationMatrix
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                pitch = orientation[1];
                roll = orientation[2];
            }
        }
    }
}
