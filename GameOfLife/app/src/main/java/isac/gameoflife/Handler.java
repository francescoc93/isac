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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
    private ReentrantLock lock,lockCounter,lockReady;
    private float cellSize;
    private int myWidth,myHeight,messageReceived,genCalculated;

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
        this.messageReceived = 0;
        genCalculated=0;
        lock=new ReentrantLock();
        lockCounter=new ReentrantLock();
        lockReady=new ReentrantLock();

       /* activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, ipAddress, Toast.LENGTH_SHORT).show();
            }
        });*/
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
        try {
            System.out.println("Messaggio ricevuto " + json.getString("type"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {

            //info è dell'altro device, infoSwipe sono i miei
            if(json.getString("type").equals("pinch")) { //messaggio broadcast
                PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS),PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),
                        json.getInt(PinchInfo.X_COORDINATE),
                        json.getInt(PinchInfo.Y_COORDINATE), json.getLong(PinchInfo.TIMESTAMP),
                        json.getInt(PinchInfo.SCREEN_WIDTH), json.getInt(PinchInfo.SCREEN_HEIGHT),
                        Float.parseFloat(json.getString(PinchInfo.XDPI)),Float.parseFloat(json.getString(PinchInfo.YDPI)));

                Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> infoSwipe=gridView.getInfoSwipe();


                if(infoSwipe!=null && messageFromOther(info.getAddress())) {

                    Pair<Long,PinchInfo.Direction> timeStampDirection=infoSwipe.first;
                    Pair<Integer,Integer> coordinate=infoSwipe.second;

                    lock.lock();

                    if(!connectedDevices.containsKey(info.getAddress())) {

                        lock.unlock();

                        if ((info.getTimestamp() > (timeStampDirection.first - /*20*/5000)) &&
                                (info.getTimestamp() < (timeStampDirection.first + /*20*/5000))/* && info.oppositeDirection(timeStampDirection.second)*/) {
                            System.out.println("DEVICE PAIRED WITH " + info.getAddress());

                            activity.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(activity, "Schermo collegato", Toast.LENGTH_SHORT).show();
                                }
                            });


                            String nameSender = "", nameReceiver = "";
                            String ipAddressDevice = info.getAddress();

/*                            int value_address_device = Integer.parseInt(ipAddressDevice.split("\\.")[3]);


                            System.out.println("IP MIO: " + value_address + " IP SUO: " +value_address_device);

                            if (value_address > value_address_device) { //se sono il maggiore tra i due
                                System.out.println("IL MIO INDIRIZZO E' MAGGIORE ");
                                nameSender = ipAddress + ipAddressDevice;
                                nameReceiver = ipAddressDevice + ipAddress;

                                System.out.println("Nome coda per inviare: " + nameSender);
                                System.out.println("Nome coda su cui ricevo: " + nameReceiver);

                                rabbitMQ.addQueue(nameSender);
                                rabbitMQ.addQueue(nameReceiver, this);

                            } else { //se sono il minore tra i due
                                System.out.println("IL MIO INDIRIZZO E' MINORE ");
                                nameReceiver =  ipAddressDevice+ipAddress;
                                nameSender = ipAddress + ipAddressDevice;

                                System.out.println("Nome coda per inviare: " + nameSender);
                                System.out.println("Nome coda su cui ricevo: " + nameReceiver);

                                rabbitMQ.addQueue(nameSender);
                                rabbitMQ.addQueue(nameReceiver, this);
                            }*/

                            nameSender = ipAddress + ipAddressDevice;
                            nameReceiver = ipAddressDevice + ipAddress;

                            System.out.println("Nome coda per inviare: " + nameSender);
                            System.out.println("Nome coda su cui ricevo: " + nameReceiver);

                            rabbitMQ.addQueue(nameSender);
                            rabbitMQ.addQueue(nameReceiver, this);

                            ConnectedDeviceInfo connectionInfo = new ConnectedDeviceInfo(this.cellSize,
                                    info.getDirection(),timeStampDirection.second,
                                    info.getXcoordinate(), info.getYcoordinate(), info.getScreenWidth(), info.getScreenHeight(),this.myWidth,
                                    this.myHeight, coordinate.first, coordinate.second, nameSender, nameReceiver,this.gridView,
                                    info.getXDpi(),info.getYDpi());

                            lock.lock();
                            System.out.println("STO METTENDO NELLA MAPPA L'IP DELL'ALTRO: " + ipAddressDevice);
                            connectedDevices.put(ipAddressDevice, connectionInfo);
                            lock.unlock();
                            connectionInfo.calculateInfo();
                        }
                    }else{
                        lock.unlock();
                    }
                }
            }/*else if(json.getString("type").equals("close")){ //messaggio al singolo device

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
            }*/else if(json.getString("type").equals("start")){ //messaggio broadcast
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

                System.out.println("HO RICEVUTO LE CELLE");

                lockCounter.lock();

                messageReceived++;

                System.out.println("MESSAGGIO RICEVUTO; MESSAGGI TOTALI: " + messageReceived);
                lockCounter.unlock();

                System.out.println("LISTA: " + json.getString("cellsList"));
               String[] cellsString = json.getString("cellsList").replace("\\[", "").replace("\\]", "").split(",");

                for (String s: cellsString){
                    System.out.println("LISTA DOPO IL REPLACE: " + s);
                }
                List<Boolean> cellsToSet = new ArrayList<>();
                for (String s : cellsString){
                   cellsToSet.add( Boolean.parseBoolean(s));
                }

                System.out.println("LISTA DOPO IL PARSE: " + cellsToSet.toString());
                int firstIndex = connectedDevices.get(json.getString(PinchInfo.ADDRESS)).getIndexFirstCell();
                int lastIndex = connectedDevices.get(json.getString(PinchInfo.ADDRESS)).getIndexLastCell();
                gridView.setPairedCells(firstIndex,lastIndex,cellsToSet,connectedDevices.get(json.getString(PinchInfo.ADDRESS)).getMyDirection());
            } else if(json.getString("type").equals("ready")){
                lockReady.lock();

                genCalculated++;

                lockReady.unlock();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean goOn(){

        boolean tmp;
        lockCounter.lock();
        lock.lock();

        if (messageReceived >= connectedDevices.size()){
            tmp= true;
            System.out.println("POSSO ANDARE AVANTI");
        } else {
            tmp= false;
        }

        lock.unlock();
        lockCounter.unlock();

        return tmp;
    }

    public void resetReceived(){

        lockCounter.lock();

        this.messageReceived = 0;

        lockCounter.unlock();
    }

    public boolean readyToSendCells(){

        boolean tmp;

        lockReady.lock();
        lock.lock();

        if (genCalculated == connectedDevices.size()){
            tmp= true;
        } else {
            tmp= false;
        }

        lock.unlock();
        lockReady.unlock();

        return tmp;
    }

    public void resetReceivedReady(){

        lockReady.lock();

        genCalculated = 0;

        lockReady.unlock();
    }

    public void sendCellsToOthers(){
        for (String s : connectedDevices.keySet()){
            JSONObject obj = new JSONObject();
            ConnectedDeviceInfo infoConn = connectedDevices.get(s);
            String queueSender = infoConn.getNameQueueSender();
            try {
                obj.put("type","cells");
                System.out.println("INDIRIZZO CHE METTO NEL JASON MIO: " + ipAddress);
                obj.put(PinchInfo.ADDRESS,ipAddress);
                System.out.println("LISTA PRIMA DI INVIARE " + infoConn.getCellsValues().toString());
                obj.put("cellsList",infoConn.getCellsValues());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println("CODA SU CUI STO INVIANDO: " + queueSender);
            rabbitMQ.sendMessage(queueSender, obj);
        }
    }

    //invio a tutti i device collegati un messaggio che indica
    //che sono pronto a inviare a loro le mie celle per la generazione successiva
    public void readyToContinue(){
        for (String s : connectedDevices.keySet()){
            JSONObject obj = new JSONObject();
            ConnectedDeviceInfo infoConn = connectedDevices.get(s);
            String queueSender = infoConn.getNameQueueSender();
            try {
                obj.put("type","ready");
                //obj.put(PinchInfo.ADDRESS,ipAddress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            rabbitMQ.sendMessage(queueSender, obj);
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
