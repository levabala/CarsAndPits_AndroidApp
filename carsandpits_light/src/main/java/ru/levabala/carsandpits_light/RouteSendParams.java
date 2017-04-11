package ru.levabala.carsandpits_light;

/**
 * Created by levabala on 11.04.2017.
 */

public class RouteSendParams {
    public byte[] data;
    public String serverIp;

    public RouteSendParams(byte[] data, String serverIp){
        this.data = data;
        this.serverIp = serverIp;
    }
}
