package ru.levabala.carsandpits_v2;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by levabala on 04.04.2017.
 */

public class Utils {
    public static void logText(String text, Context context){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static String requestStringInDialog(String title, String message, String defaultString, Context context){
        final EditText input = new EditText(context);
        input.setText(defaultString);
        //input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String answer = input.getText().toString();

                    }
                })
                .create()
                .show();
        return "";
    }
}
