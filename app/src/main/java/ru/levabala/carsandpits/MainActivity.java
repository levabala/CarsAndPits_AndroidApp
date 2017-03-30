package ru.levabala.carsandpits;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ChunkSnap{
    public Location location;
    public long timems;
    public Float[][] accelerationDeltas;

    public ChunkSnap(Location l, long t, Float[][] ad){
        location = l;
        timems = t;
        accelerationDeltas = ad;
    }
}

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float deltaX, deltaY, deltaZ, lastX, lastY, lastZ, maxDeltaX, maxDeltaY, maxDeltaZ;
    private TextView tvX, tvY, tvZ, tvHashMapSize, tvZBuffer;

    private List<Float> deltaXBuffer, deltaYBuffer, deltaZBuffer;
    private Location currentLocation;
    private List<ChunkSnap> chunks;
    int a = 0;
    int b = 20;
    boolean isFirst = true;

    private PowerManager.WakeLock mWakeLock;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toast.makeText(this,getApplicationInfo().dataDir,Toast.LENGTH_LONG).show();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        mWakeLock.acquire(); //doesn't actually work!

        Locale.setDefault(Locale.US);
        chunks = new ArrayList<>();

        maxDeltaX = maxDeltaY = maxDeltaZ = 0;

        deltaXBuffer = new ArrayList<>();
        deltaYBuffer = new ArrayList<>();
        deltaZBuffer = new ArrayList<>();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            //Toast.makeText(this, "Accelerometer founded", Toast.LENGTH_SHORT).show();
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "No accelerometer founded", Toast.LENGTH_LONG).show();
            return;
        }

        tvX = (TextView) findViewById(R.id.textViewX);
        tvY = (TextView) findViewById(R.id.textViewY);
        tvZ = (TextView) findViewById(R.id.textViewZ);
        tvZBuffer = (TextView) findViewById(R.id.textViewZBuffer);
        tvHashMapSize = (TextView) findViewById(R.id.textViewHashMapSize);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.

                if (isFirst) {
                    isFirst = false;
                    ChunkSnap chunk = new ChunkSnap(location, System.currentTimeMillis(), new Float[3][]);
                    chunks.add(chunk);
                    writeBufferDown(stringifyChunk(chunk));
                    return;
                }

                if (isBetterLocation(location, currentLocation)) {
                    currentLocation = location;
                    Float[][] accelerations = new Float[3][];
                    accelerations[0] = deltaXBuffer.toArray(new Float[deltaXBuffer.size()]);
                    accelerations[1] = deltaYBuffer.toArray(new Float[deltaYBuffer.size()]);
                    accelerations[2] = deltaZBuffer.toArray(new Float[deltaZBuffer.size()]);
                    ChunkSnap chunk = new ChunkSnap(location, System.currentTimeMillis(), accelerations);
                    chunks.add(chunk);
                    writeBufferDown(stringifyChunk(chunk));
                    /*final Toast t = Toast.makeText(getApplicationContext(),"New location\nDataCount:" + deltaXBuffer.size(), Toast.LENGTH_SHORT);
                    t.show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            t.cancel();
                        }
                    }, 100);*/

                    tvHashMapSize.setText(String.format("%d", chunks.size()));
                    String str = "";
                    for (Float f : deltaZBuffer)
                        str += f + " ";
                    tvZBuffer.setText(str);

                    maxDeltaX = maxDeltaY = maxDeltaZ = 0;
                    deltaXBuffer.clear();
                    deltaYBuffer.clear();
                    deltaZBuffer.clear();
                } else
                    Toast.makeText(getApplicationContext(), "Bad location", Toast.LENGTH_SHORT).show();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*if (a < b) {
            a++;
            return;
        }
        a = 0;*/
        // get the change of the x,y,z values of the accelerometer
        deltaX = Math.abs(lastX - event.values[0]);
        deltaY = Math.abs(lastY - event.values[1]);
        deltaZ = Math.abs(lastZ - event.values[2]);

        // if the change is below 2, it is just plain noise
        if (deltaX < 0.08)
            deltaX = 0;
        if (deltaY < 0.08)
            deltaY = 0;
        if (deltaZ < 0.08)
            deltaZ = 0;

        if (deltaX > maxDeltaX) maxDeltaX = deltaX;
        if (deltaX > maxDeltaY) maxDeltaY = deltaY;
        if (deltaY > maxDeltaZ) maxDeltaZ = deltaZ;

        deltaXBuffer.add(deltaX);
        deltaYBuffer.add(deltaY);
        deltaZBuffer.add(deltaZ);

        // set the last know values of x,y,z
        lastX = event.values[0];
        lastY = event.values[1];
        lastZ = event.values[2];

        tvX.setText(String.format("%f", deltaX));
        tvY.setText(String.format("%f", deltaY));
        tvZ.setText(String.format("%f", deltaZ));

    }

    private static final int CRITICAL_TIME = 1000 * 60; //20 seconds

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

    public void saveData(View view){
        resetTrack();
        saveAllBuffer();
    }

    public void ClearAllTracks(View view){
        new AlertDialog.Builder(this)
                .setTitle("Deleting")
                .setMessage("Delete all your tracks?")
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create()
                .show();
    }

    public void resetData(View view){
        resetTrack();
    }

    private void resetTrack(){
        deltaX = deltaY = deltaZ = 0;

        deltaXBuffer.clear();
        deltaYBuffer.clear();
        deltaZBuffer.clear();

        chunks = new ArrayList<>();
    }

    private void writeBufferDown(String str) {
        //writeToFile(str, now+".txt", getApplicationContext());
        writeToFile(str,"buffer.txt",this);
        /*
        try {
            FileOutputStream fileout = openFileOutput("buffer.txt", MODE_APPEND);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
            outputWriter.write(str);
            outputWriter.close();
        } catch (Exception e) {
            logText(e.toString());
        }*/
    }

    private void saveAllBuffer(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        String now = df.format(new Date());
        String filename = now + ".txt";
        //logText("Buffer was written to " + filename);

        String buffer = readFromFile(this,"buffer.txt");
        writeToFile(buffer,filename,this);
        writeToFile(filename + '|', "listOfTracks.txt",this);

        logText("All your tracks:\n" + readFromFile(this,"listOfTracks.txt").replaceAll("\\|", "\n"));
    }

    private String stringifyData(List<ChunkSnap> chunksArray) {
        /*
        Format is
              lat1;lng1;accTime1_1;accX1_1;accY1_1;accZ1_1;accTime1_2;accX1_2;accY1_2;accZ1_2;lat2;lng2;accTime2_1;accX2_1;accY2_1;accZ2_1;
              or
              lat1;lng1;accTime1;accX1_1;accY1_1;accZ1_1;accX1_2;accY1_2;accZ1_2;lat2;lng2;accTime2;accX2_1;accY2_1;accZ2_1;
         */
        String str = "";
        for (ChunkSnap csnap : chunks)
            str += stringifyChunk(csnap);
        return str;
    }

    private String stringifyChunk(ChunkSnap chunk){
        Location l = chunk.location;
        Float[][] accelerationData = chunk.accelerationDeltas;
        long time = chunk.timems;

        String accelerations = "";
        if (accelerationData[0] != null && accelerationData[0].length > 0)
            for (int i = 0; i < accelerationData[0].length; i++)
                for (int ii = 0; ii < accelerationData.length; ii++)
                    accelerations += accelerationData[ii][i] + ";";//String.format("%f;%f;%f;", accelerationData[i][0],accelerationData[i][1],accelerationData[i][2]);
        return String.format("%f;%f;%d;%s|", l.getLatitude(), l.getLongitude(), time, accelerations);
    }

    private void writeToFile(String data, String filename, Context context) {
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readFromFile(Context context, String filename) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(filename);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }



    private void logText(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }



    public void GoToSendActivity(View view){
        Intent sendIntent = new Intent(this, SendActivity.class);
        startActivity(sendIntent);
    }
}
