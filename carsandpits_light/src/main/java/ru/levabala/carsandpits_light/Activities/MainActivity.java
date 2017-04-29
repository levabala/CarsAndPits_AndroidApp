package ru.levabala.carsandpits_light.Activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import ru.levabala.carsandpits_light.Other.CallbackInterface;
import ru.levabala.carsandpits_light.Other.FileMethods;
import ru.levabala.carsandpits_light.Other.Point3dWithTime;
import ru.levabala.carsandpits_light.R;
import ru.levabala.carsandpits_light.Route.RoutePoint;
import ru.levabala.carsandpits_light.Route.RouteSender;
import ru.levabala.carsandpits_light.Route.RouteRecorder;
import ru.levabala.carsandpits_light.Services.SensorsService;
import ru.levabala.carsandpits_light.Other.Utils;

public class MainActivity extends AppCompatActivity {
    //some constants
    public static String LIST_OF_TRACKS_FILENAME = "listoftracks.config";
    public static String BUFFER_FILENAME = "buffer.dat";
    public static String LOCAL_SERVER_ADDRESS = "http://192.168.3.6:3000";
    public static String GLOBAL_SERVER_ADDRESS = "http://62.84.116.86:3000";
    public static String DEVICE_UNIQUE_ID = "unknown";
    public static File BUFFER_FILE;
    public static File LIST_OF_TRACKS_FILE;

    //my views
    private View fabView;
    private FloatingActionButton fab;
    private ListView listViewTracks;
    private TextView tvLocationsCount, tvSignalQuality, tvRouteSize;
    private TextView tvAxisX, tvAxisY, tvAxisZ;
    private ViewFlipper viewFlipper;
    private CheckBox checkBoxGlobalServer;

    //local vars
    private Context context;
    private List<String> listOfTracks = new ArrayList<>();
    private Map<String, File> nameToTrack = new HashMap<>();
    private ArrayAdapter<String> adapter;
    private RouteRecorder routeRecorder;
    private RouteSender routeSender;
    private Timer UIUpdateTimer;
    private Activity theActivity;
    private SharedPreferences applicationPrefs;
    private PowerManager.WakeLock mWakeLock;

    //ViewFlipper variables
    private int currentIndex = 0;
    final int TRACKS_MANAGES_INDEX = 0;
    final int TRACK_VIEWER_INDEX = 1;

    //list of permissions which we need to check
    private List<String> MY_PERMISSIONS = Arrays.asList(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WAKE_LOCK
    );;

