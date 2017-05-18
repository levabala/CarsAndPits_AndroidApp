package ru.levabala.sensors_recorder.Recorder;

import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by levabala on 12.05.2017.
 */

public enum SensorType implements Parcelable{
    ACCELEROMETER("ACCELEROMETER", Sensor.TYPE_ACCELEROMETER),
    MAGNETIC_FIELD("MAGNETIC_FIELD", Sensor.TYPE_MAGNETIC_FIELD),
    GYROSCOPE("GYROSCOPE", Sensor.TYPE_GYROSCOPE),
    GRAVITY("GRAVITY", Sensor.TYPE_GRAVITY);

    private String string;
    private int type;
    SensorType(String string, int type){
        this.string = string;
        this.type = type;
    }

    public String toString(){
        return string;
    }

    public int getType(){
        return type;
    }
}
