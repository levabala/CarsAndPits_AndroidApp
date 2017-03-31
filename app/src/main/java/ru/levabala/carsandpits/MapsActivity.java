package ru.levabala.carsandpits;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.internal.zzb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        Bundle b = getIntent().getExtras();
        String[] trackNames = b.getStringArray("trackNames");
        //String[] tracksData = b.getStringArray("tracksData");

        List<RoutePoint> route = RawRouteParser.Parse(readFromFile(this,trackNames[0]));
        LatLng startPosition = route.get(0).position;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 16));
        List<CircleOptions> circles = new ArrayList<>();

        PolylineOptions wayPath = new PolylineOptions()
                .width(4)
                .color(Color.RED)
                .add(route.get(0).position);

        int routeSize = route.size();
        for (int i = 0; i < routeSize-1; i++) {
            RoutePoint current = route.get(i);
            RoutePoint next = route.get(i+1);

            wayPath.add(next.position);

            double distance = measure(current.position, next.position);

            for (int ii = 0; ii < next.accelerations.size(); ii++){
                CircleOptions options = createCircle(current.position, 10, Color.GREEN);
                circles.add(options);
                mMap.addCircle(options);
            }
        }
        mMap.addPolyline(wayPath);

        logText(trackNames[0] + " parsed\n" + "length: " + String.valueOf(route.size()));
        logText("Circles count: " + String.valueOf(circles.size()));
    }

    private double measure(LatLng p1, LatLng p2){  // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = p2.latitude * Math.PI / 180 - p1.latitude * Math.PI / 180;
        double dLon = p2.longitude * Math.PI / 180 - p1.longitude * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(p1.latitude * Math.PI / 180) * Math.cos(p2.latitude * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
    }

    private CircleOptions createCircle(LatLng position, float radius, int color){
        return new CircleOptions()
                .center(position)
                .fillColor(Color.argb(70,0,255,0))
                .strokeColor(Color.WHITE)
                .strokeWidth(1f)
                .radius(radius);
    }

    private void logText(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
    private void logText(String[] arr) {
        String str = "";
        for (String s : arr)
            str += s + '\n';
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private String readFromFile(Context context, String filename) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(filename);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}
