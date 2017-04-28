package ru.levabala.carsandpits_light;

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

/**
 * Created by levabala on 03.04.2017.
 */

public class Route {
    public List<RoutePoint> routePoints;
    public long startTime;
    public String owner = "unknown";

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

    public static Route parseFromUBJSON(byte[] bytes) {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        UBJInputStream in = new UBJInputStream(byteIn);

        Route route = new Route();

        try {
            //total object
            in.readObjectLength();

            //header
            in.readString();
            in.readObjectLength();
            in.readString(); //startime
            long startTime = in.readInt64();
            in.readString(); //id
            String id = in.readString();

            route.startTime = startTime;
            route.owner = id;

            //route
            in.readString(); //header
            int routePointsCount = in.readArrayLength();
            for (int i = 0; i < routePointsCount; i++){
                in.readArrayLength(); //3 here
                float latitude = in.readFloat();
                float longitude = in.readFloat();

                int accelerationsCount = in.readArrayLength();
                Point3dWithTime[] accelerations = new Point3dWithTime[accelerationsCount];
                for (int i2 = 0; i2 < accelerationsCount; i2++){
                    in.readArrayLength(); //4 here
                    float x = in.readFloat();
                    float y = in.readFloat();
                    float z = in.readFloat();
                    int deltaTime = in.readInt32();

                    accelerations[i2] = new Point3dWithTime(x,y,z,deltaTime);
                }

                route.routePoints.add(new RoutePoint(new LatLng(latitude,longitude), accelerations));
            }
            in.readEnd(); //happy end
        }
        catch (Exception e){
            Log.e("MY_TAG", e.toString());
        }

        return route;
    }

    public byte[] serializeToUBJSON(Route route, Context context){
        //region route format
        /*
        {
            header: {
                startTime: recording start time(int64),
                id: random generated 64bit-hash(string)
            }
            route: [
                //route point format
                [
                    latitude(float32),
                    longitude(float32),

                    //accelerations array
                    [
                        //acceleration point
                        [
                            X(float32),
                            Y(float32),
                            Z(float32),
                            deltaTime(int32)
                        ],
                        + ...
                    ]
                ],
                + ...
            ]
        }
        */
        //endregion
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        UBJOutputStream out = new UBJOutputStream(byteOut);

        try {
            //total object
            out.writeObjectHeader(2);

            //header
            out.writeString("header");
            out.writeObjectHeader(2);
                out.writeString("startTime");
                out.writeInt64(route.startTime);
                out.writeString("id");
                out.writeString(MainActivity.DEVICE_UNIQUE_ID);

            //route
            out.writeString("header");
            out.writeArrayHeader(route.routePoints.size());
            for (RoutePoint routePoint : route.routePoints){
                out.writeArrayHeader(3);
                out.writeFloat((float)routePoint.position.latitude);
                out.writeFloat((float)routePoint.position.longitude);

                out.writeArrayHeader(routePoint.accelerations.length);
                for (Point3dWithTime accPoint : routePoint.accelerations){
                    out.writeArrayHeader(4);
                    out.writeFloat(accPoint.x);
                    out.writeFloat(accPoint.y);
                    out.writeFloat(accPoint.z);
                    out.writeInt32(accPoint.deltaTime);
                }
            }
            out.writeEnd();
        }
        catch (Exception e){
            Log.e("MY_TAG", e.toString());
        }

        return byteOut.toByteArray();
    }

    private static void logText(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
