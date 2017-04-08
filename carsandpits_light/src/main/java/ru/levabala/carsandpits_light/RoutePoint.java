package ru.levabala.carsandpits_light;

import com.google.android.gms.maps.model.LatLng;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by levabala on 02.04.2017.
 */

public class RoutePoint{
    public LatLng position;
    //public List<Point3dWithTime> accelerationsList;
    public Point3dWithTime[] accelerations;
    /*public RoutePoint(LatLng position, List<Point3dWithTime> accelerations){
        this.position = position;
        this.accelerationsList = accelerations;
    }*/
    public RoutePoint(LatLng position, Point3dWithTime[] accelerations){
        this.position = position;
        this.accelerations = accelerations;
    }

    public static byte[] getBytes(RoutePoint rp){
        //format is:
        //latitude[float] longitude[float] list.size[int] Point#1[floatx3 + int] Point#2[floatx3 + int] + ...
        int arraySize = RoutePoint.getBytesLength(rp);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(arraySize);
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeFloat((float) rp.position.latitude);
            dos.writeFloat((float) rp.position.longitude);
            dos.writeInt(rp.accelerations.length);
            for (Point3dWithTime point : rp.accelerations){
                dos.writeFloat(point.x);
                dos.writeFloat(point.y);
                dos.writeFloat(point.z);
                dos.writeInt(point.deltaTime);
            }
        }
        catch (IOException e){
            //and how can it be?
        }

        return bos.toByteArray();
    }



    private static int getBytesLength(RoutePoint rp){
        return 32 / 8 + 32 / 8 + rp.accelerations.length * (32 * 3 / 8 + 32);
    }
}

