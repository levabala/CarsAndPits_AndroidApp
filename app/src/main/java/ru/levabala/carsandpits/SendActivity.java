package ru.levabala.carsandpits;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SendActivity extends AppCompatActivity {
    private ListView listViewTracks;
    private List<String> listOfTracks = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private Switch serverType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        listViewTracks = (ListView)findViewById(R.id.listViewTracks);
        serverType = (Switch)findViewById(R.id.serverType);

        updatelistViewOfTracks();

        //Toast.makeText(this, "files: " + str, Toast.LENGTH_SHORT).show();
    }
    private void updatelistViewOfTracks(){
        listOfTracks = new ArrayList<>();

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,listOfTracks);
        listViewTracks.setAdapter(adapter);
        //setListAdapter(adapter);

        String[] arr = getArrayOfFileNames();
        String str = "";
        for (String s : arr) {
            str += s;
            if (s.length() != 0)
                listOfTracks.add(s);//.split("\\+")[0]);
        }
    }

    private String LOCAL_SERVER_ADDRESS = "http://192.168.3.6:3000";
    private String GLOBAL_SERVER_ADDRESS = "http://62.84.116.86:3000";
    private String SERVER_REQUEST_PATH = "/postData";
    private class HttpReq extends AsyncTask<String, Void, String> { //Асинхронная задача отправки маршрута
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String aString) {
            super.onPostExecute(aString);
            Toast.makeText(getApplicationContext(), "Server response: " + aString, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params) {
            String inf = "";
            String str = params[0];
            try {  //Ну а тут начинатся белеберда с http-запросом :)

                URL url;
                String urlstr = "";
                if (Boolean.valueOf(params[1]))
                    urlstr = GLOBAL_SERVER_ADDRESS;
                else urlstr = LOCAL_SERVER_ADDRESS;
                urlstr += SERVER_REQUEST_PATH;

                url = new URL(urlstr);  //Генерю URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Открываю соединение

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);  //Задаю параметры запроса
                connection.setReadTimeout(5000);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.write(str.getBytes());//map.toString().getBytes()); //Пишу трек в выходящий поток

                String response = String.valueOf(connection.getResponseMessage());
                connection.disconnect();
                outputStream.close(); //Закрываю соединение

                return response;  //Если всё ок, вывожу ответ сервера (должен быть Accepted)
            } catch (Exception e) {
                return e.toString();
            }
        }
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

    public void showTracksOnMap(View view){
        Intent mapIntent = new Intent(this, MapsActivity.class);

        List<String> trackNames = getCheckedTrackNames();
        List<String> tracksData = new ArrayList<>();

        for (String f : trackNames)
            tracksData.add(readFromFile(this,f));

        String[] trackNamesArr = new String[trackNames.size()];
        String[] tracksDataArr = new String[tracksData.size()];
        tracksData.toArray(tracksDataArr);
        trackNames.toArray(trackNamesArr);

        Bundle b = new Bundle();
        b.putStringArray("trackNames", trackNamesArr);
        //b.putStringArray("tracksData", tracksDataArr);
        mapIntent.putExtras(b);

        startActivity(mapIntent);
    }

    public void sendData(View view) {
        //SendDataTask senddatatask = new SendDataTask();
        //senddatatask.execute(map);
        List<String> list = getCheckedTrackNames();

        if (list.size() == 0){
            logText("Empty message");
            return;
        }

        for (String l : list){
            HttpReq httprequest = new HttpReq();
            httprequest.execute(readFromFile(this, l),String.valueOf(serverType.isChecked()));
        }
    }

    private List<String> getCheckedTrackNames(){
        SparseBooleanArray checked = listViewTracks.getCheckedItemPositions();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < listViewTracks.getAdapter().getCount(); i++) {
            if (checked.get(i))
                list.add((String)listViewTracks.getItemAtPosition(i));
        }
        return list;
    }

    private String[] getArrayOfFileNames(){
        String list = readFromFile(this,"listOfTracks.txt");
        list = list.replaceAll("\\|", "\n");
        String[] array = list.split("\n");
        return array;
    }

    private String prepareDataToSend(List<String> files){
        String str = "";
        for (String f : files) {
            str += readFromFile(this, f);
        }
        return str;
    }

    private void logText(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void Sended(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void ClearAllTracks(View view){
        new AlertDialog.Builder(this)
                .setTitle("Deleting")
                .setMessage("Delete all your tracks?")
                .setNegativeButton(android.R.string.cancel, null) // dismisses by default
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        deleteAllTracks();
                    }
                })
                .create()
                .show();
    }

    private void deleteAllTracks(){
        File dir = getFilesDir();
        String[] files = fileList();

        for (String s : files)
            new File(dir, s).delete();
        //boolean deleted = file.delete();
        updatelistViewOfTracks();

        logText("All tracks were deleted");
    }
}
