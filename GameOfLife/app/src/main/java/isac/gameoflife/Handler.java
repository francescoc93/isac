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
     * Connects to RabbitMQ's server
     * @return true if connection was established. False otherwise
     */
    public boolean connectToServer(){
        return rabbitMQ.connect();
    }

    /**
     * Binds the queue to an exchange
     */
    public void bindToBroadcastQueue(){
        if(rabbitMQ.isConnected()){
            rabbitMQ.addSubscribeQueue("broadcast", "fanout",this);
        }
    }

    /**
     * Sends a broadcast message to all devices who are running this application
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
     * Checks if all the neighbours have sent the cells.
     * @return true if the cells from all the neighbours were received. False otherwise
     */
    public boolean goOn(){
        lock.lock();

        //get the list of neighbours
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
     * Checks if all the neighbours (eventually all the neighbours except the sender of "pause" command)
     * have sent the cells.
     * @return true if the cells from all the neighbours was received. False otherwise
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
     * Resets the flag that checks who sent the cells
     */
    public void resetCellsReceived(){

        lock.lock();

        for (String s : connectedDevices.keySet()){
            connectedDevices.get(s).setCellsReceived(false);
        }

        lock.unlock();
    }

    /**
     * Resets the flag that checks who was ready to begin another generation
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
     * @param ip if it is null, the message will be sent to all the neighbours. Otherwise, the message will
     *           be sent to all neighbours except that one (if it is present in the neighbours list)
     */
    public void sendCommand(JSONObject message,String ip){

        lock.lock();

        Set<String> set=connectedDevices.keySet();

        if(ip==null) {

            try {
                //sets state of the game
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
     * Checks if all the neighbours are ready to begin another generation.
     * @return true if they're ready. False otherwise
     */
    public boolean readyToSendCells(){

        lock.lock();

        //gets the list of neighbors
        Set<String> set=connectedDevices.keySet();

        //checks if someone isn't ready
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
     * Sends the cells to all neighbors
     */
    public void sendCellsToOthers(){

        lock.lock();

        //gets list of neighbors
        Set<String> set=connectedDevices.keySet();

        for (String s : set){
            JSONObject obj = new JSONObject();
            ConnectedDeviceInfo infoConn = connectedDevices.get(s);
            //gets the name of queue to send the message to
            String queueSender = infoConn.getNameQueueSender();
            //creates the message and adds the list of cells to send
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
     * Sends to all neighbours the will to begin a new generation
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
     * Closes the channels with all the neighbours
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
                    //sends a message that informs the shut down of the channel
                    rabbitMQ.sendMessage(device.getNameQueueSender(), message);
                    //closes the channels
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
     * Closes connection with RabbitMQ's server
     */
    public void closeConnection(){
        rabbitMQ.closeConnection();
    }

    /**
     * Checks if it is necessary to stop the game
     * @return true if is necessary stop the game. False otherwise
     */
    public boolean stopGame(){

        lockStop.lock();
        boolean tmp=stop;
        lockStop.unlock();

        return tmp;
    }

    /**
     * Checks if the device is connected with someone else
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
     * Checks if the message incoming is from itself
     * @param ipAddressDevice IP address
     * @return true if message incoming is from another device. False otherwise
     */
    private boolean messageFromOther (String ipAddressDevice){
        return !ipAddress.equals(ipAddressDevice);
    }

    /**
     * Closes the channel
     * @param name name of the queue or the exchange
     */
    private void closeCommunication(String name){
        rabbitMQ.close(name);
    }

    /**
     * Handles the message of pinch. Whenever a swipe is performed, this method is invoked
     * @param json incoming message
     */
    private void handlePinch(JSONObject json){

        try {
            //gets all infos of the swipe of other device that sent the message
            PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS), PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),
                    json.getInt(PinchInfo.X_COORDINATE),
                    json.getInt(PinchInfo.Y_COORDINATE), json.getLong(PinchInfo.TIMESTAMP),
                    Float.parseFloat(json.getString(PinchInfo.SCREEN_WIDTH)), Float.parseFloat(json.getString(PinchInfo.SCREEN_HEIGHT)),
                    Float.parseFloat(json.getString(PinchInfo.XDPI)), Float.parseFloat(json.getString(PinchInfo.YDPI)));

            //gets the infos of my last swipe
            Pair<Pair<Long, PinchInfo.Direction>, Pair<Integer, Integer>> infoSwipe = gridView.getInfoSwipe();


            //if the device has performed a swipe and the message has arrived from another one
            if (infoSwipe != null && messageFromOther(info.getAddress())) {

                Pair<Long, PinchInfo.Direction> timeStampDirection = infoSwipe.first;
                Pair<Integer, Integer> coordinate = infoSwipe.second;

                lock.lock();

                //if that device isn't my neighbour yet
                if (!connectedDevices.containsKey(info.getAddress())) {

                    lock.unlock();

                    System.out.println("Elapsed time from swipe: "+(System.currentTimeMillis()-timeStampDirection.first));

                    //checks how much time has elapsed between the two swipes
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

                        //adds the queues for sending and receiving messages to/from that device
                        rabbitMQ.addQueue(nameSender);
                        rabbitMQ.addQueue(nameReceiver, this);

                        //creates the info about that device
                        ConnectedDeviceInfo connectionInfo = new ConnectedDeviceInfo(this.cellSize,
                                info.getDirection(), timeStampDirection.second,
                                info.getXcoordinate(), info.getYcoordinate(), info.getScreenWidth(), info.getScreenHeight(), this.myWidth,
                                this.myHeight, coordinate.first, coordinate.second, nameSender, nameReceiver, this.gridView,
                                info.getXDpi(), info.getYDpi(), gridView.getXDpi(), gridView.getYDpi());

                        lock.lock();
                        //add the device to neighbour's map
                        connectedDevices.put(ipAddressDevice, connectionInfo);
                        lock.unlock();
                        //calculates how many and which cells sends/receives from/to that device
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
     * Handles the message of closing. Whenever a neighbours detached, this method is invoked.
     * @param json incoming message
     */
    private void handleClose(JSONObject json){
        try{
            ConnectedDeviceInfo deviceInfo=null;

            lock.lock();

            if (connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))) {
                deviceInfo = connectedDevices.remove(json.getString(PinchInfo.ADDRESS));
                //removes the device from neighbours

                if(connectedDevices.size()==0){
                    //if there are no neighbors anymore, stops the game (if it is running)
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
                //closes the channels
                closeCommunication(deviceInfo.getNameQueueSender());
                closeCommunication(deviceInfo.getNameQueueReceiver());
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Handles the message of start. Whenever a neighbour starts the game, this method is invoked.
     * @param json incoming message
     */
    private void handleStart(JSONObject json){
        try{
            boolean flag=false;
            lockStop.lock();

            //checks if the device has already started the game
            if(stop) {
                stop = false;
                flag=true;
            }

            lockStop.unlock();

            //If the device hasn't started the game yet and it is connected with some others devices
            if(flag && isConnected()) {
                //starts the game
                gridView.start();

                JSONObject message=new JSONObject();

                try {
                    message.put("type","start");
                    message.put(PinchInfo.ADDRESS,ipAddress);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //forwards the message of start to the other neighbours (except the sender)
                sendCommand(message,json.getString(PinchInfo.ADDRESS));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * Handles the message of pause. Whenever a neighbours stops the game, this method is invoked.
     * @param json incoming message
     */
    private void handlePause(JSONObject json){
        try{
            //gets which device performed the pause command
            senderCommand=json.getString("sender");

            lockStop.lock();

            boolean flag=false;

            //checks if the device has already stopped the game
            if(!stop) {
                stop = true;
                flag=true;
            }

            lockStop.unlock();

            //If the device hasn't stopped the game yet and it is connected to some other device
            if(flag && isConnected()){

                JSONObject message=new JSONObject();
                try {
                    message.put("type","pause");
                    message.put(PinchInfo.ADDRESS,ipAddress);
                    message.put("sender",json.getString("sender"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //forwards the message of pause to the other neighbours (except the sender)
                sendCommand(message,json.getString(PinchInfo.ADDRESS));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
    }

    /**
     * This method is invoked whenever a neihbours has sent the cells to calculate the next generation
     * @param json incoming message
     */
    private void handleCells(JSONObject json){
        try{
            lock.lock();

            if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))){

                ConnectedDeviceInfo device=connectedDevices.get(json.getString(PinchInfo.ADDRESS));

                //gets the list of cells
                String[] cellsString = json.getString("cellsList").replaceAll("\\[", "").replaceAll("\\]", "").split(", ");


                //parses the value of the cells
                List<Boolean> cellsToSet = new ArrayList<>();
                for (String s : cellsString) {
                    cellsToSet.add(Boolean.parseBoolean(s));
                }

                //gets the first and last indices of the cells to set
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
     * This method is invoked whenever a neighbour is read to begin the calculus of the next generation
     * @param json incoming message
     */
    private void handleReady(JSONObject json){
        try{
            lock.lock();
            lockStop.lock();

            if(connectedDevices.containsKey(json.getString(PinchInfo.ADDRESS))&&!stop){
                lockStop.unlock();
                //sets the flag that indicates that the device is ready to continue
                connectedDevices.get(json.getString(PinchInfo.ADDRESS)).setReadyReceived(true);
            }else{
                //If the device has received the message when the game was stopped, it is just ignored.
                lockStop.unlock();
            }

            lock.unlock();
        }catch(JSONException e){
            e.printStackTrace();
        }
    }
}
