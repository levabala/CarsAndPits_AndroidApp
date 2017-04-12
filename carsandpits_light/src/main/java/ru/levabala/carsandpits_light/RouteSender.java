package ru.levabala.carsandpits_light;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by levabala on 11.04.2017.
 */

public class RouteSender {
    Context context;
    public RouteSender(Context context){
        this.context = context;
    }

    public void sendRoute(byte[] route, String serverUrl){
        new SendHttpReq().execute(new RouteSendParams(route, serverUrl));
    }

    //private String LOCAL_SERVER_ADDRESS = "http://192.168.3.6:3000";
    //private String GLOBAL_SERVER_ADDRESS = "http://62.84.116.86:3000";
    private String SERVER_REQUEST_PATH = "/postData";
    private class SendHttpReq extends AsyncTask<RouteSendParams, Void, String> { //Асинхронная задача отправки маршрута
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String aString) {
            super.onPostExecute(aString);
            Utils.logText("Server response: " + aString, context);
        }

        @Override
        protected String doInBackground(RouteSendParams... params) {
            String inf = "";
            RouteSendParams routeSendParams = params[0];
            byte[] route = routeSendParams.data;
            try {  //Ну а тут начинатся белеберда с http-запросом :)
                URL url = new URL(routeSendParams.serverIp + SERVER_REQUEST_PATH);  //Генерю URL
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Открываю соединение

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);  //Задаю параметры запроса
                connection.setReadTimeout(5000);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.write(route);//map.toString().getBytes()); //Пишу трек в выходящий поток

                String response = String.valueOf(connection.getResponseMessage());
                connection.disconnect();
                outputStream.close(); //Закрываю соединение

                return response;  //Если всё ок, вывожу ответ сервера (должен быть Accepted)
            } catch (Exception e) {
                return e.toString();
            }
        }
    }
}
