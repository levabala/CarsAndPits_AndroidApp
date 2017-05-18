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
 * Created by levabala on 05.05.2017.
 */

public class Recorder {
    public ArrayList<SensorType> sensorsToRecord;

    private boolean serviceIsRunning = false;
    private Context context;
    private Intent serviceIntent;
    private Timer recordTimer;
    private Activity activity;

    private boolean mBound = false;
    private SensorsService mService;

    public Recorder(ArrayList<SensorType> sensors, Activity activity){
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.serviceIntent = new Intent(context, SensorsService.class);
        this.sensorsToRecord = sensors;
        this.recordTimer = new Timer();

        context.bindService(this.serviceIntent, mConnection, Context.BIND_AUTO_CREATE);;
    }

    public void start(boolean recordGps){
        if (serviceIsRunning) return;
        serviceIsRunning = true;
        
        Utils.logText("Start", context);
    }

    public void stop(){

    }

    public void onDestroy(){
        if (mBound) {
            context.unbindService(mConnection);
            mBound = false;
        }
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
