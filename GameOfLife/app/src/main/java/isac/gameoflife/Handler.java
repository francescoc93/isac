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

    /**
     *
     * @param gridView
     * @param activity
     * @param myWidth device's width in inches
     * @param myHeight device's height in inches
     * @param cellSize cell's size in inches
     */
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

    /**
     * Connect to RabbitMQ's server
     * @return true if connection was established. False otherwise
     */
    public boolean connectToServer(){
        return rabbitMQ.connect();
    }

    /**
     * Bind the queue to an exchange
     */
    public void bindToBroadcastQueue(){
        if(rabbitMQ.isConnected()){
            rabbitMQ.addSubscribeQueue("broadcast", "fanout",this);
        }
    }

    /**
     * Send a broadcast message to all devices who are running this application
     * @param message message to send
     * @return true if message was sent. False otherwise
     */
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

    /**
     * Check if all the neighbors have sent the cells.
     * @return true if the cells from all the neighbors was received. False otherwise
     */
    public boolean goOn(){
        lock.lock();

        //get the list of neighbors
        Set<String> set=connectedDevices.keySet();

        for (String s : set){
            //check if someone didn't send the cells
            if(!connectedDevices.get(s).isCellsReceived()){
                lock.unlock();
                return false;
            }
        }

        lock.unlock();

        return true;
    }

    /**
     * Check if all the neighbors (eventually all the neighbors except the sender of "pause" command)
     * have sent the cells.
     * @return true if the cells from all the neighbors was received. False otherwise
     */
    public boolean cellsReceived(){
        lock.lock();

        Set<String> set=connectedDevices.keySet();
        ArrayList<String> list=new ArrayList<>();

        //add to the list which devices didn't send the cells
        for (String s : set){
            if(!connectedDevices.get(s).isCellsReceived()){
                list.add(s);
            }
        }

        lock.unlock();

        //if list have size 0 or contain only the sender of "pause" command
        if(list.size()==0||(list.size()==1&&list.contains(senderCommand))){
            senderCommand="";
            return true;
        }

        return false;
    }

    /**
     * Reset the flag that check who sent the cells
     */
    public void resetCellsReceived(){

        lock.lock();

        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setCellsReceived(false);
        }

        lock.unlock();
    }

    /**
     * Reset the flag that check who was ready for beginning another generation
     */
    public void resetReadyReceived(){

        lock.lock();

        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setReadyReceived(false);
        }

        lock.unlock();
    }

    /**
     * Send a command of pause/start to all neighbors
     * @param message message of pause/start
     * @param ip if is null, the message will be send to all neighbors. Otherwise, the message will
     *           be send to all neighbors except for that one (if is present in the neighbors list)
     */
    public void sendCommand(JSONObject message,String ip){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        if(ip==null) {

            try {
                //set state of the game
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

    /**
     * Check if all the neighbors are ready for beginning another generation.
     * @return true if they're ready. False otherwise
     */
    public boolean readyToSendCells(){

        lock.lock();

        //get the list of neighbors
        Set<String> set=connectedDevices.keySet();

        //check if someone isn't ready
        for (String s : set){
            if(!connectedDevices.get(s).isReadyReceived()){
                lock.unlock();
                return false;
            }
        }

        lock.unlock();

        return true;
    }

    /**
     * Send the cells to all neighbors
     */
    public void sendCellsToOthers(){

        lock.lock();

        //get list of neighbors
        Set<String> set=connectedDevices.keySet();

        for (String s : set){
            JSONObject obj = new JSONObject();
            ConnectedDeviceInfo infoConn = connectedDevices.get(s);
            //get the name of queue to send the message
            String queueSender = infoConn.getNameQueueSender();
            //create the message and add the list of cells to send
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

    /**
     * Send to all neighbors the will of beginning a new generation
     */
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

    /**
     * Close the channels with all the neighbors
     */
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
                    //send a message that inform the shutting down of the channel
                    rabbitMQ.sendMessage(device.getNameQueueSender(), message);
                    //close the channels
                    closeCommunication(device.getNameQueueSender());
                    closeCommunication(device.getNameQueueReceiver());
                }

                //clear the list of neighbors
                connectedDevices.clear();
            }

            lock.unlock();
        }
    }

    /**
     * Close connection with RabbitMQ's server
     */
    public void closeConnection(){
        rabbitMQ.closeConnection();
    }

    /**
     * Check if is necessary stop the game
     * @return true if is necessary stop the game. False otherwise
     */
    public boolean stopGame(){

        lockStop.lock();
        boolean tmp=stop;
        lockStop.unlock();

        return tmp;
    }

    /**
     * Check if the device is connected with someone else
     *
     * @return true if is connected. False otherwise
     */
    public boolean isConnected(){
        lock.lock();

        boolean tmp=connectedDevices.size()==0?false:true;

        lock.unlock();

        return tmp;
    }

    /**
     * Check if message incoming from itself
     * @param ipAddressDevice IP address
     * @return true if message incoming from another one. False otherwise
     */
    private boolean messageFromOther (String ipAddressDevice){
        return !ipAddress.equals(ipAddressDevice);
    }

    /**
     * Close the channel
     * @param name name of the queue or the exchange
     */
    private void closeCommunication(String name){
        rabbitMQ.close(name);
    }

    /**
     * Handle the message of pinch. When this method is invoked, a swipe was performed
     * @param json incoming message
     */
    private void handlePinch(JSONObject json){

        try {
            //get all info of the swipe of other device that sent the message
            PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS), PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),
                    json.getInt(PinchInfo.X_COORDINATE),
                    json.getInt(PinchInfo.Y_COORDINATE), json.getLong(PinchInfo.TIMESTAMP),
                    Float.parseFloat(json.getString(PinchInfo.SCREEN_WIDTH)), Float.parseFloat(json.getString(PinchInfo.SCREEN_HEIGHT)),
                    Float.parseFloat(json.getString(PinchInfo.XDPI)), Float.parseFloat(json.getString(PinchInfo.YDPI)));

            //get the info of my last swipe
            Pair<Pair<Long, PinchInfo.Direction>, Pair<Integer, Integer>> infoSwipe = gridView.getInfoSwipe();


            //if i performed a swipe and the message is has arrived from another one
            if (infoSwipe != null && messageFromOther(info.getAddress())) {

                Pair<Long, PinchInfo.Direction> timeStampDirection = infoSwipe.first;
                Pair<Integer, Integer> coordinate = infoSwipe.second;

                lock.lock();

                //if that device isn't yet my neighbor
                if (!connectedDevices.containsKey(info.getAddress())) {

                    lock.unlock();

                    System.out.println("Elapsed time from swipe: "+(System.currentTimeMillis()-timeStampDirection.first));

                    //check how many time has elapsed between the two swipes
                    if (Math.abs(info.getTimestamp()-timeStampDirection.first)<=2000) {

                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, "Screen connected", Toast.LENGTH_SHORT).show();
                            }
                        });


                        String nameSender = "", nameReceiver = "";
                        String ipAddressDevice = info.getAddress();

                        nameSender = ipAddress + ipAddressDevice;
                        nameReceiver = ipAddressDevice + ipAddress;

                        //add the queues for send and receive messages to/from that device
                        rabbitMQ.addQueue(nameSender);
                        rabbitMQ.addQueue(nameReceiver, this);

                        //create the info about that device
                        ConnectedDeviceInfo connectionInfo = new ConnectedDeviceInfo(this.cellSize,
                                info.getDirection(), timeStampDirection.second,
                                info.getXcoordinate(), info.getYcoordinate(), info.getScreenWidth(), info.getScreenHeight(), this.myWidth,
                                this.myHeight, coordinate.first, coordinate.second, nameSender, nameReceiver, this.gridView,
                                info.getXDpi(), info.getYDpi(), gridView.getXDpi(), gridView.getYDpi());

                        lock.lock();
                        //add the device to neighbor's map
                        connectedDevices.put(ipAddressDevice, connectionInfo);
                        lock.unlock();
                        //calculate how many and which cells sends/receives from/to that device
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

    /**
     * Handle the message of closing. When this method is invoked, a neighbor was detached
     * @param json incoming message
     */
    private void handleClose(JSONObject json){
        try{
            ConnectedDeviceInfo deviceInfo=null;

            lock.lock();

            if (connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))) {
                deviceInfo = connectedDevices.remove(json.getString(PinchInfo.ADDRESS));
                //remove the device from neighbors

                if(connectedDevices.size()==0){
                    //if there are no neighbors anymore, stop the game (if is running)
                    lockStop.lock();

                    stop = true;

                    lockStop.unlock();

                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(activity, "Screen detached", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            lock.unlock();

            if(deviceInfo!=null && rabbitMQ.isConnected()){
                //close the channels
                closeCommunication(deviceInfo.getNameQueueSender());
                closeCommunication(deviceInfo.getNameQueueReceiver());
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Handle the message of start. When this method is invoked, a neighbor has just started the game
     * @param json incoming message
     */
    private void handleStart(JSONObject json){
        try{
            boolean flag=false;
            lockStop.lock();

            //check if i already started the game
            if(stop) {
                stop = false;
                flag=true;
            }

            lockStop.unlock();

            //if i didn't yet start the game and i'm connected with someone
            if(flag && isConnected()) {
                //start the game
                gridView.start();

                JSONObject message=new JSONObject();

                try {
                    message.put("type","start");
                    message.put(PinchInfo.ADDRESS,ipAddress);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //forward the message of start to the other neighbors (except the sender)
                sendCommand(message,json.getString(PinchInfo.ADDRESS));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Handle the message of pause. When this method is invoked, a neighbor has just stopped the game
     * @param json incoming message
     */
    private void handlePause(JSONObject json){
        try{
            //get who performed the pause command
            senderCommand=json.getString("sender");

            lockStop.lock();

            boolean flag=false;

            //check if i already stopped the game
            if(!stop) {
                stop = true;
                flag=true;
            }

            lockStop.unlock();

            //if i didn't yet stop the game and i'm connected with someone
            if(flag && isConnected()){

                JSONObject message=new JSONObject();
                try {
                    message.put("type","pause");
                    message.put(PinchInfo.ADDRESS,ipAddress);
                    message.put("sender",json.getString("sender"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //forward the message of pause to the other neighbors (except the sender)
                sendCommand(message,json.getString(PinchInfo.ADDRESS));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * when this method is invoked, a neighbor has just sent the cells for calculate of the next generation
     * @param json incoming message
     */
    private void handleCells(JSONObject json){
        try{
            lock.lock();

            if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))){

                ConnectedDeviceInfo device=connectedDevices.get(json.getString(PinchInfo.ADDRESS));

                //get the list of cells
                String[] cellsString = json.getString("cellsList").replaceAll("\\[", "").replaceAll("\\]", "").split(", ");


                //parse the value of the cells
                List<Boolean> cellsToSet = new ArrayList<>();
                for (String s : cellsString) {
                    cellsToSet.add(Boolean.parseBoolean(s));
                }

                //get the first and last index of the cells to set
                int firstIndex = device.getIndexFirstCell();
                int lastIndex = device.getIndexLastCell();
                //set the cell's value
                gridView.setPairedCells(firstIndex, lastIndex, cellsToSet,device.getMyDirection());

                //set the flag that indicate that device has sent the cells
                device.setCellsReceived(true);
            }

            lock.unlock();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * when this method is invoked, a neighbor is ready to beginning the next generation
     * @param json incoming message
     */
    private void handleReady(JSONObject json){
        try{
            lock.lock();
            lockStop.lock();

            if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))&&!stop){
                lockStop.unlock();
                //set the flag that indicate that device is ready to continue
                connectedDevices.get(json.getString(PinchInfo.ADDRESS)).setReadyReceived(true);
            }else{
                //if i receive the message when the game is stopped i ignored it
                lockStop.unlock();
            }

            lock.unlock();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
}
