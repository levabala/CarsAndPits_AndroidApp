package ru.levabala.carsandpits;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by levabala on 30.03.2017.
 */
public class RoutePoint{
    public LatLng position;
    public List<Point3d> accelerations;
    public long time;
    public RoutePoint(LatLng position, List<Point3d> accelerations, long time){
        this.position = position;
        this.accelerations = accelerations;
        this.time = time;
    }
}

