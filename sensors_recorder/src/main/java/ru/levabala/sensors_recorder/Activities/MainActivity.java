package ru.levabala.sensors_recorder.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.UUID;

import ru.levabala.sensors_recorder.Other.FileMethods;
import ru.levabala.sensors_recorder.Other.Utils;
import ru.levabala.sensors_recorder.R;

public class MainActivity extends AppCompatActivity {
    //some constants
    public static String LOCAL_SERVER_ADDRESS = "http://192.168.3.6:3000";
    public static String GLOBAL_SERVER_ADDRESS = "http://62.84.116.86:3000";
    public static String DEVICE_UNIQUE_ID = "unknown";

    //views
    private View fabView;
    private FloatingActionButton fab;
    private ListView listViewSensors;

    //variables
    private Context context;
    private Timer UIUpdateTimer;
    private Activity theActivity;
    private SharedPreferences applicationPrefs;
    private PowerManager.WakeLock mWakeLock;

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

        //let's request some permissions
        for (String permission : MY_PERMISSIONS)
            requestPermission(permission);

        //now we need to check out filestree
        FileMethods.checkAndCreateOurFolders(context);
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
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        listViewSensors = (ListView)findViewById(R.id.listViewSensors);
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

    public void startRecording(){

    }
}
