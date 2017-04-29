package ru.levabala.carsandpits_light.Event;

import org.ubjson.io.UBJOutputStream;

import java.io.ByteArrayOutputStream;

import ru.levabala.carsandpits_light.Other.Point3dWithTime;

/**
 * Created by levabala on 28.04.2017.
 */

public class HowPit extends HowObject{
    int duration;
    Point3dWithTime[] accelerations;

    public HowPit(int duration, Point3dWithTime[] accelerations){
        this.duration = duration;
        this.accelerations = accelerations;
    }

    public byte[] serializeToUBJSON(){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        UBJOutputStream out = new UBJOutputStream(byteOut);

        try {
            out.writeObjectHeader(2);
            out.writeString("duration");
            out.writeInt32(duration);
            out.writeString("accelerations");
            out.writeArrayHeader(accelerations.length);
            for (Point3dWithTime p : accelerations) {
                out.writeArrayHeader(4);
                out.writeFloat(p.x);
                out.writeFloat(p.y);
                out.writeFloat(p.z);
                out.writeInt32(p.deltaTime);
            }
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
