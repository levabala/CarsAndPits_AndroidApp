package ru.levabala.carsandpits_v2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TrackActivity extends AppCompatActivity {
    public static RoutesManager routesManager;

    Messenger messenger;
    Messenger replyMessenger;
    boolean isBound;
    Handler handler = new Handler();
    SensorsServiceController serviceController;

    private TextView tvRouteLength, tvGpsAccuracy, tvRealRouteLength;
    private int messagesCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        routesManager = new RoutesManager(this);

        //let's request some permissions
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        0);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        tvRouteLength = (TextView)findViewById(R.id.textViewRouteLength);
        tvGpsAccuracy = (TextView)findViewById(R.id.textViewGpsAccuracy);
        tvRealRouteLength = (TextView)findViewById(R.id.textViewRealCount);

        initSensorsService();
    }

    private void initSensorsService(){
        //final List<RoutePoint> totalR = SensorsService.totalRoute;
        serviceController = new SensorsServiceController(this);
        handler.postDelayed(updateData,500);

    }


    private Runnable updateData = new Runnable(){
        public void run(){
            tvRouteLength.setText(String.valueOf(SensorsService.totalRoute.size()));
            tvGpsAccuracy.setText(String.valueOf(SensorsService.gpsAccuracy));
            handler.postDelayed(updateData,500);
        }
    };

    private void logText(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }



    public void resumeService(View view){
        serviceController.Resume();
        //sendMessageToService("Resume");
    }
    public void pauseService(View view){
        serviceController.Pause();
        //sendMessageToService("Pause");
    }

    public void readBuffer(View view){
        try {
            InputStream is = this.openFileInput("buffer.dat");
            Route r = Route.createFromRawData(is, this);
            //Toast.makeText(this, r.stringify(), Toast.LENGTH_LONG).show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void showRoutesFile(View view){

    }

    public void saveRoute(View view){
        if (!FileMethods.isFileEmpty("buffer.dat", 64 / 8)){ //long in bytes
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
            String formattedDate = df.format(c.getTime());

            FileMethods.saveBufferToFileAndClear(formattedDate + ".dat", this);
        }
        else {
            //logText("Clear");
            FileMethods.clearFile("buffer.dat", this);
        }
    }
}
