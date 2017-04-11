package ru.levabala.carsandpits_light;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.telecom.Call;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by levabala on 07.04.2017.
 */

public class Utils {
    public static void logText(String text, Context context){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void snackbar(String text, View fabView){
        Snackbar.make(fabView, text, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    public static void requestStringInDialog(String title, String message, String defaultString, EditText input,
                                             DialogInterface.OnClickListener onClickListener, Activity activity, Context context){
        input.setText(defaultString);
        input.selectAll();
        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, onClickListener)
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
}
