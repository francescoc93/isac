package isac.gameoflife;

import android.content.Context;
import android.widget.Toast;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Francesco on 16/03/2017.
 */

public class Handler implements MessageListener {

    private GridView gridView;
    private Context context;
    private String ipAddress;
    private RabbitMQ rabbitMQ;
    private HashMap<String,ConnectedDeviceInfo> connectedDevices;
    private int number_device,value_address;
    private boolean portrait;

    public Handler(GridView gridView,Context context){
        ipAddress=Utils.getIpAddress();
        value_address=Integer.parseInt(ipAddress.split("\\.")[3]);
        this.gridView=gridView;
        this.context=context;
        this.rabbitMQ=new RabbitMQ(Utils.getAddress(),"[user]","[user]");;
        number_device=0;
        connectedDevices=new HashMap<>();
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
        System.out.println("Messaggio broadcast ricevuto");
        try {

            if(json.getString("type").equals("pinch")) {
                PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS),/* (PinchInfo.Direction)json.get(PinchInfo.DIRECTION)*/PinchInfo.Direction.valueOf(json.getString(PinchInfo.DIRECTION)),json.getInt(PinchInfo.X_COORDINATE),
                        json.getInt(PinchInfo.Y_COORDINATE), json.getBoolean(PinchInfo.PORTRAIT), json.getLong(PinchInfo.TIMESTAMP),
                        json.getInt(PinchInfo.SCREEN_WIDTH), json.getInt(PinchInfo.SCREEN_HEIGHT), json.getInt(PinchInfo.CONNECTED_DEVICE));
                if (!ipAddress.equals(info.getAddress()) && info.getTimestamp() > gridView.getTimeStamp() - 20 &&
                        info.getTimestamp() < gridView.getTimeStamp() + 20 && info.oppositeDirection(gridView.getDirection())) {
                    System.out.println("DEVICE PAIRED WITH " + info.getAddress());
                    Toast.makeText(context, "Schermo collegato", Toast.LENGTH_SHORT).show();

                    //TODO: METTERE IN CONNECTED DEVICE INFO IL NUMERO DI CELLE
                    //TODO (2) : HANDLE MESSAGE 

                    String nameSender="", nameReceiver="";
                    String ipAddressDevice = info.getAddress();

                    int value_address_device=Integer.parseInt(ipAddressDevice.split(".")[3]);

                    if(value_address>value_address_device){ //se sono il maggiore tra i due
                        nameSender=ipAddress+ipAddressDevice;
                        nameReceiver=ipAddressDevice+ipAddress;
                        rabbitMQ.addQueue(nameSender);
                        rabbitMQ.addQueue(nameReceiver, new MessageListener() {
                            @Override
                            public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {

                            }
                        });
                    }else{ //se sono il minore tra i due
                        nameReceiver=ipAddress+ipAddressDevice;
                        nameSender=ipAddressDevice+ipAddress;
                        rabbitMQ.addQueue(nameReceiver);
                        rabbitMQ.addQueue(nameSender, new MessageListener() {
                            @Override
                            public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {

                            }
                        });
                    }

                    //TODO: calcoli per x e y (pdf)
                    connectedDevices.put(ipAddressDevice,new ConnectedDeviceInfo(info.isPortrait(),
                            info.getXcoordinate(), info.getYcoordinate(), nameSender, nameReceiver));

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send broadcast message to all the devices connected with him
     * @param message
     */
    public void sendMessaggeToConnectedDevices(final JSONObject message){

        if(rabbitMQ.isConnected()) {
            Collection<ConnectedDeviceInfo> devices = connectedDevices.values();

            for (ConnectedDeviceInfo device : devices) {
                rabbitMQ.sendMessage(device.getNameQueueSender(), message);
            }
        }
    }

    public int getNumberConnectedDevice(){
        return number_device;
    }
}
