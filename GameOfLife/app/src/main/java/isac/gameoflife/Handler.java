package isac.gameoflife;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v4.util.Pair;
import android.text.format.Formatter;
import android.widget.Toast;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Francesco on 16/03/2017.
 */

public class Handler implements MessageListener {

    private GridView gridView;
    private Context context;
    private String ipAddress;
    private RabbitMQ rabbitMQ;
    private HashMap<String,ConnectedDeviceInfo> connectedDevices;
    private int value_address;
    private boolean portrait;
    private Object lock;

    public Handler(GridView gridView,Context context){

        ipAddress=Utils.getIpAddress();
        System.out.println("Indirizzo IP " + ipAddress);
        value_address=Integer.parseInt(ipAddress.split("\\.")[3]);
        this.gridView=gridView;
        this.context=context;
        this.rabbitMQ=new RabbitMQ(Utils.getAddress(),"[user]","[user]");
        connectedDevices=new HashMap<>();
        lock=new Object();
    }

    public boolean connectToServer(){
        return rabbitMQ.connect();
    }

    public void bindToBroadcastQueue(){
        if(rabbitMQ.isConnected()){
            rabbitMQ.addPublishExchange("broadcast", "fanout");
            rabbitMQ.addSubscribeQueue("broadcast", "fanout",this);
        }
    }

    public void sendBroadcastMessage(JSONObject message){
        if(rabbitMQ.isConnected()) {
            rabbitMQ.sendMessage("broadcast", message);
        }
    }

    public void setPortrait(boolean portrait){
        this.portrait = portrait;
    }

    @Override
    public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
        System.out.println("Messaggio ricevuto");
        try {

            if(json.getString("type").equals("pinch")) { //messaggio broadcast
                PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS),PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),json.getInt(PinchInfo.X_COORDINATE),
                        json.getInt(PinchInfo.Y_COORDINATE), json.getBoolean(PinchInfo.PORTRAIT), json.getLong(PinchInfo.TIMESTAMP),
                        json.getInt(PinchInfo.SCREEN_WIDTH), json.getInt(PinchInfo.SCREEN_HEIGHT));

                Pair<Long,PinchInfo.Direction> infoSwipe=gridView.getInfoSwipe();

                if(infoSwipe!=null) {
                    if (!ipAddress.equals(info.getAddress()) && (info.getTimestamp() > (infoSwipe.first - 20)) &&
                            (info.getTimestamp() < (infoSwipe.first + 20)) && info.oppositeDirection(infoSwipe.second)) {
                        System.out.println("DEVICE PAIRED WITH " + info.getAddress());
                        Toast.makeText(context, "Schermo collegato", Toast.LENGTH_SHORT).show();

                        //TODO: METTERE IN CONNECTED DEVICE INFO IL NUMERO DI CELLE
                        //TODO (2) : HANDLE MESSAGE

                        String nameSender = "", nameReceiver = "";
                        String ipAddressDevice = info.getAddress();

                        int value_address_device = Integer.parseInt(ipAddressDevice.split(".")[3]);

                        if (value_address > value_address_device) { //se sono il maggiore tra i due
                            nameSender = ipAddress + ipAddressDevice;
                            nameReceiver = ipAddressDevice + ipAddress;
                            rabbitMQ.addQueue(nameSender);
                            rabbitMQ.addQueue(nameReceiver, this);

                            //TODO: calcoli per x e y (pdf)
                            synchronized (lock) {
                                connectedDevices.put(ipAddressDevice, new ConnectedDeviceInfo(info.isPortrait(),
                                        info.getXcoordinate(), info.getYcoordinate(), nameSender, nameReceiver));
                            }

                        } else { //se sono il minore tra i due
                            nameReceiver = ipAddress + ipAddressDevice;
                            nameSender = ipAddressDevice + ipAddress;
                            rabbitMQ.addQueue(nameReceiver);
                            rabbitMQ.addQueue(nameSender, this);

                            //TODO: calcoli per x e y (pdf)
                            synchronized (lock) {
                                connectedDevices.put(ipAddressDevice, new ConnectedDeviceInfo(info.isPortrait(),
                                        info.getXcoordinate(), info.getYcoordinate(), nameReceiver, nameSender));
                            }

                        }
                    }
                }
            }else if(json.getString("type").equals("close")){ //messaggio al singolo device

                ConnectedDeviceInfo deviceInfo=null;

                synchronized (lock) {
                    if (connectedDevices.containsKey(PinchInfo.ADDRESS)) {
                        deviceInfo = connectedDevices.remove(json.getString(PinchInfo.ADDRESS));
                   }
                }

                if(deviceInfo!=null && rabbitMQ.isConnected()){
                    closeCommunication(deviceInfo.getNameQueueSender());
                    closeCommunication(deviceInfo.getNameQueueReceiver());
                }
            }else if(json.getString("type").equals("start")){ //messaggio broadcast

                if(!ipAddress.equals(json.getString(PinchInfo.ADDRESS))) {

                    boolean flag = false;

                    synchronized (lock) {
                        if (connectedDevices.size() != 0) {
                            flag = true;
                        }
                    }

                    if (flag) {
                        gridView.start();
                    }
                }

            }else if(json.getString("type").equals("pause")){ //messaggio broadcast

                if(!ipAddress.equals(json.getString(PinchInfo.ADDRESS))) {
                    boolean flag = false;

                    synchronized (lock) {
                        if (connectedDevices.size() != 0) {
                            flag = true;
                        }
                    }

                    if (flag) {
                        gridView.pause();
                    }
                }

            }else if(json.getString("type").equals("reset")){ //messaggio broadcast

                if(!ipAddress.equals(json.getString(PinchInfo.ADDRESS))) {
                    boolean flag = false;

                    synchronized (lock) {
                        if (connectedDevices.size() != 0) {
                            flag = true;
                        }
                    }

                    if (flag) {
                        gridView.clear();
                    }

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void closeDeviceCommunication() {
        if(rabbitMQ.isConnected()){

            synchronized (lock) {
                Collection<ConnectedDeviceInfo> devices = connectedDevices.values();

                JSONObject message = new JSONObject();

                try {
                    message.put("type", "close");
                    message.put(PinchInfo.ADDRESS, ipAddress);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (ConnectedDeviceInfo device : devices) {
                    rabbitMQ.sendMessage(device.getNameQueueSender(), message);
                    closeCommunication(device.getNameQueueSender());
                    closeCommunication(device.getNameQueueReceiver());
                }

                if (connectedDevices.size() != 0) {
                    connectedDevices.clear();
                }
            }
        }

    }

    public int getConnectedDevice(){
        synchronized (lock){
            return connectedDevices.size();
        }
    }

    private void closeCommunication(String name){
        rabbitMQ.close(name);
    }
}
