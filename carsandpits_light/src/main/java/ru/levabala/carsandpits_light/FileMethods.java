package ru.levabala.carsandpits_light;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.ubjson.io.ByteArrayInputStream;
import org.ubjson.io.UBJInputStream;
import org.ubjson.io.UBJOutputStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
    public static File getInternalFile(String filename, Context context){
        File ourFolder = new File(context.getFilesDir().getAbsolutePath() + "/CarsAndPits");
        return new File(ourFolder, filename);
    }

    public static File getExternalFile(String filename){
        File ourFolder = new File(Environment.getExternalStorageDirectory().toString() + "/CarsAndPits");
        return new File(ourFolder, filename);
    }

    public static boolean checkAndCreateOurFolders(Context context){
        boolean result1 = true;
        boolean result2 = true;

        File folder1 = new File(context.getFilesDir().getAbsolutePath() + "/CarsAndPits");
        File folder2 = new File(Environment.getExternalStorageDirectory().toString() + "/CarsAndPits");
        if (!folder1.exists())
            result1 = folder1.mkdirs();
        if (!folder2.exists())
            result2 = folder2.mkdirs();

        return result1 && result2;
    }

    public static void appendToFile(byte[] data, File file, Context context){
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }
        catch (Exception e){
            Utils.logText("ERROR\n"  +e.toString(), context);
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(file, true);
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            Utils.logText("ERROR appendToFile\n" + e.toString(), context);
            e.printStackTrace();
        }
    }

    public static void clearFile(File file, Context context){
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            PrintWriter writer = new PrintWriter(outputStream);
            writer.print("");
            writer.close();
        }
        catch (FileNotFoundException e){
            Utils.logText("ERROR clearFile\n" + e.toString(), context);
            e.printStackTrace();
        }
    }

    public static void copyFromTo(File fromFile, File toFile, Context context){
        try {
            if (!toFile.exists()) {
                boolean res1 = toFile.getParentFile().mkdirs();
                boolean res2 = toFile.createNewFile();
            }
        }
        catch (Exception e){
            Utils.logText("ERROR\n"  +e.toString(), context);
        }
        try {
            FileInputStream in = new FileInputStream(fromFile);
            FileOutputStream out = new FileOutputStream(toFile);

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

    public static String readFileToString(File file, Context context) {
        String ret = "";
        try {
            FileInputStream inputStream = new FileInputStream(file);

            if (inputStream.available() > 0) {
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

    public static byte[] readFile(File file, Context context) {
        byte[] bytes = new byte[0];
        try {
            FileInputStream is = new FileInputStream(file);
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

    public static boolean isFileEmpty(File file, int bytesMin){
        int fileLength = (int)file.length();
        return fileLength < bytesMin;
    }
}
