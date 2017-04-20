package isac.gameoflife;

import android.support.v4.util.Pair;
import android.widget.Toast;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Francesco on 16/03/2017.
 */

public class Handler implements MessageListener {

    private GridView gridView;
    private MainActivity activity;
    private String ipAddress;
    private RabbitMQ rabbitMQ;
    private HashMap<String,ConnectedDeviceInfo> connectedDevices;
    private ReentrantLock lock,lockStop;
    private float cellSize;
    private float myWidth,myHeight;
    private boolean stop,reset;

    public Handler(GridView gridView,final MainActivity activity, float myWidth,float myHeight){

        this.myHeight = myHeight;
        this.myWidth = myWidth;
        ipAddress=Utils.getIpAddress();
        System.out.println("Indirizzo IP " + ipAddress);
        this.gridView=gridView;
        this.cellSize = gridView.getCellSize();
        this.activity=activity;
        this.rabbitMQ=new RabbitMQ(Utils.getAddress(),"[user]","[user]");
        connectedDevices=new HashMap<>();
        lock=new ReentrantLock();
        lockStop=new ReentrantLock();
        stop=false;
        reset=false;
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
                        Float.parseFloat(json.getString(PinchInfo.SCREEN_WIDTH)), Float.parseFloat(json.getString(PinchInfo.SCREEN_HEIGHT)),
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

                            nameSender = ipAddress + ipAddressDevice;
                            nameReceiver = ipAddressDevice + ipAddress;

                            System.out.println("Nome coda per inviare: " + nameSender);
                            System.out.println("Nome coda su cui ricevo: " + nameReceiver);

                            rabbitMQ.addQueue(nameSender);
                            rabbitMQ.addQueue(nameReceiver, this);

                            System.out.println("ALTEZZA: " +this.myHeight + "LARGHEZZA: " + this.myWidth);
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
            }else if(json.getString("type").equals("start")){

                //if(isConnected()){
                boolean flag=false;
                lockStop.lock();

                if(stop) {
                    stop = false;
                    reset=false;
                    flag=true;

                }

                lockStop.unlock();

                if(flag && isConnected()) {
                    gridView.start();

                    JSONObject message=new JSONObject();

                    try {
                        message.put("type","start");
                        message.put(PinchInfo.ADDRESS,ipAddress);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sendCommand(message,json.getString(PinchInfo.ADDRESS));
                }
                //}
            }else if(json.getString("type").equals("pause")){
              //  if(isConnected()) {

                lockStop.lock();

                boolean flag=false;

                if(!stop) {
                    stop = true;
                    flag=true;
                }

                lockStop.unlock();

                if(flag && isConnected()){

                    JSONObject message=new JSONObject();
                    try {
                        message.put("type","pause");
                        message.put(PinchInfo.ADDRESS,ipAddress);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sendCommand(message,json.getString(PinchInfo.ADDRESS));
                }


                  //  gridView.pause();
              //  }
            }else if(json.getString("type").equals("reset")){
               // if(isConnected()) {

                lockStop.lock();

                if(!stop) {
                    stop = true;
                }

                boolean flag=false;

                if(!reset){
                  reset=true;
                  flag=true;
                }

                lockStop.unlock();


                if(flag && isConnected()) {
                    JSONObject message=new JSONObject();

                    try {
                        message.put("type","reset");
                        message.put(PinchInfo.ADDRESS,ipAddress);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    sendCommand(message,json.getString(PinchInfo.ADDRESS));
                    gridView.distributedClear();
                }

                   // gridView.clear();
              //  }
            } else if (json.getString("type").equals("cells")){

                System.out.println("HO RICEVUTO LE CELLE");

                lock.lock();

                if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))){
                    ConnectedDeviceInfo device=connectedDevices.get(json.getString(PinchInfo.ADDRESS));

                    System.out.println("LISTA: " + json.getString("cellsList"));
                    String[] cellsString = json.getString("cellsList").replaceAll("\\[", "").replaceAll("\\]", "").split(", ");


                    List<Boolean> cellsToSet = new ArrayList<>();
                    for (String s : cellsString) {
                        cellsToSet.add(Boolean.parseBoolean(s));
                    }

                    int firstIndex = device.getIndexFirstCell();
                    int lastIndex = device.getIndexLastCell();
                    gridView.setPairedCells(firstIndex, lastIndex, cellsToSet,device.getMyDirection());

                    device.setCellsReceived(true);
                }

                lock.unlock();

            } else if(json.getString("type").equals("ready")){

                lock.lock();

                if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))){
                    connectedDevices.get(json.getString(PinchInfo.ADDRESS)).setReadyReceived(true);
                }

                lock.unlock();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean goOn(){
        lock.lock();

        Set<String> set=connectedDevices.keySet();

        for (String s : set){
            if(!connectedDevices.get(s).isCellsReceived()){
                lock.unlock();
                return false;
            }
        }

        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setCellsReceived(false);
        }

        lock.unlock();

        return true;

    }

    public void sendCommand(JSONObject message,String ip){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        if(ip==null) {
            for (String s : set) {
                rabbitMQ.sendMessage(connectedDevices.get(s).getNameQueueSender(), message);
            }
        }else{
            for (String s : set) {
                if(!ip.equals(s)) {
                    rabbitMQ.sendMessage(connectedDevices.get(s).getNameQueueSender(), message);
                }
            }
        }

        lock.unlock();

    }


    public boolean readyToSendCells(){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        for (String s : set){

            if(!connectedDevices.get(s).isReadyReceived()){
                lock.unlock();
                return false;
            }
        }


        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setReadyReceived(false);
        }

        lock.unlock();

        return true;
    }

    public void sendCellsToOthers(){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        for (String s : set){
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

        lock.unlock();
    }

    //invio a tutti i device collegati un messaggio che indica
    //che sono pronto a inviare a loro le mie celle per la generazione successiva
    public void readyToContinue(){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        for (String s : set){
            JSONObject obj = new JSONObject();
            ConnectedDeviceInfo infoConn = connectedDevices.get(s);
            String queueSender = infoConn.getNameQueueSender();
            try {
                obj.put("type","ready");
                obj.put(PinchInfo.ADDRESS,ipAddress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            rabbitMQ.sendMessage(queueSender, obj);
        }

        lock.unlock();
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
                }

                connectedDevices.clear();

            }

            lock.unlock();
        }
    }

    public boolean stopGame(){

        lockStop.lock();
        boolean tmp=stop;
        lockStop.unlock();

        return tmp;
    }

    public void stopGame(boolean value){
        lockStop.lock();
        stop=value;
        lockStop.unlock();
    }

    public void resetGame(boolean value){
        lockStop.lock();
        reset=value;
        lockStop.unlock();
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
