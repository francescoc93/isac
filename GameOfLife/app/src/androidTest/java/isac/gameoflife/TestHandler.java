package isac.gameoflife;

import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.GeneralSwipeAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Swipe;
import android.support.test.espresso.action.Tap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Created by Francesco on 24/04/2017.
 */

@RunWith(AndroidJUnit4.class)
public class TestHandler {

    private static boolean flag=true;
    private static GridView gridView;
    private static FakeDevice device;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);


    @Before
    public void setup(){
        if(flag){
            flag=false;
            device=new FakeDevice();
        }
    }


    @Test
    public void testPinchAndDetachment(){
        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);

        //all'inizio il device non è connesso
        assertFalse(gridView.getGameHandler().isConnected());

        //simulo lo swipe
        doSwipe();

        //il device fake invia uno swipe
        device.sendSwipe();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //i device sono collegati
        assertTrue(gridView.getGameHandler().isConnected());

        //creo le code per il device fake
        device.addQueue();

        //scollego il device
        device.detachDevice();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //i device non sono più collegati
        assertFalse(gridView.getGameHandler().isConnected());
    }



    @Test
    public void testCommunicationBetweenDevice(){

        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                    .findViewById(android.R.id.content)).getChildAt(0);

        //setto le celle vive
        setCell();
        //simulo lo swipe
        doSwipe();

        //il device fake invia lo swipe
        device.sendSwipe();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //controllo che i device siano connessi
        assertTrue(gridView.getGameHandler().isConnected());
        assertTrue(gridView.getGameHandler().stopGame());

        //creo le code per il device fake
        device.addQueue();

        JSONObject message=new JSONObject();

        try {
            message.put(PinchInfo.ADDRESS,"127.0.0.1");
            message.put("type","start");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //il device fake invia al device reale il comando per iniziare il gioco
        device.sendMessage(message);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFalse(gridView.getGameHandler().stopGame());

        ArrayList<Boolean> list=new ArrayList<>();

        for(int i=0;i<11;i++){

            if(i>=4 && i<=6){
                list.add(true);
            }else{
                list.add(false);
            }
        }

        message=new JSONObject();
        try {
            message.put(PinchInfo.ADDRESS,"127.0.0.1");
            message.put("type","cells");
            message.put("cellsList",list);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //il device fake invia i valori delle celle connesse
        device.sendMessage(message);

        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        message=new JSONObject();

        try {
            message.put(PinchInfo.ADDRESS,"127.0.0.1");
            message.put("type","ready");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //il device fake comunica che è pronto per la generazione successiva
        device.sendMessage(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        message=new JSONObject();

        try {
            message.put(PinchInfo.ADDRESS,"127.0.0.1");
            message.put("type","pause");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //il device fake mette in pausa il gioco
        device.sendMessage(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(gridView.getGameHandler().stopGame());

        message=new JSONObject();

        try {
            message.put(PinchInfo.ADDRESS,"127.0.0.1");
            message.put("type","reset");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //il device fake invia il comando di reset
        device.sendMessage(message);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //scollego i device
        device.detachDevice();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void setCell(){
        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.LONG, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{280,600};
            }
        }, Press.FINGER));

        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.LONG, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{380,600};
            }
        }, Press.FINGER));

        onView(withId(android.R.id.content)).perform(new GeneralClickAction(Tap.LONG, new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {
                return new float[]{450,600};
            }
        }, Press.FINGER));
    }

    private void doSwipe(){
        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT, Press.FINGER));
    }


    private class FakeDevice implements MessageListener{

        private String ipAddress;
        private RabbitMQ rabbitMQ;

        public FakeDevice(){
            this.ipAddress="127.0.0.1";
            rabbitMQ=new RabbitMQ(Utils.getServerAddress(),"[user]","[user]");
            rabbitMQ.connect();
            rabbitMQ.addPublishExchange("broadcast","fanout");
            rabbitMQ.addSubscribeQueue("broadcast","fanout",this);
        }

        public void sendSwipe(){
            rabbitMQ.sendMessage("broadcast",new PinchInfo(ipAddress,PinchInfo.Direction.LEFT,0,400,
                    System.currentTimeMillis(),800,1200,240,240).toJSON());
        }


        public void addQueue(){
            rabbitMQ.addQueue(ipAddress+Utils.getIpAddress());
            rabbitMQ.addQueue(Utils.getIpAddress()+ipAddress,this);
        }

        public void detachDevice(){
            JSONObject json=new JSONObject();
            try {
                json.put("type","close");
                json.put(PinchInfo.ADDRESS,ipAddress);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            rabbitMQ.sendMessage(ipAddress+Utils.getIpAddress(),json);
            rabbitMQ.close(ipAddress+Utils.getIpAddress());
            rabbitMQ.close(Utils.getIpAddress()+ipAddress);
        }

        public void sendMessage(JSONObject message){
            rabbitMQ.sendMessage(ipAddress+Utils.getIpAddress(),message);
        }

        public void closeConnection(){
            rabbitMQ.closeConnection();
        }

        @Override
        public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
            try {
                System.out.println("TEST HANDLER TYPE "+json.getString("type"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
