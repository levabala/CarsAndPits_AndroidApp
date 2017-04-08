package ru.levabala.carsandpits_light;

import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by levabala on 03.04.2017.
 */

public class Route {
    public List<RoutePoint> routePoints;
    public long startTime;

    public Route(List<RoutePoint> routePoints, long startTime){
        this.routePoints = routePoints;
        this.startTime = startTime;
    }

    public Route(){
        this.routePoints = new ArrayList<>();
        this.startTime = 0;
    }

    public String stringify(){
        String output = "";

        output = "Start time: " + String.valueOf(startTime) + '\n'
                + "Points count: " + String.valueOf(routePoints.size()) + '\n';

        int index = 0;
        for (RoutePoint rp : routePoints){
            output += "Point#" + String.valueOf(index)
                    + " Position: " + String.valueOf(rp.position.latitude) + ' ' + String.valueOf(rp.position.longitude) + '\n'
                    + "Accelerations count: " + String.valueOf(rp.accelerations.length) + '\n';

            int accIndex = 0;
            for (Point3dWithTime p : rp.accelerations){
                output += "\t#" + String.valueOf(accIndex);

                accIndex++;
            }

            index++;
        }


        return output;
    }

    public static Route createFromRawData(InputStream is, Context context){
        DataInputStream dis = new DataInputStream(is);
        Route r = new Route();
        try{
            //logText("Bytes available: " + String.valueOf(dis.available()), context);
            long time = dis.readLong();
            //logText("Start time: " + String.valueOf(time), context);
            r.startTime = time;

            RoutePoint rp;
            while (dis.available() > 0){
                //logText("Available: " + String.valueOf(dis.available()), context);
                float lat = dis.readFloat();
                float lng = dis.readFloat();

                int arraySize = dis.readInt();
                Point3dWithTime[] points = new Point3dWithTime[arraySize];
                for (int i = 0; i < arraySize; i++){
                    float x = dis.readFloat();
                    float y = dis.readFloat();
                    float z = dis.readFloat();
                    int deltaT = dis.readInt();

                    points[i] = new Point3dWithTime(x,y,z,deltaT);
                }
                rp = new RoutePoint(new LatLng(lat,lng), points);
                //logText("New point!", context);
                r.routePoints.add(rp);
            }
        }
        catch (Exception e){
            logText("ERROR!\n" + e.toString(), context);
            e.printStackTrace();
        }

        return r;
    }

    private static void logText(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
