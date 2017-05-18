package ru.levabala.sensors_recorder.Recorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import ru.levabala.sensors_recorder.Activities.MainActivity;
import ru.levabala.sensors_recorder.Other.CallbackInterface;
import ru.levabala.sensors_recorder.Other.FileMethods;
import ru.levabala.sensors_recorder.Other.Utils;
import ru.levabala.sensors_recorder.Services.SensorsService;

import static ru.levabala.sensors_recorder.Recorder.SensorType.ACCELEROMETER;
import static ru.levabala.sensors_recorder.Recorder.SensorType.GRAVITY;
import static ru.levabala.sensors_recorder.Recorder.SensorType.GYROSCOPE;
import static ru.levabala.sensors_recorder.Recorder.SensorType.MAGNETIC_FIELD;

/**
 * Created by levabala on 12.05.2017.
 */

public class OldRecorder {
    public Map<Integer, List<DataTuple>> data;
    public ArrayList<Integer> sensorsList;

    private boolean serviceIsRunning = false;
    private Context context;
    private Intent serviceIntent;
    private Timer recordTimer;
    private Activity activity;
    private long startTime;
    public static String startTimeString;

    private boolean mBound = false;
    private SensorsService mService;

    public OldRecorder(ArrayList<Integer> sensors, Activity activity){
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.serviceIntent = new Intent(context, SensorsService.class);
        this.sensorsList = sensors;
        this.recordTimer = new Timer();

        context.bindService(this.serviceIntent, mConnection, Context.BIND_AUTO_CREATE);

        data = new HashMap<>();
        for (Integer type : sensors)
            data.put(type, new ArrayList<DataTuple>());

        checkPreviousBuffer();
    }

    public void onDestroy(){
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
    }

    public void start(boolean recordGPS){
        if (serviceIsRunning) return;

        Utils.logText("Start", context);

        serviceIsRunning = true;
        startTime = System.currentTimeMillis();
        startTimeString = new SimpleDateFormat("yyyy-MM-dd'T'HH'h'mm'm'ss").format(Calendar.getInstance().getTime());

        if (recordGPS)
            if (!((LocationManager) context.getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)){ //yohoho
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setMessage("Please, turn on GPS");
                dialog.setPositiveButton("Turn on", (DialogInterface paramDialogInterface, int paramInt) -> {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                });
                dialog.setNegativeButton("Cancel", (DialogInterface paramDialogInterface, int paramInt) -> {

                });
                dialog.show();
            }

        serviceIntent.putIntegerArrayListExtra("sensorsToRecord", sensorsList);
        serviceIntent.putExtra("recordGPS", recordGPS);
        context.startService(serviceIntent);

        FileMethods.appendToFile(
                ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                FileMethods.getExternalFile(startTimeString, GYROSCOPE.toString() + ".txt"),
                context
        );

        FileMethods.appendToFile(
                ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                FileMethods.getExternalFile(startTimeString, MAGNETIC_FIELD.toString() + ".txt"),
                context
        );

        FileMethods.appendToFile(
                ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                FileMethods.getExternalFile(startTimeString, ACCELEROMETER.toString() + ".txt"),
                context
        );

        FileMethods.appendToFile(
                ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                FileMethods.getExternalFile(startTimeString, GRAVITY.toString() + ".txt"),
                context
        );

        FileMethods.appendToFile(
                ("Start time: " + String.valueOf(SensorsService.startTime) + "\nDevice id: " + MainActivity.DEVICE_UNIQUE_ID + '\n').getBytes(),
                FileMethods.getExternalFile(startTimeString, "GPS.txt"),
                context
        );

        recordTimer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                if (!mBound) return;

                HashMap<Integer, List<DataTuple>> buffer = mService.getAndClearBuffer();

                if (buffer.containsKey(Sensor.TYPE_GYROSCOPE) && buffer.get(Sensor.TYPE_GYROSCOPE).size() > 0)
                    FileMethods.appendToFile(
                            DataTuple.serializeListToString(buffer.get(Sensor.TYPE_GYROSCOPE)).getBytes(),
                            FileMethods.getExternalFile(startTimeString, "GYROSCOPE.txt"),
                            context
                    );

                if (buffer.containsKey(Sensor.TYPE_MAGNETIC_FIELD) && buffer.get(Sensor.TYPE_MAGNETIC_FIELD).size() > 0)
                    FileMethods.appendToFile(
                            DataTuple.serializeListToString(buffer.get(Sensor.TYPE_MAGNETIC_FIELD)).getBytes(),
                            FileMethods.getExternalFile(startTimeString, "MAGNETIC_FIELD.txt"),
                            context
                    );

                if (buffer.containsKey(Sensor.TYPE_ACCELEROMETER) && buffer.get(Sensor.TYPE_ACCELEROMETER).size() > 0)
                    FileMethods.appendToFile(
                            DataTuple.serializeListToString(buffer.get(Sensor.TYPE_ACCELEROMETER)).getBytes(),
                            FileMethods.getExternalFile(startTimeString, "ACCELEROMETER.txt"),
                            context
                    );

                if (buffer.containsKey(Sensor.TYPE_GRAVITY) && buffer.get(Sensor.TYPE_GRAVITY).size() > 0)
                    FileMethods.appendToFile(
                            DataTuple.serializeListToString(buffer.get(Sensor.TYPE_GRAVITY)).getBytes(),
                            FileMethods.getExternalFile(startTimeString, "GRAVITY.txt"),
                            context
                    );

                if (buffer.containsKey(SensorsService.TYPE_GPS) && buffer.get(SensorsService.TYPE_GPS).size() > 0)
                    FileMethods.appendToFile(
                            DataTuple.serializeListToString(buffer.get(SensorsService.TYPE_GPS)).getBytes(),
                            FileMethods.getExternalFile(startTimeString, "GPS.txt"),
                            context
                    );
            }
        },0,3500);
    }

    public void pause(){

    }

    public void resume(){

    }

    public void stop(){
        Utils.logText("End", context);
        context.stopService(serviceIntent);
        mService.stopSelf();
        recordTimer.cancel();
        recordTimer = new Timer();

        serviceIsRunning = false;
    }

    private void checkPreviousBuffer(){
        if (!FileMethods.isFileEmpty(MainActivity.EXTERNAL_BUFFER_FILE, 3)){
            Utils.snackbarAlert(
                    "Here are some data from previous session to save",
                    activity.findViewById(MainActivity.FAB_ID),
                    (View v) -> {
                        saveBuffer(() -> {});
                    });
        }
    }

    private void saveBuffer(final CallbackInterface callback){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
        final String formattedDate = df.format(c.getTime());

        final EditText input = new EditText(context);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (input.getText().toString().equals(MainActivity.BUFFER_FILENAME))
                    Utils.logText("You mustn't save tracks as \"buffer.dat\"", context);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = input.getText().toString() + ".dat";
                File file = FileMethods.getExternalFile(filename);
                //checking for app-reserved files
                if (filename.equals(MainActivity.BUFFER_FILENAME)){
                    Utils.logText("Not saved. You can't use the names:\n'buffer.dat'\n'listoftracks.config'", context);
                    return;
                }
                saveRecordedData(file);

                Utils.logText("Saved as " + filename + "\nSize: "
                        + String.valueOf(file.length()) + "B", context);

                data = new HashMap<>();
                serviceIsRunning = false;
                callback.run();
            }
        };
        Utils.requestStringInDialog("Route saving", "File name:", formattedDate, input, onClickListener, (Activity)context, context);
    }

    private void saveRecordedData(File file){

    }

    //binder
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SensorsService.LocalBinder binder = (SensorsService.LocalBinder)service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
