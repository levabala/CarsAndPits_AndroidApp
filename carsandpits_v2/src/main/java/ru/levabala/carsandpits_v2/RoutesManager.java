package ru.levabala.carsandpits_v2;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.OutputStream;
import java.util.List;

/**
 * Created by levabala on 04.04.2017.
 */

public class RoutesManager {
    private static final String TRACKS_LIST_FILE_NAME = "routeslist.xml";
    private Context context;

    public RoutesManager(Context context){
        this.context = context;
    }

    public void saveRoute(String filename, List<Object> params){
        XmlSerializer xmlSerializer = Xml.newSerializer();
        try {
            OutputStream out = context.openFileOutput(TRACKS_LIST_FILE_NAME, Context.MODE_APPEND);
            xmlSerializer.setOutput(out, "utf-8");

            //Start Document
            xmlSerializer.startDocument("UTF-8", true);
            //xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            xmlSerializer.startTag("", "route");

            xmlSerializer.startTag("", "filename");
            xmlSerializer.text(filename);
            xmlSerializer.endTag("", "filename");

            xmlSerializer.endTag("", "route");
            xmlSerializer.endDocument();

            out.close();

            Utils.logText(FileMethods.readFileToString(TRACKS_LIST_FILE_NAME, context), context);
        }
        catch (Exception e){
            Utils.logText("ERROR " + e.toString(), context);
        }
    }
}
