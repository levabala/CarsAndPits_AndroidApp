package ru.levabala.carsandpits_light;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by levabala on 08.04.2017.
 */

public class RouterRecorder {
    public Route route;

    private Context context;
    private Intent intent;
    private boolean serviceIsRunning;

    public RouterRecorder(Context context){
        this.context = context;
        this.intent = new Intent(context, SensorsService.class);;
    }

    public void startRecord(){
        if (serviceIsRunning) {
            Utils.logText("ERROR\nCan't start service - it's running", context);
            return;
        }

        serviceIsRunning = true;

        context.startService(intent);
    }

    public void stopRecord(Runnable afterStopAction){
        route = new Route(new ArrayList<>(SensorsService.totalRoute), SensorsService.startTime);

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
                if (input.getText().toString().equals("buffer.dat") || input.getText().toString().equals("listoftracks.config"))
                    Utils.logText("You mustn't save tracks as \"listoftracks.config\" or \"buffer.dat\"", context);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = input.getText().toString() + ".dat";
                //checking for app-reserved files
                if (filename.equals("buffer.dat") || filename.equals("listoftracks.config")){
                    Utils.logText("Not saved. You can't use the names:\n'buffer.dat'\n'listoftracks.config'", context);
                    return;
                }
                FileMethods.saveBufferToFileAndClear(filename, context);
                Utils.logText("Route saved", context);


            }
        };
        Utils.requestStringInDialog("Route saving", "File name:", formattedDate, input, onClickListener, (Activity)context, context);

        SensorsService.totalRoute.clear();
        serviceIsRunning = false;
    }
}
