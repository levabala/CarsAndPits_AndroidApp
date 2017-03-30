package ru.levabala.carsandpits;

import android.graphics.Point;

import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by levabala on 30.03.2017.
 */
public class RawRouteParser {
    public static List<RoutePoint> Parse(String raw){
        List<RoutePoint> output = new ArrayList<>();
        raw = raw.substring(0, raw.length() - 1); //removing last "|"

        String[] rawParts = raw.split("\\|");
        int c = 0;
        for (String line : rawParts){
            String[] lineParts = line.split("\\;");
            LatLng position = new LatLng(Double.parseDouble(lineParts[0]), Double.parseDouble(lineParts[1]));
            long time = Long.parseLong(lineParts[2]);

            //lineParts = Arrays.copyOf(lineParts, lineParts.length - 3);
            List<Point3d> accelerations = new ArrayList<>();
            for (int i = 3; i < lineParts.length; i+=3)
                if (lineParts[i] != "")
                    accelerations.add(new Point3d(
                            Float.parseFloat(lineParts[i]),
                            Float.parseFloat(lineParts[i + 1]),
                            Float.parseFloat(lineParts[i + 2])));
            RoutePoint rt = new RoutePoint(position,accelerations,time);
            output.add(rt);
        }

        return output;
    }
}