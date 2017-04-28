package ru.levabala.carsandpits_light;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SensorsService extends Service implements SensorEventListener {
    public static float gpsAccuracy = 0;
    public static long startTime = 0;
    public static List<RoutePoint> totalRoute = new ArrayList<>();
    public List<Point3dWithTime> dataBuffer = new ArrayList<>();

    private long lastTime = System.currentTimeMillis();

    //region Location detecting
    private static final int CRITICAL_TIME = 1000 * 2; //2s seconds
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0f;
    private LocationManager mLocationManager = null;
    private Messenger replyMessanger;
    private Context context = this;

    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magneticField;
    private Sensor gravity;

    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            //logText("LocationListener created");

            long millis = System.currentTimeMillis();
            ByteBuffer buffer = ByteBuffer.allocate(64/8);
            buffer.putLong(millis);
            startTime = millis;
            lastTime = millis;
            FileMethods.appendToFile(buffer.array(), MainActivity.BUFFER_FILE, context);

            /*try {
                mLastLocation = mLocationManager.getLastKnownLocation(provider);
            }
            catch (SecurityException e){
                logText("ERROR " + e.toString());
            }

            gpsAccuracy = mLastLocation.getAccuracy();

            generateAndSaveRoutePoint(mLastLocation);

            totalRoute.add(new RoutePoint(
                    new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                    new Point3dWithTime[0]));*/
        }

        @Override
        public void onLocationChanged(Location location)
        {
            //Log.e(TAG, "onLocationChanged: " + location);
            gpsAccuracy = location.getAccuracy();
            if (isBetterLocation(location,mLastLocation)){
                mLastLocation = location;
                generateAndSaveRoutePoint(location);
            }
            //else logText("Bad location");
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            //Log.e(TAG, "onProviderDisabled: " + provider);
            logText("onProviderDisabled: " + provider);
            sendMessage("onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            //Log.e(TAG, "onProviderEnabled: " + provider);
            logText("onProviderEnabled: " + provider);
            sendMessage("onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            //Log.e(TAG, "onStatusChanged: " + provider);
            //logText("onStatusChanged: " + provider);
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > CRITICAL_TIME;
        boolean isSignificantlyOlder = timeDelta < -CRITICAL_TIME;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private LocationListener mLocationListener;

    private void initializeLocationManager() {
        if (mLocationManager == null)
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    }
    //endregion

    //region Other sensors detecting
    private float[] gravityValues = null;
    private float[] magneticValues = null;

    @Override
    public void onSensorChanged(SensorEvent event) {
        //logText("Sensor event\nSensor: " + String.valueOf(event.sensor.getType()));

        if ((gravityValues != null) && (magneticValues != null)
                && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {

            float[] deviceRelativeAcceleration = new float[4];
            deviceRelativeAcceleration[0] = event.values[0];
            deviceRelativeAcceleration[1] = event.values[1];
            deviceRelativeAcceleration[2] = event.values[2];
            deviceRelativeAcceleration[3] = 0;

            // Change the device relative acceleration values to earth relative values
            // X axis -> East
            // Y axis -> North Pole
            // Z axis -> Sky

            float[] R = new float[16], I = new float[16], earthAcc = new float[16];

            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

            float[] inv = new float[16];

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);

            long nowTime = System.currentTimeMillis();
            long deltaTime = nowTime - lastTime;
            lastTime = nowTime;

            int deltaTimeInteger = 0;
            //let's try to convert to integer
            try{
                deltaTimeInteger = (int)deltaTime;
            }
            catch (Exception e){
                deltaTimeInteger = -1; //now we now that there's error with overflowing
                logText("ERROR " + e.toString());
            }


            dataBuffer.add(new Point3dWithTime(earthAcc[0],earthAcc[1],earthAcc[2], deltaTimeInteger));
            //Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //endregion

    private void generateAndSaveRoutePoint(Location l){
        //logText("dataBuffer.size: " + String.valueOf(dataBuffer.size()));
        Point3dWithTime[] dataBufferArray = new Point3dWithTime[dataBuffer.size()];
        dataBuffer.toArray(dataBufferArray);
        RoutePoint rp = new RoutePoint(new LatLng(l.getLatitude(), l.getLongitude()), dataBufferArray);
        dataBuffer.clear();

        totalRoute.add(rp);
        //sendMessage(totalRoute.size());

        byte[] data = RoutePoint.getBytes(rp);
        FileMethods.appendToFile(data, MainActivity.BUFFER_FILE, this);
    }

    public SensorsService() {
        //logText("Create");
    }

    @Override
    public void onCreate(){

    }

    private void init(){
        if (!FileMethods.isFileEmpty(MainActivity.BUFFER_FILE, 64 / 8)){ //long in bytes
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
            String formattedDate = df.format(c.getTime());

            FileMethods.copyFromTo(MainActivity.BUFFER_FILE, FileMethods.getExternalFile(formattedDate + ".dat"), context);
            FileMethods.clearFile(MainActivity.BUFFER_FILE, context);
        }
        else {
            //logText("Clear");
            FileMethods.clearFile(MainActivity.BUFFER_FILE, context);
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        /*String str = "";
        List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : list)
            str += s.getStringType() + "\n";
        Toast.makeText(this,str,Toast.LENGTH_LONG).show();*/

        //registering accelerometer
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "No accelerometer founded", Toast.LENGTH_LONG).show();
            return;
        }
        //registering magnetic field sensor
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            gravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            mSensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "No gravity sensor founded", Toast.LENGTH_LONG).show();
            return;
        }
        //registering gravity sensor
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "No magnetic sensor founded", Toast.LENGTH_LONG).show();
            return;
        }

        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        initializeLocationManager();
        requestLocationUpdating();
    }

    private void requestLocationUpdating(){
        //logText("requestLocationUpdating");

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
        }
        catch (SecurityException e){
            logText("requestLocationUpdating ERROR\n" + e.toString());
        }

        //logText("requestLocationUpdating done");
    }

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            replyMessanger = msg.replyTo; //init reply messenger

            if (msg.obj.equals("Pause")) Pause();
            else if (msg.obj.equals("Resume")) Resume();
            //sendRouteLength();
        }
    }

    private void sendRouteLength() {
        if (replyMessanger != null)
            try {
                Message message = new Message();
                message.obj = totalRoute.size();
                replyMessanger.send(message);//replying / sending msg to activity
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }

    private void sendMessage(Object obj){
        if (replyMessanger != null)
            try {
                Message message = new Message();
                message.obj = obj;
                replyMessanger.send(message);//replying / sending msg to activity
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }
    Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //logText("Service starting");
        init();
        return super.onStartCommand(intent,flags,startId);
    }

    private void logText(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    boolean isRunning = true;
    private void Pause(){
        if (!isRunning) return;
        mLocationManager.removeUpdates(mLocationListener);
        isRunning = false;
        logText(String.valueOf(isRunning));
    }

    private void Resume(){
        if (isRunning) return;
        requestLocationUpdating();
        isRunning = true;
        logText(String.valueOf(isRunning));
    }
}
