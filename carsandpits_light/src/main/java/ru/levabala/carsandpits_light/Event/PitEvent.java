package ru.levabala.carsandpits_light.Event;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

import ru.levabala.carsandpits_light.Event.Event;

/**
 * Created by levabala on 28.04.2017.
 */

public class PitEvent extends Event {
    public PitEvent(String who, LatLng where, long when, HowPit how){
        super(who,where,when,how);

        this.what = EventType.PIT_EVENT;
    }
}
