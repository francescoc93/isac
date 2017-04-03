package isac.gameoflife;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.text.format.Formatter;
import android.widget.Toast;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Francesco on 16/03/2017.
 */

public class Handler implements MessageListener {

    private GridView gridView;
    private MainActivity activity;
    private String ipAddress;
    private RabbitMQ rabbitMQ;
    private HashMap<String,ConnectedDeviceInfo> connectedDevices;
    private int value_address;
    private ReentrantLock lock;
    private float cellSize;
    private int myWidth,myHeight;

    public Handler(GridView gridView,final MainActivity activity, int myWidth,int myHeight){

        this.myHeight = myHeight;
        this.myWidth = myWidth;
        ipAddress=Utils.getIpAddress();
        System.out.println("Indirizzo IP " + ipAddress);
        value_address=Integer.parseInt(ipAddress.split("\\.")[3]);
        this.gridView=gridView;
        this.cellSize = gridView.getCellSize();
        this.activity=activity;
        this.rabbitMQ=new RabbitMQ(Utils.getAddress(),"[user]","[user]");
        connectedDevices=new HashMap<>();
        lock=new ReentrantLock();

        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, ipAddress, Toast.LENGTH_SHORT).show();
            }
        });
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

    public boolean sendBroadcastMessage(JSONObject message){
        if(rabbitMQ.isConnected()) {
            rabbitMQ.sendMessage("broadcast", message);
            return true;
        }

        return false;
    }

    @Override
    public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
        System.out.println("Messaggio ricevuto");
        try {

            //info è dell'altro device, infoSwipe sono i miei
            if(json.getString("type").equals("pinch")) { //messaggio broadcast
                PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS),PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),json.getInt(PinchInfo.X_COORDINATE),
                        json.getInt(PinchInfo.Y_COORDINATE), json.getLong(PinchInfo.TIMESTAMP),
                        json.getInt(PinchInfo.SCREEN_WIDTH), json.getInt(PinchInfo.SCREEN_HEIGHT));

                Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> infoSwipe=gridView.getInfoSwipe();
                Pair<Long,PinchInfo.Direction> timeStampDirection=infoSwipe.first;
                Pair<Integer,Integer> coordinate=infoSwipe.second;

                if(infoSwipe!=null && messageFromOther(info.getAddress())) {


                    lock.lock();

                    if(!connectedDevices.containsKey(info.getAddress())) {

                        lock.unlock();

                        if ((info.getTimestamp() > (timeStampDirection.first - /*20*/5000)) &&
                                (info.getTimestamp() < (timeStampDirection.first + /*20*/5000)) && info.oppositeDirection(timeStampDirection.second)) {
                            System.out.println("DEVICE PAIRED WITH " + info.getAddress());

                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity, "Schermo collegato", Toast.LENGTH_SHORT).show();
                                }
                            });


                            String nameSender = "", nameReceiver = "";
                            String ipAddressDevice = info.getAddress();

                            int value_address_device = Integer.parseInt(ipAddressDevice.split("\\.")[3]);


                            if (value_address > value_address_device) { //se sono il maggiore tra i due
                                nameSender = ipAddress + ipAddressDevice;
                                nameReceiver = ipAddressDevice + ipAddress;

                                System.out.println("Nome coda per inviare: " + nameSender);
                                System.out.println("Nome coda su cui ricevo: " + nameReceiver);

                                rabbitMQ.addQueue(nameSender);
                                rabbitMQ.addQueue(nameReceiver, this);

                            } else { //se sono il minore tra i due
                                nameReceiver = ipAddress + ipAddressDevice;
                                nameSender = ipAddressDevice + ipAddress;

                                System.out.println("Nome coda per inviare: " + nameReceiver);
                                System.out.println("Nome coda su cui ricevo: " + nameSender);

                                rabbitMQ.addQueue(nameReceiver);
                                rabbitMQ.addQueue(nameSender, this);
                            }

                            lock.lock();

                            ConnectedDeviceInfo connectionInfo = new ConnectedDeviceInfo(this.cellSize,
                                    info.getDirection(),timeStampDirection.second,
                                    info.getXcoordinate(), info.getYcoordinate(), info.getScreenWidth(), info.getScreenHeight(),this.myWidth,
                                    this.myHeight, coordinate.first, coordinate.second, nameSender, nameReceiver);

                            connectedDevices.put(ipAddressDevice, connectionInfo);
                            connectionInfo.calculateInfo();

                            lock.unlock();
                        }
                    }else{
                        lock.unlock();
                    }
                }
            }else if(json.getString("type").equals("close")){ //messaggio al singolo device

                ConnectedDeviceInfo deviceInfo=null;

                lock.lock();

                if (connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))) {
                    deviceInfo = connectedDevices.remove(json.getString(PinchInfo.ADDRESS));

                    if(connectedDevices.size()==0){
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, "Schermo scollegato", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                lock.unlock();

                if(deviceInfo!=null && rabbitMQ.isConnected()){
                    closeCommunication(deviceInfo.getNameQueueSender());
                    closeCommunication(deviceInfo.getNameQueueReceiver());
                }
            }else if(json.getString("type").equals("start")){ //messaggio broadcast
                if(messageFromOther(json.getString(PinchInfo.ADDRESS)) && isConnected()) {
                    gridView.start();
                }
            }else if(json.getString("type").equals("pause")){ //messaggio broadcast
                if(messageFromOther(json.getString(PinchInfo.ADDRESS)) && isConnected()) {
                    gridView.pause();
                }
            }else if(json.getString("type").equals("reset")){ //messaggio broadcast
                if(messageFromOther(json.getString(PinchInfo.ADDRESS)) && isConnected()) {
                    gridView.clear();
                }
            } else if (json.getString("type").equals("cells")){
                //TODO: PRENDERE INFORMAZIONI DAL MESSAGGIO E RICHIAMARE IL METODO SETPAIREDCELLS DELLA GRIDVIEW
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void closeDeviceCommunication() {
        if(rabbitMQ.isConnected()) {

            lock.lock();

            if (connectedDevices.size() != 0) {
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

                    System.out.println("Nome coda su cui invio che chiudo: "+device.getNameQueueSender());
                    System.out.println("Nome coda su cui ricevo che chiudo: "+device.getNameQueueReceiver());
                }

                connectedDevices.clear();

                /*if (connectedDevices.size() != 0) {
                    connectedDevices.clear();
                }*/
            }

            lock.unlock();
        }



    }

    public boolean isConnected(){
        lock.lock();

        boolean tmp=connectedDevices.size()==0?false:true;

        lock.unlock();

        return tmp;
    }

    private boolean messageFromOther (String ipAddressDevice){
        return !ipAddress.equals(ipAddressDevice);
    }
    private void closeCommunication(String name){
        rabbitMQ.close(name);
    }
}
