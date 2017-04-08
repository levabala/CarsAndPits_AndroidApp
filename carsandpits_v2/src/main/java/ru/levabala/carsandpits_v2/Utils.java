package ru.levabala.carsandpits_v2;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by levabala on 04.04.2017.
 */

public class Utils {
    public static void logText(String text, Context context){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
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
}
