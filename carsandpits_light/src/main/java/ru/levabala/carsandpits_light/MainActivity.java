package ru.levabala.carsandpits_light;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.w3c.dom.Text;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    //some constants
    public static String LIST_OF_TRACKS_FILENAME = "listoftracks.config";
    public static String BUFFER_FILENAME = "buffer.dat";
    public static String LOCAL_SERVER_ADDRESS = "http://192.168.3.6:3000";
    public static String GLOBAL_SERVER_ADDRESS = "http://62.84.116.86:3000";

    //my views
    private View fabView;
    private FloatingActionButton fab;
    private ListView listViewTracks;
    private TextView tvLocationsCount, tvSignalQuality, tvRouteSize;
    private ViewFlipper viewFlipper;

    //local vars
    private Context context;
    private List<String> listOfTracks = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private RouterRecorder routerRecorder;
    private Timer UIUpdateTimer;
    private Activity theActivity;

    //ViewFlipper variables
    private int currentIndex = 0;
    final int TRACKS_MANAGES_INDEX = 0;
    final int TRACK_VIEWER_INDEX = 1;

    //list of permissions which we need to check
    private List<String> MY_PERMISSIONS = Arrays.asList(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    );;

    //-------------------------------- OnCreate functions --------------------------------
    //region OnCreate functions
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //activities.add(MainActivity.class);
        context = this;
        theActivity = (Activity)context;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //registering all views to local variables + some listeners
        registerViews();

        //let's show how many tracks have we already recorded
        updatelistViewOfTracks();

        //let's init support classes and variables
        routerRecorder = new RouterRecorder(context);
        UIUpdateTimer = new Timer();

        //let's request some permissions
        for (String permission : MY_PERMISSIONS)
            requestPermission(permission);
    }

    private void requestPermission(String permission){
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                //hmm...
                //what should be here?
                //text message to user why need we to take access to some smartphone features?
                Utils.logText("Please, give us the permission", context);
            } else
                ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
    }

    private void registerViews(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewFlipper = (ViewFlipper)findViewById(R.id.viewFlipper);
        tvLocationsCount = (TextView)findViewById(R.id.textViewLocationsCount);
        tvRouteSize = (TextView)findViewById(R.id.textViewRouteSize);
        tvSignalQuality = (TextView)findViewById(R.id.textViewSignalQuality);

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
            return true;
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

    //endregion

    //-------------------------------- Main functions --------------------------------

    private void startTrack(){
        switchActivityTo(TRACK_VIEWER_INDEX, context);

        //change fab image to STOP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_stop_white, context.getTheme()));
        } else {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_stop_white));
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveTrack();
            }
        });

        routerRecorder.startRecord();

        //timer to show some track params
        startUIUpdates();
        Utils.snackbar("Record was started", fabView);
    }

    private void saveTrack(){
        routerRecorder.stopRecord(new CallbackInterface() {
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

                Utils.logText(FileMethods.readFileToString(LIST_OF_TRACKS_FILENAME, context), context);
            }
        });
    }

    //-------------------------------- Second-level functions --------------------------------
    //region Second-level functions

    private String[] getArrayOfFileNames(){
        String list = FileMethods.readFileToString(LIST_OF_TRACKS_FILENAME, context);
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

                                int fileSizeB = (int)FileMethods.fileSize(BUFFER_FILENAME, context);
                                float fileSizeKB = (float)fileSizeB / 1024f;
                                float fileSizeMB = (float)fileSizeB / 1024f / 1024f;
                                String fileSizeStr = String.format("%.1f", fileSizeKB) + "KB";;
                                if (fileSizeMB > 10) fileSizeStr = String.format("%.1f", fileSizeMB) + "MB";

                                tvRouteSize.setText(fileSizeStr);
                            }
                        });
            }
        },0,500);
    }

    //endregion

    //-------------------------------- Interface methods --------------------------------
    //region Interface methods
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

    public void updatelistViewOfTracks(){
        listOfTracks = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,listOfTracks);
        listViewTracks.setAdapter(adapter);

        String[] arr = FileMethods.readFileToString(LIST_OF_TRACKS_FILENAME, context).split("\\|");
        for (String s : arr)
            if (s.length() != 0)
                listOfTracks.add(s);
    }

    public void showTracksOnMap(View view){
        String listoftracks = FileMethods.readFileToString(LIST_OF_TRACKS_FILENAME, context);
        String[] arr = listoftracks.split("\\|");
        String str = "";
        for (String s : arr)
            str += s + '\n';
        Utils.logText(str, context);
    }

    public void sendChosenTracks(View view){

    }

    public void deleteAllTracks(View view){
        Utils.requestAcceptDialog(
                "Deleting tracks", "Delete all your tracks?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int deleteFilesCount = routerRecorder.deleteAllTracks();
                        updatelistViewOfTracks();
                        Utils.logText(String.valueOf(deleteFilesCount) + " files deleted", context);
                        Utils.logText(FileMethods.readFileToString(LIST_OF_TRACKS_FILENAME, context), context);
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
