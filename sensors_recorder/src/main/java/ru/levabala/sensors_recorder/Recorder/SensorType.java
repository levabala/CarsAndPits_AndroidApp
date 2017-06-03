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
    GRAVITY("GRAVITY", Sensor.TYPE_GRAVITY),
    AMBIENT_TEMPERATURE("AMBIENT_TEMPERATURE", Sensor.TYPE_AMBIENT_TEMPERATURE),
    LIGHT("LIGHT", Sensor.TYPE_LIGHT),
    LINEAR_ACCELERATION("LINEAR_ACCELERATION", Sensor.TYPE_LINEAR_ACCELERATION),
    ORIENTATION("ORIENTATION", Sensor.TYPE_ORIENTATION),
    PRESSURE("PRESSURE", Sensor.TYPE_PRESSURE),
    PROXIMITY("PROXIMITY", Sensor.TYPE_PROXIMITY),
    RELATIVE_HUMIDITY("RELATIVE_HUMIDITY", Sensor.TYPE_RELATIVE_HUMIDITY),
    ROTATION_VECTOR("ROTATION_VECTOR", Sensor.TYPE_ROTATION_VECTOR),
    TEMPERATURE("TEMPERATURE", Sensor.TYPE_TEMPERATURE),
    GAME_ROTATION_VECTOR("GAME_ROTATION_VECTOR", Sensor.TYPE_GAME_ROTATION_VECTOR),
    GEOMAGNETIC_ROTATION_VECTOR("GEOMAGNETIC_ROTATION_VECTOR", Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),
    HEART_BEAT("HEART_BEAT", Sensor.TYPE_HEART_BEAT),
    HEART_RATE("HEART_RATE", Sensor.TYPE_HEART_RATE),
    SIGNIFICANT_MOTION("SIGNIFICANT_MOTION", Sensor.TYPE_SIGNIFICANT_MOTION),
    STATIONARY_DETECT("STATIONARY_DETECT", Sensor.TYPE_STATIONARY_DETECT),
    STEP_COUNTER("STEP_COUNTER", Sensor.TYPE_STEP_COUNTER),
    STEP_DETECTOR("STEP_DETECTOR", Sensor.TYPE_STEP_DETECTOR),
    GPS("GPS", 999),
    UNKNOWN("UNKNOWN", -1);

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
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return AMBIENT_TEMPERATURE;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return LINEAR_ACCELERATION;
            case Sensor.TYPE_GRAVITY:
                return GRAVITY;
            case Sensor.TYPE_ORIENTATION:
                return ORIENTATION;
            case Sensor.TYPE_PRESSURE:
                return PRESSURE;
            case Sensor.TYPE_PROXIMITY:
                return PROXIMITY;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return RELATIVE_HUMIDITY;
            case Sensor.TYPE_ROTATION_VECTOR:
                return ROTATION_VECTOR;
            case Sensor.TYPE_TEMPERATURE:
                return TEMPERATURE;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                return GAME_ROTATION_VECTOR;
            case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                return GEOMAGNETIC_ROTATION_VECTOR;
            case Sensor.TYPE_HEART_BEAT:
                return HEART_BEAT;
            case Sensor.TYPE_HEART_RATE:
                return HEART_RATE;
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                return SIGNIFICANT_MOTION;
            case Sensor.TYPE_STATIONARY_DETECT:
                return STATIONARY_DETECT;
            case Sensor.TYPE_STEP_COUNTER:
                return STEP_COUNTER;
            case Sensor.TYPE_STEP_DETECTOR:
                return STEP_DETECTOR;
            case Sensor.TYPE_LIGHT:
                return LIGHT;
            default:
                return UNKNOWN;
            //TODO: also add uncalibrated sensors to fill up the list
        }
    }
}
