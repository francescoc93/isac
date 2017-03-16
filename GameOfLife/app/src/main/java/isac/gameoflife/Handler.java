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
                PinchInfo info = new PinchInfo(json.getString(PinchInfo.ADDRESS), json.getInt(PinchInfo.X_COORDINATE),
                        json.getInt(PinchInfo.Y_COORDINATE), json.getBoolean(PinchInfo.PORTRAIT), json.getLong(PinchInfo.TIMESTAMP),
                        json.getInt(PinchInfo.SCREEN_WIDTH), json.getInt(PinchInfo.SCREEN_HEIGHT), json.getInt(PinchInfo.CONNECTED_DEVICE));
                if (!ipAddress.equals(info.getAddress()) && info.getTimestamp() > gridView.getTimeStamp() - 20 && info.getTimestamp() < gridView.getTimeStamp() + 20) {
                    System.out.println("DEVICE PAIRED WITH " + info.getAddress());
                    Toast.makeText(context, "Schermo collegato", Toast.LENGTH_SHORT).show();

                    //TODO: FINIRE CALCOLO PINCH

                    String nameSender="", nameReceiver="";
                    String ipAddressDevice = info.getAddress();

                    int value_address_device=Integer.parseInt(ipAddressDevice.split(".")[3]);

                    if(value_address>value_address_device){
                        nameSender=ipAddress+ipAddressDevice;
                        nameReceiver=ipAddressDevice+ipAddress;
                    }else{
                        nameReceiver=ipAddress+ipAddressDevice;
                        nameSender=ipAddressDevice+ipAddress;
                    }


                    //TODO: INSERIRE VALORI CORRETTI PRIMI 3 PARAMETRI
                    connectedDevices.put(ipAddressDevice,new ConnectedDeviceInfo(portrait, 0, 0,nameSender, nameReceiver));

                    rabbitMQ.addQueue(nameSender);
                    rabbitMQ.addQueue(nameReceiver, new MessageListener() {
                        @Override
                        public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {

                        }
                    });


                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send broadcast message to all connected devices with him
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
