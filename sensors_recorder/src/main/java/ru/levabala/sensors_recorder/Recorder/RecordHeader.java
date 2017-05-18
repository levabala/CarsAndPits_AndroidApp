package ru.levabala.sensors_recorder.Recorder;

import ru.levabala.sensors_recorder.Activities.MainActivity;
import ru.levabala.sensors_recorder.Services.SensorsService;

/**
 * Created by levabala on 16.05.2017.
 */

public class RecordHeader {
    long startTime;
    String device_id;

    public RecordHeader(long startTime, String device_id){
        this.startTime = startTime;
        this.device_id = device_id;
    }

    public String toString(){
        return ("Start time: " + String.valueOf(startTime) + "\nDevice id: " + String.valueOf(device_id));
    }
}
