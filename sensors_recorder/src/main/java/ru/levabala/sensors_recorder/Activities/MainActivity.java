package ru.levabala.sensors_recorder.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import ru.levabala.sensors_recorder.Other.CallbackInterface;
import ru.levabala.sensors_recorder.Other.FileMethods;
import ru.levabala.sensors_recorder.Other.ToggleButtonClickSafe;
import ru.levabala.sensors_recorder.Other.Utils;
import ru.levabala.sensors_recorder.Other.WakeLocker;
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
    private ListView listViewSensorsToRecord;
    private TextView tvGPS,tvGravity,tvGyroscope,tvAcceleration,tvMagneticField;
    private ToggleButtonClickSafe tgButton;

    //variables
    private Context context;
    private Timer UIUpdateTimer;
    private Activity theActivity;
    private SharedPreferences applicationPrefs;
    private PowerManager.WakeLock mWakeLock;
    private Recorder recorder;
    private ArrayList<SensorType> sensorsToRecord = new ArrayList<>();
    private Map<SensorType, String> sensorsInfo = new HashMap<>();
    private ArrayAdapter sensorsAdapter;
    private ArrayList<SensorType> availableSensors = new ArrayList<>();
    private boolean recordGPS = false;

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
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //let's init support classes and variables
        UIUpdateTimer = new Timer();
        applicationPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        //obtaining all sensors
        List<Sensor> allSensors = ((SensorManager)getSystemService(Context.SENSOR_SERVICE)).getSensorList(Sensor.TYPE_ALL);
        availableSensors.add(SensorType.GPS);
        for (Sensor sensor : allSensors) {
            SensorType sensorType = SensorType.getById(sensor.getType());
            if (sensorType != SensorType.UNKNOWN && !availableSensors.contains(sensorType))
                availableSensors.add(sensorType);
            //else Utils.logText("Unknown sensor: " + sensor.getName() + "(" + String.valueOf(sensor.getType()) + ")", context);
        }

        //here we set up list of sensors to record (captain obvious)
        /*sensorsToRecord.add(SensorType.ACCELEROMETER);
        sensorsToRecord.add(SensorType.MAGNETIC_FIELD);*/
        recorder = new Recorder(sensorsToRecord, theActivity);

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
        else {
            ArrayList<Integer> sensors = Utils.stringSetToArrayListInteger(
                    applicationPrefs.getStringSet("SENSORS_TO_RECORD", new ArraySet<>()));
            for (Integer i : sensors)
                sensorsToRecord.add(SensorType.getById(i));
        }

        //also need to check if we have been recording GPS last session
        if (!applicationPrefs.contains("RECORD_GPS"))
            applicationPrefs.edit().putBoolean("RECORD_GPS", recordGPS).apply();
        else recordGPS = applicationPrefs.getBoolean("RECORD_GPS", true);

        //registering all views to local variables + some listeners
        registerViews();

        //let's request some permissions
        for (String permission : MY_PERMISSIONS)
            requestPermission(permission);

        //now we need to check out filestree
        FileMethods.checkAndCreateOurFolders(context);

        //UI updates
        UIUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                theActivity.runOnUiThread(() ->
                {
                    for (int i = 0; i < listViewSensorsToRecord.getCount(); i++){
                        Map<SensorType, String> sensorsInfo = recorder.getSensorsInfo();
                        View child = listViewSensorsToRecord.getChildAt(i);
                        if (child != null) {
                            SensorsAdapter.ViewHolder holder =
                                    (SensorsAdapter.ViewHolder) child.getTag();
                            SensorType sensorType = (SensorType)holder.name.getTag();
                            String info = "(" + (sensorsInfo.containsKey(sensorType) ? sensorsInfo.get(sensorType) : "") + ")";
                            holder.info.setText(info);
                        }
                    }

                });
            }
        },0,1500);

        clearConfigs();
    }

    @Override
    public void onResume(){
        super.onResume();

        Locale locale = new Locale("US");
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
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
            //startRecording(view);
        });

        tgButton = (ToggleButtonClickSafe)findViewById(R.id.toggleButton);
        tgButton.setCallback(new CallbackInterface() {
            @Override
            public void run() {
                switchRecordingState(tgButton);
            }
        });

        listViewSensorsToRecord = (ListView)findViewById(R.id.listViewSensorsToRecord);
        sensorsAdapter = new SensorsAdapter(this,
                R.layout.sensor_info, availableSensors);
        listViewSensorsToRecord.setAdapter(sensorsAdapter);


        listViewSensorsToRecord.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                SensorType sensorType = (SensorType) parent.getItemAtPosition(position);
                /*Toast.makeText(getApplicationContext(),
                        "Clicked on Row: " + sensorType.toString(),
                        Toast.LENGTH_LONG).show();*/
            }
        });
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
        Set<String> sensors = new ArraySet<>();
        for (SensorType stype : sensorsToRecord)
            sensors.add(String.valueOf(stype.getType()));
        applicationPrefs.edit().putStringSet("SENSORS_TO_RECORD", sensors).apply();
        applicationPrefs.edit().putBoolean("RECORD_GPS", recordGPS).apply();
    }

    private void clearConfigs(){
        applicationPrefs.edit().putStringSet("SENSORS_TO_RECORD", new ArraySet<>()).apply();
    }

    public void updateSensorsInfo(){

    }

    public void switchRecordingState(View view){
        if (((ToggleButton)view).isChecked())
            startRecording(view);
        else stopRecording(view);
    }

    public void startRecording(View view){
        recorder = new Recorder(sensorsToRecord, theActivity);
        recorder.start(recordGPS, context);
    }

    public void sendFilesToServer(View view){
        //Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
        //Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS, 0);

        /*WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = 0;
        getWindow().setAttributes(params);*/

        if(Build.VERSION.SDK_INT < 19){
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else {
            //for higher api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }

        WakeLocker.acquire(context);
    }

    public void showDataFolder(View view){
        Uri selectedUri = Uri.parse(FileMethods.getExternalAppFolder().getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
            startActivity(intent);
        else
            Utils.logText("No file explorer on your device!", context);
    }

    public void stopRecording(View view){
        recorder.stop();
    }

    private class SensorsAdapter extends ArrayAdapter<SensorType> {

        private ArrayList<SensorType> sensorsList;

        public SensorsAdapter(Context context, int textViewResourceId,
                               ArrayList<SensorType> sensorsList) {
            super(context, textViewResourceId, sensorsList);
            this.sensorsList = new ArrayList<SensorType>();
            this.sensorsList.addAll(sensorsList);
        }

        private class ViewHolder {
            TextView info;
            CheckBox name;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.sensor_info, null);

                holder = new ViewHolder();
                holder.info = (TextView) convertView.findViewById(R.id.info);
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
                convertView.setTag(holder);

                holder.name.setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v ;
                        SensorType sensorType = (SensorType) cb.getTag();

                        /*Utils.logText(
                                "Clicked on Checkbox: " + cb.getText() +
                                        " is " + cb.isChecked(),
                                context);*/

                        if (recorder.serviceIsRunning)
                            cb.setChecked(!cb.isChecked());
                        else
                            if (sensorType == SensorType.GPS)
                                recordGPS = cb.isChecked();
                            else
                                if (cb.isChecked())
                                    sensorsToRecord.add(sensorType);
                                else sensorsToRecord.remove(sensorType);
                        saveChanges();
                    }
                });
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (position < sensorsList.size()) {
                SensorType sensorType = sensorsList.get(position);
                holder.info.setText("(info'll be here)");
                holder.name.setText(sensorType.toString());
                holder.name.setChecked(sensorsToRecord.contains(sensorType) || (sensorType == SensorType.GPS && recordGPS));
                holder.name.setTag(sensorType);
            }

            return convertView;

        }

    }

    private void log(String text){
        Utils.log(text);
    }
}

//TODO: Implement an ability of sending data to the server
