package ru.levabala.carsandpits_light;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.ubjson.io.ByteArrayInputStream;
import org.ubjson.io.UBJInputStream;
import org.ubjson.io.UBJOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Created by levabala on 08.04.2017.
 */

public class FileMethods {
        public static void parseUBJSON(byte[] bytes) {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        UBJInputStream in = new UBJInputStream(byteIn);

        try {

        }
        catch (Exception e){
            Log.e("MY_TAG", e.toString());
        }
    }

    public static void appendToFile(byte[] data, String filename, Context context){
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write(data);
            outputStream.close();
        } catch (Exception e) {
            Utils.logText("ERROR appendToFile\n" + e.toString(), context);
            e.printStackTrace();
        }
    }

    public static void clearFile(String filename, Context context){
        try {
            OutputStream outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            PrintWriter writer = new PrintWriter(outputStream);
            writer.print("");
            writer.close();
        }
        catch (FileNotFoundException e){
            Utils.logText("ERROR clearFile\n" + e.toString(), context);
            e.printStackTrace();
        }
    }

    public static void copyFromTo(String filenameFrom, String filenameTo, Context context){
        try {
            context.getFilesDir().mkdirs();

            InputStream in = context.openFileInput(filenameFrom);
            OutputStream out = context.openFileOutput(filenameTo, Context.MODE_PRIVATE);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        catch (Exception e){
            Utils.logText("ERROR copyFromTo\n" + e.toString(), context);
        }
    }

    public static String readFileToString(String filename, Context context) {

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
        } catch (Exception e){
            Utils.logText("ERROR readFileToString\n" + e.toString(), context);
            e.printStackTrace();
        }

        return ret;
    }

    public static byte[] readFile(String filename, Context context) {
        byte[] bytes = new byte[0];
        try {
            InputStream is = context.openFileInput(filename);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(b)) != -1) {
                bos.write(b, 0, bytesRead);
            }
            bytes = bos.toByteArray();
        } catch (Exception e){
            Utils.logText("ERROR readFile\n" + e.toString(), context);
            e.printStackTrace();
        }
        return bytes;
    }

    public static void saveBufferToFileAndClear(String targetFilename, Context context){
        copyFromTo(MainActivity.BUFFER_FILENAME, targetFilename, context);
        clearFile(MainActivity.BUFFER_FILENAME, context);
    }

    public static boolean isFileEmpty(String filename, int bytesMin, Context context){
        int fileLength = (int)fileSize(filename, context);
        return fileLength < bytesMin;
    }

    public static long fileSize(String filename, Context context){
        return new File(context.getFilesDir().getAbsolutePath() + "/" + filename).length();
    }
}
