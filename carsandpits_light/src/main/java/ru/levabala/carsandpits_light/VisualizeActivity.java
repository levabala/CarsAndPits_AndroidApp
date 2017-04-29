package ru.levabala.carsandpits_light;

import android.graphics.Color;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

import ru.levabala.carsandpits_light.Event.Event;
import ru.levabala.carsandpits_light.Other.Complex;
import ru.levabala.carsandpits_light.Other.Point3dWithTime;
import ru.levabala.carsandpits_light.Route.RouteAnalyzer;

public class VisualizeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualize);

        test();
    }

    private void test(){
        LineChart chart = (LineChart) findViewById(R.id.chart);
        List<Entry> entriesX = new ArrayList<Entry>();
        List<Entry> entriesY = new ArrayList<Entry>();
        List<Entry> entriesZ = new ArrayList<Entry>();

        int length = 32;
        Point3dWithTime[] input = new Point3dWithTime[length];

        float step = (float)Math.PI / length;
        for (int i = 0; i < length; i++)
            input[i] = new Point3dWithTime((float)Math.sin(step * i), (float)Math.sin(step * i * 2), (float)Math.sin(step * i * 3), 1);

        int time = 0;
        for (Point3dWithTime p : input) {
            entriesX.add(new Entry(time, p.x));
            entriesY.add(new Entry(time, p.y));
            entriesZ.add(new Entry(time, p.z));
            time += p.deltaTime;
        }

        LineDataSet dataSetX = new LineDataSet(entriesX, "X"); // add entries to dataset
        dataSetX.setColor(Color.RED);
        LineDataSet dataSetY = new LineDataSet(entriesY, "Y"); // add entries to dataset
        dataSetY.setColor(Color.GREEN);
        LineDataSet dataSetZ = new LineDataSet(entriesZ, "Z"); // add entries to dataset
        dataSetZ.setColor(Color.YELLOW);



        chart.setData(new LineData(dataSetX));//, dataSetY, dataSetZ));
        chart.invalidate();

        //List<Event> result = RouteAnalyzer.analyze(input);
        Complex[][][] arr = RouteAnalyzer.findPitsWithFourier(input);

        for (Complex[] a : arr[0]){

        }
    }
}
