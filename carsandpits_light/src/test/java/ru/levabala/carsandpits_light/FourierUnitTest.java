package ru.levabala.carsandpits_light;

import android.util.Log;

import org.junit.Test;

import java.util.List;

import ru.levabala.carsandpits_light.Event.Event;
import ru.levabala.carsandpits_light.Other.Point3dWithTime;
import ru.levabala.carsandpits_light.Route.RouteAnalyzer;

import static org.junit.Assert.*;

/**
 * Created by levabala on 29.04.2017.
 */

public class FourierUnitTest {
    @Test
    public void fourierTransform_isCorrect() throws Exception {
        int length = 1024;
        Point3dWithTime[] input = new Point3dWithTime[length];

        for (int i = 0; i < length; i++)
            input[i] = new Point3dWithTime((float)Math.sin(i / 3), (float)Math.sin(i / 4), (float)Math.sin(i / 5), i);
        List<Event> result = RouteAnalyzer.analyze(input);
    }
}
