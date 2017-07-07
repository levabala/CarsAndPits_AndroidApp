package ru.levabala.sensors_recorder.Other;

import android.app.Application;
import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.ToggleButton;

import ru.levabala.sensors_recorder.Activities.MainActivity;

/**
 * Created by levabala on 08.06.2017.
 */

public class ToggleButtonClickSafe extends ToggleButton {
    CallbackInterface callback;
    Context context;

    public ToggleButtonClickSafe(Context context){
        super(context);
        this.context = context;
    }

    public ToggleButtonClickSafe(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context = context;
    }

    public ToggleButtonClickSafe(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public void setCallback(CallbackInterface callback){
        this.callback = callback;
    }

    @Override
    public boolean performClick() {
        //super.performClick();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP){
            super.performClick();
            callback.run();
        }
        return super.onTouchEvent(event);
    }
}
