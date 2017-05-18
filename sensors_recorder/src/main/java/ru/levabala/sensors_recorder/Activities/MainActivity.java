package ru.levabala.sensors_recorder.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import ru.levabala.sensors_recorder.Other.FileMethods;
import ru.levabala.sensors_recorder.Other.Utils;
import ru.levabala.sensors_recorder.R;
import ru.levabala.sensors_recorder.Recorder.Recorder;
import ru.levabala.sensors_recorder.Recorder.SensorType;

public class MainActivity extends AppCompatActivity {
    //some constants
    public static final long startTimeMillis = System.currentTimeMillis();
    public static final String startTimeString = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
    public static String LOCAL_SERVER_ADDRESS = "http://192.168.3.6:3000";
    public static String GLOBAL_SERVER_ADDRESS = "http://62.84.116.86:3000";
    public static String DEVICE_UNIQUE_ID = "unknown";
    public static String BUFFER_FILENAME = "buffer.dat";
    public static File EXTERNAL_BUFFER_FILE;
    public static File INTERNAL_BUFFER_FILE;
    public static int FAB_ID;

    //views
    private View fabView;
    private FloatingActionButton fab;
    private ListView listViewSensors;
    private TextView tvGPS,tvGravity,tvGyroscope,tvAcceleration,tvMagneticField;

    //variables
    private Context context;
    private Timer UIUpdateTimer;
    private Activity theActivity;
    private SharedPreferences applicationPrefs;
    private PowerManager.WakeLock mWakeLock;
    private Recorder recorder;
    private Set<String> sensorsToRecord = new ArraySet<>();

    private List<String> MY_PERMISSIONS = Arrays.asList(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK
    );;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        theActivity = (Activity)context;

        //constants setting up
        EXTERNAL_BUFFER_FILE = FileMethods.getExternalFile("buffer.dat");
        INTERNAL_BUFFER_FILE = FileMethods.getInternalFile("buffer.dat", context);
        FileMethods.checkOutFile(EXTERNAL_BUFFER_FILE);
        FileMethods.checkOutFile(INTERNAL_BUFFER_FILE);
        FAB_ID = R.id.fab;

        //wake lock
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //registering all views to local variables + some listeners
        registerViews();

        //let's init support classes and variables
        UIUpdateTimer = new Timer();
        applicationPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        //now a couple of configuration settings
        if (!applicationPrefs.contains("LOCAL_SERVER_ADDRESS"))
            applicationPrefs.edit().putString("LOCAL_SERVER_ADDRESS", LOCAL_SERVER_ADDRESS).apply();
        else LOCAL_SERVER_ADDRESS = applicationPrefs.getString("LOCAL_SERVER_ADDRESS", LOCAL_SERVER_ADDRESS);

        //let's check for exist your unique id
        if (!applicationPrefs.contains("DEVICE_UNIQUE_ID"))
            applicationPrefs.edit().putString("DEVICE_UNIQUE_ID", UUID.randomUUID().toString()).apply();
        else DEVICE_UNIQUE_ID = applicationPrefs.getString("DEVICE_UNIQUE_ID", DEVICE_UNIQUE_ID);

        //here we check out for list of sensors to record
        if (!applicationPrefs.contains("SENSORS_TO_RECORD"))
            applicationPrefs.edit().putStringSet("SENSORS_TO_RECORD", new ArraySet<>()).apply();
        else sensorsToRecord = applicationPrefs.getStringSet("SENSORS_TO_RECORD", new ArraySet<>());

        //here we set up list of sensors to record (captain obvious)
        sensorsToRecord.add(String.valueOf(Sensor.TYPE_GYROSCOPE));
        sensorsToRecord.add(String.valueOf(Sensor.TYPE_MAGNETIC_FIELD));
        sensorsToRecord.add(String.valueOf(Sensor.TYPE_ACCELEROMETER));
        sensorsToRecord.add(String.valueOf(Sensor.TYPE_GRAVITY));

        //let's request some permissions
        for (String permission : MY_PERMISSIONS)
            requestPermission(permission);

        //now we need to check out filestree
        FileMethods.checkAndCreateOurFolders(context);

        //finally we need to initialize RECORDER
        recorder = new Recorder(new ArrayList<>(), theActivity);//Utils.stringSetToArrayListInteger(sensorsToRecord), theActivity);

        //UI updates
        UIUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                /*theActivity.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                tvGPS.setText(
                                        "GPS: " + String.valueOf(FileMethods.getExternalFile(Recorder.startTimeString, "GPS.txt").length() / 1000f) + "KB"
                                );
                                tvAcceleration.setText(
                                        "Accelerometer: " + String.valueOf(FileMethods.getExternalFile(Recorder.startTimeString, "ACCELEROMETER.txt").length() / 1000f) + "KB"
                                );
                                tvGyroscope.setText(
                                        "Gyroscope: " + String.valueOf(FileMethods.getExternalFile(Recorder.startTimeString, "GYROSCOPE.txt").length() / 1000f) + "KB"
                                );
                                tvMagneticField.setText(
                                        "Magnetic field: " + String.valueOf(FileMethods.getExternalFile(Recorder.startTimeString, "MAGNETIC_FIELD.txt").length() / 1000f) + "KB"
                                );
                                tvGravity.setText(
                                        "Gravity: " + String.valueOf(FileMethods.getExternalFile(Recorder.startTimeString, "GRAVITY.txt").length() / 1000f) + "KB"
                                );
                            }
                        });*/
            }
        },0,500);
    }

    private void requestPermission(String permission){
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                //hmm...
                //what should be here?
                //text message to user why need we to take access to some smartphone features?
                Utils.logText("Please, give us the permission", context);
                ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
            } else
                ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
    }

    private void registerViews(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabView = findViewById(R.id.fab);
        fab = (FloatingActionButton) fabView;
        fab.setOnClickListener((View view) -> {
            startRecording(view);
        });

        listViewSensors = (ListView)findViewById(R.id.listViewSensors);
        tvGPS = (TextView)findViewById(R.id.textViewGPSCount);
        tvAcceleration = (TextView)findViewById(R.id.textViewAccelerationCount);
        tvGravity = (TextView)findViewById(R.id.textViewGravityCount);
        tvMagneticField = (TextView)findViewById(R.id.textViewMagneticFieldCount);
        tvGyroscope = (TextView)findViewById(R.id.textViewGyroscopeCount);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy(){
        recorder.onDestroy();
        saveChanges();
        super.onDestroy();
    }

    private void saveChanges(){
        applicationPrefs.edit().putStringSet("SENSORS_TO_RECORD", sensorsToRecord).apply();
    }

    public void startRecording(View view){
        recorder.start(true);
    }

    public void stopRecording(View view){
        recorder.stop();
    }
}
