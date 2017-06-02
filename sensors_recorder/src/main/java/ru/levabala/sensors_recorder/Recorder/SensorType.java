package ru.levabala.sensors_recorder.Recorder;

import android.hardware.Sensor;

import java.util.HashMap;

/**
 * Created by levabala on 02.06.2017.
 */

public enum SensorType {
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

    public static SensorType getById(int id){
        switch (id){
            case Sensor.TYPE_ACCELEROMETER:
                return ACCELEROMETER;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return MAGNETIC_FIELD;
            case Sensor.TYPE_GYROSCOPE:
                return GYROSCOPE;
            case Sensor.TYPE_GRAVITY:
                return GRAVITY;
            default:
                return ACCELEROMETER;
        }
    }
}
