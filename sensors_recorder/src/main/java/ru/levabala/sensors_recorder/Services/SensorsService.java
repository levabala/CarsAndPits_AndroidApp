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
import android.media.tv.TvInputService;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.SparseArray;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ru.levabala.sensors_recorder.Activities.MainActivity;
import ru.levabala.sensors_recorder.Other.FileMethods;
import ru.levabala.sensors_recorder.Other.Utils;
import ru.levabala.sensors_recorder.Recorder.DataTuple;
import ru.levabala.sensors_recorder.Recorder.SensorType;

public class SensorsService extends Service implements SensorEventListener{
    public static Map<SensorType, String> sensorsInfo = new HashMap<>();
    public static float CRITICAL_TIME;
    public static float gpsAccuracy;
    public static long startTime;
    public static double distance;

    public static final int TYPE_GPS = 999;
    private String startDate = "unknown";

    private SensorManager sensorManager;
    private List<Sensor> sensorList = new ArrayList<>();
    private SparseArray<FileOutputStream> sensorOutputStreams = new SparseArray<>();
    private LocationListener locationListener;
    private LocationManager locationManager;
    private Context context = this;

    //private Map<Integer, List<DataTuple>> buffer = new HashMap<>();


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
        startTime = System.currentTimeMillis();
        distance = 0;
        startDate = new SimpleDateFormat("yyyy-MM-dd'T'HH'h'mm'm'ss").format(Calendar.getInstance().getTime());
        ArrayList<Integer> sensorsToRecord = intent.getIntegerArrayListExtra("sensorsToRecord");
        boolean recordGPS = intent.getBooleanExtra("recordGPS", false);

        for (int sensorId: sensorsToRecord){
            FileMethods.appendToFile(
                    ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                    FileMethods.getExternalFile(startDate, SensorType.getById(sensorId).toString() + ".txt"),
                    context
            );
        }
        if (recordGPS)
            FileMethods.appendToFile(
                    ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                    FileMethods.getExternalFile(startDate, "GPS.txt"),
                    context
            );

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
        DataTuple tuplya = new DataTuple(event.values.clone(), offsetFromStart);
        int type = event.sensor.getType();
        writeDataTuplyaDown(tuplya, type);
        sensorsInfo.put(SensorType.getById(type), "1st value: " + String.valueOf(event.values[0]));
    }

    @Override
    public void onDestroy(){
        for(int i = 0; i < sensorOutputStreams.size(); i++) {
            int key = sensorOutputStreams.keyAt(i);
            FileOutputStream out = sensorOutputStreams.get(key);
            try {
                out.close();
            }
            catch (IOException ex){
                Toast.makeText(this, ex.toString() +
                                "\nCan't close output stream",
                        Toast.LENGTH_LONG).show();
            }
        }

        try {
            if (locationManager != null)
                locationManager.removeUpdates(locationListener);
            if (sensorManager != null)
                sensorManager.unregisterListener(this);
        }
        catch (Exception e){
            logText("Some error during Destroing..\n" + e.toString());
        }
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

    private Sensor registerSensor(int type, SensorManager manager) throws Resources.NotFoundException {
        Sensor sensor = manager.getDefaultSensor(type);
        if (sensor != null)
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        else
            throw new NotFoundException("Service wasn't founded");

        FileMethods.getExternalFile(startDate).mkdirs();

        int sensorId = sensor.getType();
        sensorsInfo.put(SensorType.getById(sensorId), "");
        try {
            sensorOutputStreams.put(
                    sensor.getType(),
                    new FileOutputStream(
                            FileMethods.getExternalFile(startDate, SensorType.getById(sensorId).toString() + ".txt"), true
                    )
            );
        }
        catch (FileNotFoundException ex){
            throw new NotFoundException("FileOutputStream Error");
        }
        return sensor;
    }

    private void registerGPSRecorder(){
        if (locationManager == null)
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        sensorsInfo.put(SensorType.GPS, "0km/h");

        locationListener = new LocationListener(LocationManager.GPS_PROVIDER);
        try {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } catch (SecurityException e) {
                logText("requestLocationUpdating ERROR\n" + e.toString());
            }
        }
        catch (Exception e){
            logText("requestLocationUpdating ERROR\n" + e.toString());
        }

        try {
            sensorOutputStreams.put(
                    TYPE_GPS,
                    new FileOutputStream(FileMethods.getExternalFile(startDate, "GPS.txt"), true)
            );
        }
        catch (FileNotFoundException ex){
            logText("Can't register GPS FileOutputStream");
        }
    }

    private void startRecording(){

    }

    private void pauseRecording(){

    }

    //location listener
    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation = null;

        public LocationListener(String provider)
        {
            long millis = System.currentTimeMillis();
        }

        @Override
        public void onLocationChanged(Location location)
        {
            gpsAccuracy = location.getAccuracy();
            if (isBetterLocation(location,mLastLocation)){
                if (mLastLocation == null) mLastLocation = location;

                double deltaDistance = calcDistance(
                        location.getLatitude(), mLastLocation.getLatitude(),
                        location.getLongitude(), mLastLocation.getLongitude(),
                        location.getAltitude(), mLastLocation.getAltitude()) / 1000f;
                double deltaTime = (double)(location.getTime() - mLastLocation.getTime());
                double speed = deltaDistance / deltaTime;

                distance += deltaDistance;

                String str = String.format(Locale.US, "%.1fkm/h", speed);
                sensorsInfo.put(SensorType.GPS, str);

                mLastLocation = location;
                long nowTime = System.currentTimeMillis();
                int startTimeOffset = (int)(nowTime - startTime);
                DataTuple gpsTuplya = new DataTuple(new float[]{
                        (float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), (float)distance, (float)speed
                }, startTimeOffset);

                writeDataTuplyaDown(gpsTuplya, TYPE_GPS);
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

        public double calcDistance(double lat1, double lat2, double lon1,
                                      double lon2, double el1, double el2) {

            final int R = 6371; // Radius of the earth

            double latDistance = Math.toRadians(lat2 - lat1);
            double lonDistance = Math.toRadians(lon2 - lon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = R * c * 1000; // convert to meters

            double height = el1 - el2;

            distance = Math.pow(distance, 2) + Math.pow(height, 2);

            return Math.sqrt(distance);
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

    private void writeDataTuplyaDown(DataTuple tuplya, int sensorType){
        try {
            sensorOutputStreams.get(sensorType).write(
                    (tuplya.toString() + '\n').getBytes()
            );
        }
        catch (IOException ex){
            Utils.logText(ex.toString(), context);
        }
    }

    private void logText(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
