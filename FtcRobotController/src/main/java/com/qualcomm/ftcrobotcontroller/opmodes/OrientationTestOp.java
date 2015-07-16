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
    Sensor gyro;

    // orientation values
    private float azimut = 0.0f;       // value in radians
    private float pitch = 0.0f;        // value in radians
    private float roll = 0.0f;         // value in radians

    private float[] mGravity;       // latest sensor values
    private float[] mGeomagnetic;   // latest sensor values

    // for the gyro
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private long timestamp;
    float currentGyroRotationMatrix[] = new float[9];
    boolean hasGyroInitialOrientation;

    private float gyrox = 0.0f;
    private float gyroy = 0.0f;
    private float gyroz = 0.0f;

    // I HAVE NO IDEA IF THIS IS A GOOD VALUE OR NOT
    private static final float EPSILON = 0.03f;
    private float lastOmegaManitude;
    private float v0, v1, v2, v3;

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
        //mSensorManager = (SensorManager) FTCApplicationContextProvider.getContext().getSystemService(Context.SENSOR_SERVICE);

        /*
         Looks like the application context is available on the hardwaremap
         */
        mSensorManager = (SensorManager) hardwareMap.appContext.getSystemService(Context.SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);



        // delay value is SENSOR_DELAY_UI which is ok for telemetry, maybe not for actual robot use
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);

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
        telemetry.addData("4 azimut", "azimut = " + Math.round(Math.toDegrees(azimut)));
        telemetry.addData("5 pitch", "pitch = " + Math.round(Math.toDegrees(pitch)));
        telemetry.addData("6 roll", "roll = " + Math.round(Math.toDegrees(roll)));
        telemetry.addData("7 gyro","gyro => ("+Math.round(Math.toDegrees(gyrox))+","
                +Math.round(Math.toDegrees(gyroy))+","
                +Math.round(Math.toDegrees(gyroz))+")");
        telemetry.addData("8 omega","omega => "+lastOmegaManitude);
        telemetry.addData("9 vs","rot vectors ("+v0+","
                +v1+","
                +v2+","
                +v3+")");
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

                // NOW... we have an orientation ... if we don't yet have a current orientation for
                //  the gyro we can set it now.
                if (!hasGyroInitialOrientation) {
                    hasGyroInitialOrientation = SensorManager.getRotationMatrix(currentGyroRotationMatrix, null, mGravity, mGeomagnetic);
                }
            }
        }

        // taken entirely from http://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-gyro
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE && hasGyroInitialOrientation) {

            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {

                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

                lastOmegaManitude = omegaMagnitude;

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;


                v0 = axisX;
                v1 = axisY;
                v2 = axisZ;
                v3 = dT;

                //v0 = deltaRotationVector[0];
                //v1 = deltaRotationVector[1];
                //v2 = deltaRotationVector[2];
                //v3 = deltaRotationVector[3];
            }
            timestamp = event.timestamp;
            float[] deltaRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
            // User code should concatenate the delta rotation we computed with the current rotation
            // in order to get the updated rotation.
            // rotationCurrent = rotationCurrent * deltaRotationMatrix;

            currentGyroRotationMatrix = matrixMultiplication(currentGyroRotationMatrix,deltaRotationMatrix);

            float orientation[] = new float[3];
            SensorManager.getOrientation(currentGyroRotationMatrix, orientation);
            gyrox = orientation[0]; // orientation contains: azimut, pitch and roll
            gyroy = orientation[1];
            gyroz = orientation[2];

        }


    }

    private float[] matrixMultiplication(float[] a, float[] b) {
        float[] result = new float[9];

        result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
        result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
        result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

        result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
        result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
        result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

        result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
        result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
        result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

        return result;
    }

}
