package ru.levabala.carsandpits_light.Route;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.levabala.carsandpits_light.Event.Event;
import ru.levabala.carsandpits_light.Other.Complex;
import ru.levabala.carsandpits_light.Other.FFT;
import ru.levabala.carsandpits_light.Other.Point3d;
import ru.levabala.carsandpits_light.Other.Point3dWithTime;
import ru.levabala.carsandpits_light.Other.SimplePoint2d;

import static ru.levabala.carsandpits_light.Other.FFT.fft;

/**
 * Created by levabala on 28.04.2017.
 */

public class RouteAnalyzer {
    public static List<Event> analyze(Point3dWithTime[] accelerations){
        List<Event> events = new ArrayList<>();

        events.addAll(findPitsWithAverageCalc(accelerations));
        //events.addAll(findPitsWithFourier(accelerations));

        return events;
    }

    public static float sensibility = 2;
    private static List<Event> findPitsWithAverageCalc(Point3dWithTime[] accelerations){
        List<Event> events = new ArrayList<>();

        Point3dWithTime average = new Point3dWithTime(0,0,0,0);
        for (Point3dWithTime acc : accelerations){
            average.x += acc.x;
            average.y += acc.y;
            average.z += acc.z;
            average.deltaTime += acc.deltaTime;
        }
        average.x /= accelerations.length;
        average.y /= accelerations.length;
        average.z /= accelerations.length;
        average.deltaTime /= accelerations.length;

        float averageAreaX = 0;
        float averageAreaY = 0;
        float averageAreaZ = 0;
        int time = 0;
        for (int i = 0; i < accelerations.length-1; i++){
            Point3dWithTime current = accelerations[i];
            Point3dWithTime next = accelerations[i+1];
            averageAreaX += calcArea(new SimplePoint2d(time, current.x), new SimplePoint2d(time + next.deltaTime, next.x));
            averageAreaY += calcArea(new SimplePoint2d(time, current.y), new SimplePoint2d(time + next.deltaTime, next.y));
            averageAreaZ += calcArea(new SimplePoint2d(time, current.z), new SimplePoint2d(time + next.deltaTime, next.z));

            time += next.deltaTime;
        }

        averageAreaX /= accelerations.length-1;
        averageAreaY /= accelerations.length-1;
        averageAreaZ /= accelerations.length-1;

        return events;
    }

    public static int fourierBuffer = 8;
    public static Complex[][][] findPitsWithFourier(Point3dWithTime[] accelerations){
        List<Event> events = new ArrayList<>();

        int fouriersCount = accelerations.length / fourierBuffer * fourierBuffer;
        int time = 0;
        Complex[][][] out = new Complex[3][fouriersCount][];

        for (int i = 0; i < fouriersCount; i += fourierBuffer) {
            Complex[][] input = new Complex[3][fourierBuffer];
            for (int ii = 0; ii < fourierBuffer; ii++) {
                input[0][ii] = new Complex(time, accelerations[i + ii].x);
                input[1][ii] = new Complex(time, accelerations[i + ii].y);
                input[2][ii] = new Complex(time, accelerations[i + ii].z);
                time += accelerations[i + ii].deltaTime;
            }
            out[0][i / fourierBuffer] = fft(input[0]);
            out[1][i / fourierBuffer] = fft(input[1]);
            out[2][i / fourierBuffer] = fft(input[2]);
        }

        return out;
        //return events;
    }

    private static List<Event> findSharpManeuvers(Point3dWithTime[] accelerations){
        List<Event> events = new ArrayList<>();

        return events;
    }

    private static float calcArea(SimplePoint2d p1, SimplePoint2d p2){
        float area = 0;

        if (p2.x < p1.x){
            float tx = p1.x;
            float ty = p1.y;
            p1 = p2;
            p2 = new SimplePoint2d(tx,ty);
        }

        if (p1.y * p2.y < 0){
            SimplePoint2d zeroPoint = findCrossing(p1,p2, new SimplePoint2d(p1.x, 0), new SimplePoint2d(p2.x, 0));
            try{
                area += calcArea(p1,zeroPoint) + calcArea(zeroPoint, p2);
            }
            catch (Exception e){
                throw e;
            }
        }

        float a = p1.y;
        float b = p2.y;
        float h = Math.abs(p2.x - p1.x);

        area += (a + b) * h / 2;

        return area;
    }

    private static SimplePoint2d findCrossing(SimplePoint2d p1_1, SimplePoint2d p1_2, SimplePoint2d p2_1, SimplePoint2d p2_2){
        float k1 = (p1_2.y - p1_1.y) / (p1_2.x - p1_1.x);
        float k2 = (p2_2.y - p2_1.y) / (p2_2.x - p2_1.x);
        float b1 = p1_1.y - p1_1.x * k1;
        float b2 = p2_1.y - p2_1.x * k2;

        float x = (b2 - b1) / (k1 - k2);
        float y = k1 * x + b1;

        if (k1 - k2 != 0) x = (b2 - b1) / (k1 - k2);
        if (k1 - k2 != 0) y = k1 * x + b1;
        else y = b1;

        return new SimplePoint2d(x,y);
    }
}
