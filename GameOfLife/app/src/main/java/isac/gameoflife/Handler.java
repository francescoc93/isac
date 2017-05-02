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


public class Handler implements MessageListener {

    private GridView gridView;
    private MainActivity activity;
    private String ipAddress,senderCommand;
    private RabbitMQ rabbitMQ;
    private HashMap<String,ConnectedDeviceInfo> connectedDevices;
    private ReentrantLock lock,lockStop;
    private float cellSize;
    private float myWidth,myHeight;
    private boolean stop;

    public Handler(GridView gridView,final MainActivity activity, float myWidth,float myHeight,float cellSize){

        this.myHeight = myHeight;
        this.myWidth = myWidth;
        ipAddress=Utils.getIpAddress();
        this.gridView=gridView;
        this.cellSize = cellSize;
        this.activity=activity;
        this.rabbitMQ=new RabbitMQ(Utils.getServerAddress(),"[user]","[user]");
        connectedDevices=new HashMap<>();
        lock=new ReentrantLock();
        lockStop=new ReentrantLock();
        stop=true;
        senderCommand="";
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
            switch(json.getString("type")){
                case "pinch":handlePinch(json);break;
                case "close":handleClose(json);break;
                case "start":handleStart(json);break;
                case "pause":handlePause(json);break;
                case "cells":handleCells(json);break;
                case "ready":handleReady(json);break;
                default:break;
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

        lock.unlock();

        return true;
    }

    public boolean cellsReceived(){
        lock.lock();

        Set<String> set=connectedDevices.keySet();
        ArrayList<String> list=new ArrayList<>();

        for (String s : set){
            if(!connectedDevices.get(s).isCellsReceived()){
                list.add(s);
            }
        }

        lock.unlock();

        if(list.size()==0||(list.size()==1&&list.contains(senderCommand))){
            senderCommand="";
            return true;
        }

        return false;
    }

    public void resetCellsReceived(){

        lock.lock();

        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setCellsReceived(false);
        }

        lock.unlock();
    }

    public void resetReadyReceived(){

        lock.lock();

        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setReadyReceived(false);
        }

        lock.unlock();
    }

    public void sendCommand(JSONObject message,String ip){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        if(ip==null) {

            try {
                switch(message.getString("type")){
                    case "start":stop=false;break;
                    case "pause":stop=true;break;
                    default: break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

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
                obj.put(PinchInfo.ADDRESS,ipAddress);
                obj.put("cellsList",infoConn.getCellsValues());
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
            String queueSender = connectedDevices.get(s).getNameQueueSender();
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

    public void closeConnection(){
        rabbitMQ.closeConnection();
    }

    public boolean stopGame(){

        lockStop.lock();
        boolean tmp=stop;
        lockStop.unlock();

        return tmp;
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

    private void handlePinch(JSONObject json){

        try {
            PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS), PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),
                    json.getInt(PinchInfo.X_COORDINATE),
                    json.getInt(PinchInfo.Y_COORDINATE), json.getLong(PinchInfo.TIMESTAMP),
                    Float.parseFloat(json.getString(PinchInfo.SCREEN_WIDTH)), Float.parseFloat(json.getString(PinchInfo.SCREEN_HEIGHT)),
                    Float.parseFloat(json.getString(PinchInfo.XDPI)), Float.parseFloat(json.getString(PinchInfo.YDPI)));

            Pair<Pair<Long, PinchInfo.Direction>, Pair<Integer, Integer>> infoSwipe = gridView.getInfoSwipe();


            if (infoSwipe != null && messageFromOther(info.getAddress())) {

                Pair<Long, PinchInfo.Direction> timeStampDirection = infoSwipe.first;
                Pair<Integer, Integer> coordinate = infoSwipe.second;

                lock.lock();

                if (!connectedDevices.containsKey(info.getAddress())) {

                    lock.unlock();


                    if (Math.abs(info.getTimestamp()-timeStampDirection.first)<=2000) {

                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, "Schermo collegato", Toast.LENGTH_SHORT).show();
                            }
                        });


                        String nameSender = "", nameReceiver = "";
                        String ipAddressDevice = info.getAddress();

                        nameSender = ipAddress + ipAddressDevice;
                        nameReceiver = ipAddressDevice + ipAddress;

                        rabbitMQ.addQueue(nameSender);
                        rabbitMQ.addQueue(nameReceiver, this);

                        ConnectedDeviceInfo connectionInfo = new ConnectedDeviceInfo(this.cellSize,
                                info.getDirection(), timeStampDirection.second,
                                info.getXcoordinate(), info.getYcoordinate(), info.getScreenWidth(), info.getScreenHeight(), this.myWidth,
                                this.myHeight, coordinate.first, coordinate.second, nameSender, nameReceiver, this.gridView,
                                info.getXDpi(), info.getYDpi(), gridView.getXDpi(), gridView.getYDpi());

                        lock.lock();
                        connectedDevices.put(ipAddressDevice, connectionInfo);
                        lock.unlock();
                        connectionInfo.calculateInfo();
                    }
                } else {
                    lock.unlock();
                }
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void handleClose(JSONObject json){
        try{
            ConnectedDeviceInfo deviceInfo=null;

            lock.lock();

            if (connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))) {
                deviceInfo = connectedDevices.remove(json.getString(PinchInfo.ADDRESS));

                if(connectedDevices.size()==0){

                    lockStop.lock();

                    stop = true;

                    lockStop.unlock();

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
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void handleStart(JSONObject json){
        try{
            boolean flag=false;
            lockStop.lock();

            if(stop) {
                stop = false;
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
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void handlePause(JSONObject json){
        try{
            senderCommand=json.getString("sender");

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
                    message.put("sender",json.getString("sender"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sendCommand(message,json.getString(PinchInfo.ADDRESS));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void handleCells(JSONObject json){
        try{
            lock.lock();

            if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))){

                ConnectedDeviceInfo device=connectedDevices.get(json.getString(PinchInfo.ADDRESS));

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
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    private void handleReady(JSONObject json){
        try{
            lock.lock();
            lockStop.lock();

            if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))&&!stop){
                lockStop.unlock();
                connectedDevices.get(json.getString(PinchInfo.ADDRESS)).setReadyReceived(true);
            }else{
                lockStop.unlock();
            }

            lock.unlock();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
}