    //-------------------------------- OnCreate functions --------------------------------
    //region OnCreate functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //activities.add(MainActivity.class);
        context = this;
        theActivity = (Activity)context;

        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_main);

        //let's set some static vars
        BUFFER_FILE = FileMethods.getInternalFile(BUFFER_FILENAME, context);
        LIST_OF_TRACKS_FILE = FileMethods.getInternalFile(LIST_OF_TRACKS_FILENAME, context);

        //wake lock
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //registering all views to local variables + some listeners
        registerViews();

        //let's show how many tracks have we already recorded
        updatelistViewOfTracks();

        //let's init support classes and variables
        routeRecorder = new RouteRecorder(context);
        routeSender = new RouteSender(context);
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

        viewFlipper = (ViewFlipper)findViewById(R.id.viewFlipper);
        tvLocationsCount = (TextView)findViewById(R.id.textViewLocationsCount);
        tvRouteSize = (TextView)findViewById(R.id.textViewRouteSize);
        tvAxisX = (TextView)findViewById(R.id.textViewAxisX);
        tvAxisY = (TextView)findViewById(R.id.textViewAxisY);
        tvAxisZ = (TextView)findViewById(R.id.textViewAxisZ);
        tvSignalQuality = (TextView)findViewById(R.id.textViewSignalQuality);
        checkBoxGlobalServer = (CheckBox)findViewById(R.id.checkBoxGlobalServer);

        fabView = findViewById(R.id.fab);
        fab = (FloatingActionButton) fabView;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTrackCreatePopup();
            }
        });

        listViewTracks = (ListView)findViewById(R.id.listViewTracks);
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
            Intent intent = new Intent(context, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        if (currentIndex != 0)
            switchActivityTo(0, context);
        else
            super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        switchActivityTo(currentIndex, context);
    }

    @Override
    public void onDestroy() {
        mWakeLock.release();
        super.onDestroy();
    }

    //endregion

    //-------------------------------- Main functions --------------------------------

    public void startTrack(){
        switchActivityTo(TRACK_VIEWER_INDEX, context);

        //change fab image to STOP
        Drawable d = ContextCompat.getDrawable(context, R.drawable.ic_action_stop_white);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_stop_white, context.getTheme()));
        else
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_stop_white));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveTrack();
            }
        });

        routeRecorder.startRecord();

        //timer to show some track params
        startUIUpdates();
        Utils.snackbar("Record was started", fabView);
    }

    private void saveTrack(){
        routeRecorder.stopRecord(new CallbackInterface() {
            @Override
            public void run() {
                switchActivityTo(TRACKS_MANAGES_INDEX, context);

                //change fab image to PLUS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_plus_white, context.getTheme()));
                else
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_plus_white));
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showTrackCreatePopup();
                    }
                });
                updatelistViewOfTracks();
                UIUpdateTimer.cancel();
            }
        });
    }

    //-------------------------------- Second-level functions --------------------------------
    //region Second-level functions

    private String[] getArrayOfFileNames(){
        String list = FileMethods.readFileToString(LIST_OF_TRACKS_FILE, context);
        return list.split("\\|");
    }

    private void startUIUpdates(){
        UIUpdateTimer = new Timer();
        UIUpdateTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                theActivity.runOnUiThread(
                        new Runnable(){
                            @Override
                            public void run(){
                                tvLocationsCount.setText(String.valueOf(SensorsService.totalRoute.size()));
                                tvSignalQuality.setText(String.valueOf(SensorsService.gpsAccuracy));

                                int fileSizeB = (int)BUFFER_FILE.length();
                                float fileSizeKB = (float)fileSizeB / 1024f;
                                float fileSizeMB = (float)fileSizeB / 1024f / 1024f;
                                String fileSizeStr = String.format("%.1f", fileSizeKB) + "KB";;
                                if (fileSizeMB > 10) fileSizeStr = String.format("%.1f", fileSizeMB) + "MB";

                                tvRouteSize.setText(fileSizeStr);

                                if (SensorsService.totalRoute.size() == 0) return;
                                RoutePoint lastRoutePoint = SensorsService.totalRoute.get(SensorsService.totalRoute.size()-1);
                                if (lastRoutePoint.accelerations.length == 0) return;
                                Point3dWithTime lastAccPoint = lastRoutePoint.accelerations[lastRoutePoint.accelerations.length-1];
                                tvAxisX.setText(String.valueOf(lastAccPoint.x));
                                tvAxisY.setText(String.valueOf(lastAccPoint.y));
                                tvAxisZ.setText(String.valueOf(lastAccPoint.z));
                            }
                        });
            }
        },0,500);
    }

    //endregion

    //-------------------------------- Interface methods --------------------------------
    //region Interface methods
    private void switchActivityTo(int index, Context context){
        if (currentIndex == index) return;

        //Utils.logText("From " + String.valueOf(currentIndex) + " to " + String.valueOf(index), context);

        if (index > currentIndex) {
            viewFlipper.setInAnimation(context, R.anim.in_from_right);
            viewFlipper.setOutAnimation(context, R.anim.out_to_left);
        }
        else {
            viewFlipper.setInAnimation(context, R.anim.in_from_left);
            viewFlipper.setOutAnimation(context, R.anim.out_to_right);
        }
        viewFlipper.setDisplayedChild(index);

        //Intent i = new Intent(context, activities.get(index));
        //context.startActivity(i);

        currentIndex = index;
    }

    public void showTrackCreatePopup(){
        // Inflate the popup_layout.xml
        /*LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.popup_create_track, viewGroup);*/

        View layout = getLayoutInflater().inflate(R.layout.popup_create_track, null);

        // Creating the PopupWindow
        final PopupWindow trackPopup = new PopupWindow(this);
        trackPopup.setContentView(layout);
        trackPopup.setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        trackPopup.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        trackPopup.setFocusable(true);
        trackPopup.setAnimationStyle(R.style.PopupAnimation);

        // Clear the default translucent background
        //trackPopup.setBackgroundDrawable(new BitmapDrawable());
        trackPopup.setBackgroundDrawable(new BitmapDrawable());

        // Displaying the popup at the specified location, + offsets.
        trackPopup.showAtLocation(layout, Gravity.CENTER, 0, 0);

        // Getting a reference to Close button, and close the popup when clicked.
        Button close = (Button) layout.findViewById(R.id.buttonCancel);
        Button startRecord = (Button) layout.findViewById(R.id.buttonStartRecord);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackPopup.dismiss();
            }
        });
        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackPopup.dismiss();
                startTrack();
            }
        });
    }

    public void updatelistViewOfTracks(){
        listOfTracks = new ArrayList<>();
        nameToTrack = new HashMap<>();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,listOfTracks);
        listViewTracks.setAdapter(adapter);

        String[] arr = FileMethods.readFileToString(LIST_OF_TRACKS_FILE, context).split("\\|");
        for (int i = arr.length-1; i >= 0; i--) {
            String s = arr[i];
            if (s.length() != 0) {
                listOfTracks.add(s);
                nameToTrack.put(s, FileMethods.getExternalFile(s));
            }
        }
    }

    public void showTracksOnMap(View view){
        String listoftracks = FileMethods.readFileToString(LIST_OF_TRACKS_FILE, context);
        String[] arr = listoftracks.split("\\|");
        String str = "";
        for (String s : arr)
            str += s + '\n';
        Utils.logText(str, context);
    }

    public void sendChosenTracks(View view){
        String serverUrl = GLOBAL_SERVER_ADDRESS;
        if (!checkBoxGlobalServer.isChecked())
            serverUrl = LOCAL_SERVER_ADDRESS;

        for (String filename : getCheckedFilesList())
            routeSender.sendRoute(FileMethods.readFile(FileMethods.getExternalFile(filename), context), serverUrl);
    }

    public void shareChosenTracks(View view){
        ArrayList<Uri> list = new ArrayList<Uri>();
        for (String filename : getCheckedFilesList()) {
            File file = nameToTrack.get(filename);
            //list.add(Uri.fromFile(file));
            Uri contentUri = FileProvider.getUriForFile(MainActivity.this,
                    "ru.levabala.carsandpits_light", file);
            list.add(contentUri);
            //list.add(FileProvider.getUriForFile(context, "ru.levabala.carsandpits_light", new File(context.getFilesDir().getAbsolutePath() + "/" + filename)));
        }

        Intent shareIntent = new Intent();//_MULTIPLE);

        //let's grant our share intent to work with the tracks
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> resInfoList = packageManager.queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            for (Uri uri : list)
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        shareIntent.setType("image/*");

        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setAction(Intent.ACTION_SEND);
        //shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        Uri uri = list.get(0);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(shareIntent, "Share tracks to.."));
    }

    public void requestNewLocalServerUrl(View view){
        final EditText input = new EditText(context);
        Utils.requestStringInDialog("Local server URL", "Edit local server address", LOCAL_SERVER_ADDRESS, input,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LOCAL_SERVER_ADDRESS = input.getText().toString();
                        applicationPrefs.edit().putString("LOCAL_SERVER_ADDRESS", LOCAL_SERVER_ADDRESS).apply();

                        Utils.logText("Local server address changed to " + LOCAL_SERVER_ADDRESS, context);
                    }
                }, theActivity, context);
    }

    private List<String> getCheckedFilesList(){
        SparseBooleanArray checked = listViewTracks.getCheckedItemPositions();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < listViewTracks.getAdapter().getCount(); i++) {
            if (checked.get(i))
                list.add((String)listViewTracks.getItemAtPosition(i));
        }
        return list;
    }

    public void deleteAllTracks(View view){
        Utils.requestAcceptDialog(
                "Deleting tracks", "Delete all your tracks?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int deleteFilesCount = routeRecorder.deleteAllTracks();
                        updatelistViewOfTracks();
                        Utils.logText(String.valueOf(deleteFilesCount) + " files deleted", context);
                        Utils.logText(FileMethods.readFileToString(LIST_OF_TRACKS_FILE, context), context);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.logText("Nice choice", context);
                    }
                }, context
        );
    }

    //endregion
}
