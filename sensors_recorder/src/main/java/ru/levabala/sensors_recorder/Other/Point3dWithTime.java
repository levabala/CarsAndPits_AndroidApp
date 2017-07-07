package ru.levabala.sensors_recorder.Other;

/**
 * Created by levabala on 02.04.2017.
 */

public class Point3dWithTime extends Point3d {
    public int deltaTime;
    public Point3dWithTime(float x, float y, float z, int deltaTime){
        super(x,y,z);
        this.deltaTime = deltaTime;
    }
}
