package ru.levabala.sensors_recorder.Recorder;

import android.provider.ContactsContract;

import org.ubjson.io.UBJInputStream;
import org.ubjson.io.UBJOutputStream;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Created by levabala on 06.05.2017.
 */

public class DataTuple{
    public float[] values;
    public int offsetFromStart;

    public DataTuple(float[] values, int offsetFromStart){
        this.values = values;
        this.offsetFromStart = offsetFromStart;
    }

    public byte[] serialize(){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        UBJOutputStream out = new UBJOutputStream(byteOut);

        try {
            out.writeInt32(offsetFromStart);
            out.writeArrayHeader(values.length);
            for (float val : values)
                out.writeFloat(val);
        }
        catch (Exception e){
            try {
                throw e;
            }
            catch (Exception ee){
                System.out.println("Completed");
            }
        }

        return byteOut.toByteArray();
    }

    public static byte[] serializeList(List<DataTuple> list){
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        UBJOutputStream out = new UBJOutputStream(byteOut);

        try {
            for (DataTuple tuplya : list){
                out.writeInt32(tuplya.offsetFromStart);
                out.writeArrayHeader(tuplya.values.length);
                for (float val : tuplya.values)
                    out.writeFloat(val);
            }
        }
        catch (Exception e){
            try {
                throw e;
            }
            catch (Exception ee){
                System.out.println("Completed");
            }
        }

        return byteOut.toByteArray();
    }

    public String toString(){
        String out = String.valueOf(offsetFromStart) + '\t';
        for (float val : values)
            out += String.format("%15f ", val) + '\t';
        return out;
    }

    public static String serializeListToString(List<DataTuple> list){
        String out = "";
        StringBuffer buffer = new StringBuffer();
        for (DataTuple tuplya : list)
            buffer.append(tuplya.toString() + '\n');

        return buffer.toString();
    }
}
