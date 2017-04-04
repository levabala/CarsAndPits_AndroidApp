package ru.levabala.carsandpits_v2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by levabala on 03.04.2017.
 */

public class SensorsServiceController {
    Intent intent;
    ServiceConnection connection;
    Messenger messenger;
    Messenger replyMessenger;
    boolean isBound;
    Context context;

    public SensorsServiceController(Context context){
        this.context = context;
        this.connection = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) { isBound = false; }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                isBound = true;
                messenger = new Messenger(service);
            }
        };
        this.intent = new Intent(context, SensorsService.class);;
        context.bindService(intent, connection, context.BIND_AUTO_CREATE);
        context.startService(intent);

        class HandlerReplyMsg extends Handler {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                String recdMessage = msg.obj.toString(); //msg received from service
                logText(recdMessage);
            }
        }

        replyMessenger = new Messenger(new HandlerReplyMsg());
    }

    public void Pause(){
        sendMessageToService("Pause");
    }

    public void Resume(){
        sendMessageToService("Resume");
    }

    public void sendMessageToService(Object obj){
        if (isBound) {
            try {
                Message message = new Message();
                message.replyTo = replyMessenger;

                Bundle bundle = new Bundle();
                message.setData(bundle);

                message.obj = obj;

                messenger.send(message); //sending message to service
            } catch (RemoteException e) {
                //logText("Error");
                //e.printStackTrace();
            }
        }
    }
    private void logText(String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}
