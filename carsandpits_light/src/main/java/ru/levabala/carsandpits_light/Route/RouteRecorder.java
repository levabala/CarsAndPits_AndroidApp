package ru.levabala.carsandpits_light.Route;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import ru.levabala.carsandpits_light.Other.CallbackInterface;
import ru.levabala.carsandpits_light.Other.FileMethods;
import ru.levabala.carsandpits_light.Activities.MainActivity;
import ru.levabala.carsandpits_light.Services.SensorsService;
import ru.levabala.carsandpits_light.Other.Utils;

/**
 * Created by levabala on 08.04.2017.
 */

public class RouteRecorder {
    public Route route;

    private Context context;
    private Intent intent;
    private boolean serviceIsRunning;

    public RouteRecorder(Context context){
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

    public void stopRecord(final CallbackInterface callback){
        route = new Route();//new ArrayList<>(SensorsService.totalRoute));

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
                if (input.getText().toString().equals(MainActivity.BUFFER_FILENAME) ||
                        input.getText().toString().equals(MainActivity.LIST_OF_TRACKS_FILENAME))
                    Utils.logText("You mustn't save tracks as \"listoftracks.config\" or \"buffer.dat\"", context);
                else if (s.toString().indexOf('|') != -1){
                    input.setText(s.toString().replace("|", ""));
                    Utils.logText("You mustn't use \"|\" in tracks name because it's system symbol", context);
                }
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
                if (filename.equals(MainActivity.BUFFER_FILENAME) || filename.equals(MainActivity.LIST_OF_TRACKS_FILENAME)){
                    Utils.logText("Not saved. You can't use the names:\n'buffer.dat'\n'listoftracks.config'", context);
                    return;
                }
                saveRecordedRoute(file);

                Utils.logText("Saved as " + filename + "\nSize: "
                        + String.valueOf(file.length()) + "B", context);

                SensorsService.totalRoute.clear();
                serviceIsRunning = false;
                callback.run();
            }
        };
        Utils.requestStringInDialog("Route saving", "File name:", formattedDate, input, onClickListener, (Activity)context, context);
    }

    public void saveRecordedRoute(File file){
        FileMethods.copyFromTo(MainActivity.BUFFER_FILE, file, context);
        FileMethods.clearFile(MainActivity.BUFFER_FILE, context);
        FileMethods.appendToFile((file.getName() + "|").getBytes(), MainActivity.LIST_OF_TRACKS_FILE, context);
    }

    public int deleteAllTracks(){
        if (MainActivity.LIST_OF_TRACKS_FILE.length() < 8) return 0;

        String[] files = FileMethods.readFileToString(MainActivity.LIST_OF_TRACKS_FILE, context).split("\\|");
        File dir = context.getFilesDir();

        for (String s : files)
            new File(dir, s).delete();

        FileMethods.clearFile(MainActivity.LIST_OF_TRACKS_FILE, context);
        return files.length;
    }
}
