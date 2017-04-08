package ru.levabala.carsandpits_light;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private View fabView;
    private FloatingActionButton fab;

    private Context context;
    private ListView listViewTracks;
    private List<String> listOfTracks = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private RouterRecorder routerRecorder;
    private Timer UIUpdateTimer;
    private Activity theActivity;

    private TextView tvLocationsCount, tvSignalQuality, tvRouteSize;

    public static ViewFlipper viewFlipper;
    //public static List<Class> activities = new ArrayList<>();
    public static int currentIndex = 0;
    public static int TRACKS_MANAGES_INDEX = 0;
    public static int TRACK_VIEWER_INDEX = 1;

    public static void switchActivityTo(int index, Context context){
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //activities.add(MainActivity.class);
        context = this;
        theActivity = (Activity)context;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        updatelistViewOfTracks();

        routerRecorder = new RouterRecorder(context);

        UIUpdateTimer = new Timer();
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
                switchActivityTo(TRACK_VIEWER_INDEX, context);
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

                UIUpdateTimer = new Timer();
                UIUpdateTimer.scheduleAtFixedRate(new TimerTask(){
                    @Override
                    public void run(){
                        theActivity.runOnUiThread(
                                new Runnable(){
                                    @Override
                                    public void run(){
                                        tvLocationsCount.setText(String.valueOf(SensorsService.totalRoute.size()));
                                        tvRouteSize.setText(String.valueOf(FileMethods.fileSize("buffer.dat")));
                                        tvSignalQuality.setText(String.valueOf(SensorsService.gpsAccuracy));
                                    }
                                });
                    }
                },0,500);
                Utils.snackbar("Record was started", fabView);
            }
        });
    }

    public void saveTrack(){
        routerRecorder.stopRecord();

        switchActivityTo(0,context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_plus_white, context.getTheme()));
        } else {
            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_plus_white));
        }
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTrackCreatePopup();
            }
        });
    }

    private void updatelistViewOfTracks(){
        listOfTracks = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,listOfTracks);
        listViewTracks.setAdapter(adapter);
        //setListAdapter(adapter);

        //String[] arr = getArrayOfFileNames();
        String[] arr = new String[10];
        arr[0] = "Track1";
        arr[1] = "Track2";
        arr[2] = "Track3";
        arr[3] = "Track4";
        arr[4] = "Track5";
        arr[5] = "Track6";
        arr[6] = "Track7";
        arr[7] = "Track8";
        arr[8] = "Track9";
        arr[9] = "Track10";
        String str = "";
        for (String s : arr) {
            str += s;
            if (s.length() != 0)
                listOfTracks.add(s);//.split("\\+")[0]);
        }
    }
}
