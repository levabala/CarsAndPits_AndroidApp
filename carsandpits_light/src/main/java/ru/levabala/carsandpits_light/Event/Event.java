package ru.levabala.carsandpits_light.Event;

import com.google.android.gms.maps.model.LatLng;

import org.ubjson.io.UBJOutputStream;

import java.io.ByteArrayOutputStream;

/**
 * Created by levabala on 28.04.2017.
 */

public abstract class Event {
    EventType what = EventType.EVENT;
    String who = "unknown";
    LatLng where = new LatLng(0, 0);
    long when = 0;
    HowObject how;

    public Event(String who, LatLng where, long when, HowObject how){
        this.who = who;
        this.where = where;
        this.when = when;
        this.how = how;
    }

    public byte[] serializeToUBJSON (){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        UBJOutputStream out = new UBJOutputStream(byteOut);

        try {
            out.writeArrayHeader(5);

            //what
            out.writeString(what.toString());
            //who
            out.writeString(who);
            //where
            out.writeArrayHeader(2);
            out.writeFloat((float)where.latitude);
            out.writeFloat((float)where.longitude);
            //when
            out.writeInt64(when);
            //how
            out.write(how.serializeToUBJSON());
        }
        catch (Exception e){
            try {
                throw e;
            }
            catch (Exception ee){
                System.out.println("Completed");
            }
        }

        return byteOut.toByteArray();
    }
}
