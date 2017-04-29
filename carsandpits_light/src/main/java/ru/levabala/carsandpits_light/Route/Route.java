package ru.levabala.carsandpits_light.Route;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.ubjson.io.ByteArrayInputStream;
import org.ubjson.io.UBJInputStream;
import org.ubjson.io.UBJOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import ru.levabala.carsandpits_light.Event.Event;

/**
 * Created by levabala on 03.04.2017.
 */

public class Route {
    List<Event> events;

    public Route(){
        events = new ArrayList<>();
    }

    public byte[] serializeToUBJSON(){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        UBJOutputStream out = new UBJOutputStream(byteOut);

        try {
            out.writeArrayHeader(events.size());
            for (Event event : events)
                out.write(event.serializeToUBJSON());
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
