package ru.levabala.sensors_recorder.Other;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

/**
 * Created by levabala on 07.04.2017.
 */

public class Utils {
    public static void logText(String text, Context context){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void log(String text){
        Log.d("DEBUG", text);
    }

    public static void snackbarAlert(String text, View fabView, View.OnClickListener listener){
        Snackbar.make(fabView, text, Snackbar.LENGTH_LONG)
                .setAction("Action", listener).show();
    }

    public static void requestStringInDialog(String title, String message, String defaultString, EditText input,
                                             DialogInterface.OnClickListener onPositiveListener, Activity activity, Context context){
        input.setText(defaultString);
        input.selectAll();
        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, onPositiveListener)
                .create();
        alert.show();
        if(input.requestFocus())
            alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public static void requestAcceptDialog(String title, String message, DialogInterface.OnClickListener acceptCallback,
                                     DialogInterface.OnClickListener cancelCallback, Context context){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, cancelCallback) // dismisses by default
                .setPositiveButton(android.R.string.ok, acceptCallback)
                .create()
                .show();
    }

    public static ArrayList<Integer> stringSetToArrayListInteger(Set<String> set){
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (String s : set)
            arrayList.add(Integer.parseInt(s));

        return arrayList;
    }
}
