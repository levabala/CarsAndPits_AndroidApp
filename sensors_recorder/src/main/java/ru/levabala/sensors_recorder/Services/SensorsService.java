package ru.levabala.sensors_recorder.Services;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.levabala.sensors_recorder.Recorder.DataTuple;

public class SensorsService extends Service implements SensorEventListener{
    public static float CRITICAL_TIME;
    public static float gpsAccuracy;

    public static final int TYPE_GPS = 999;
    public static final long startTime = System.currentTimeMillis();;

    private SensorManager sensorManager;
    private List<Sensor> sensorList = new ArrayList<>();
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Context context = this;

    private Map<Integer, List<DataTuple>> buffer = new HashMap<>();


    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public SensorsService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SensorsService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<Integer> sensorsToRecord = intent.getIntegerArrayListExtra("sensorsToRecord");
        boolean recordGPS = intent.getBooleanExtra("recordGPS", false);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerSensors(sensorsToRecord);
        if (recordGPS) registerGPSRecorder();

        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int offsetFromStart = (int)(System.currentTimeMillis() - startTime);
        buffer.get(event.sensor.getType()).add(new DataTuple(
                event.values.clone(), offsetFromStart
        ));
    }

    private void registerSensors(ArrayList<Integer> sensorsToRecord){
        for (int type : sensorsToRecord)
            try{
                sensorList.add(registerSensor(type, sensorManager));
            }
            catch (NotFoundException e){
                Toast.makeText(this, e.toString() +
                        "\nSensor " + String.valueOf(type) + " cannot be obtained\naborting...",
                        Toast.LENGTH_LONG).show();
            }
    }

    private Sensor registerSensor(int type, SensorManager manager) throws Resources.NotFoundException{
        Sensor sensor = manager.getDefaultSensor(type);
        if (sensor != null)
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        else
            throw new NotFoundException("Service wasn't founded");

        buffer.put(type, new ArrayList<>());

        return sensor;
    }

    private void registerGPSRecorder(){
        if (locationManager == null)
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener(LocationManager.GPS_PROVIDER);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        catch (SecurityException e){
            logText("requestLocationUpdating ERROR\n" + e.toString());
        }

        buffer.put(TYPE_GPS, new ArrayList<>());
    }

    private void startRecording(){

    }

    private void pauseRecording(){

    }


    //client methods
    public HashMap<Integer, List<DataTuple>> getAndClearBuffer(){
        HashMap<Integer, List<DataTuple>> output = new HashMap<>(buffer);
        for(Map.Entry<Integer, List<DataTuple>> entry : buffer.entrySet()) {
            buffer.put(entry.getKey(), new ArrayList<>());
        }
        return output;
    }


    //location listener
    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            long millis = System.currentTimeMillis();
        }

        @Override
        public void onLocationChanged(Location location)
        {
            gpsAccuracy = location.getAccuracy();
            if (isBetterLocation(location,mLastLocation)){
                mLastLocation = location;

                long nowTime = System.currentTimeMillis();
                int startTimeOffset = (int)(nowTime - startTime);
                buffer.get(TYPE_GPS).add(
                        new DataTuple(new float[]{(float)location.getLatitude(), (float)location.getLongitude()}, startTimeOffset)
                );
            }
        }

        @Override
        public void onProviderDisabled(String provider)
        {

        }

        @Override
        public void onProviderEnabled(String provider)
        {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {

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

    private void logText(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
