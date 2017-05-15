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

@RunWith(AndroidJUnit4.class)
public class TestHandler {

    private static boolean flag=true;
    private static GridView gridView;
    private static FakeDevice device;
    private static JSONObject messageStart,messagePause,messageClose;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);


    @Before
    public void setup(){
        if(flag){
            flag=false;
            device=new FakeDevice("127.0.0.1");

            messageStart=new JSONObject();
            try {
                messageStart.put("type","start");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            messagePause=new JSONObject();
            try {
                messagePause.put("type","pause");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            messageClose=new JSONObject();
            try {
                messageClose.put("type","close");
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    @Test
    public void testPinchAndDetachment(){
        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);

        assertFalse(gridView.getGameHandler().isConnected());

        doSwipe(GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT);

        device.sendSwipe(PinchInfo.Direction.LEFT,0,400);

        delay(3000);

        assertTrue(gridView.getGameHandler().isConnected());

        device.addQueue();

        device.detachDevice();

        delay(1000);

        assertFalse(gridView.getGameHandler().isConnected());
    }


    @Test
    public void testCommunicationBetweenDevice(){

        JSONObject message;

        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                    .findViewById(android.R.id.content)).getChildAt(0);

        setCell();
        doSwipe(GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT);

        device.sendSwipe(PinchInfo.Direction.LEFT,0,400);

        delay(1000);

        assertTrue(gridView.getGameHandler().isConnected());
        assertTrue(gridView.getGameHandler().stopGame());

        device.addQueue();

        delay(1000);

        device.sendMessage(messageStart);

        delay(500);

        assertTrue(gridView.isStarted());
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
            message.put("type","cells");
            message.put("cellsList",list);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        device.sendMessage(message);

        delay(200);


        assertTrue(gridView.isStarted());
        //il device fake mette in pausa il gioco
        device.sendMessage(messagePause);

        delay(1000);

        assertTrue(gridView.getGameHandler().stopGame());
        assertFalse(gridView.isStarted());

        delay(1000);

        device.detachDevice();

        delay(5000);

        assertFalse(gridView.getGameHandler().isConnected());
    }


    @Test
    public void testCommunicationBetweenDevice2(){

        JSONObject message;

        FakeDevice device2=new FakeDevice("192.168.43.34");

        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);

        setCell();

        doSwipe(GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT);
        device.sendSwipe(PinchInfo.Direction.LEFT,0,400);

        delay(3000);

        doSwipe(GeneralLocation.CENTER,GeneralLocation.CENTER_LEFT);
        device2.sendSwipe(PinchInfo.Direction.RIGHT,800,400);

        delay(1000);

        assertTrue(gridView.getGameHandler().isConnected());
        assertTrue(gridView.getGameHandler().stopGame());

        device.addQueue();
        device2.addQueue();

        delay(5000);

        device2.sendMessage(messageStart);
        device.sendMessage(messageStart);

        delay(500);

        assertTrue(gridView.isStarted());
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
            message.put("type","cells");
            message.put("cellsList",list);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        device.sendMessage(message);
        device2.sendMessage(message);

        delay(500);

        assertTrue(gridView.isStarted());
        //il device fake mette in pausa il gioco
        device.sendMessage(messagePause);

        delay(1000);

        assertTrue(gridView.getGameHandler().stopGame());
        assertFalse(gridView.isStarted());


        list.clear();

        for(int i=0;i<11;i++){

            if(i<=2){
                list.add(true);
            }else{
                list.add(false);
            }
        }

        JSONObject listMessage=new JSONObject();
        try {
            message.put("type","cells");
            message.put("cellsList",list);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        device2.sendMessage(listMessage);
        device2.sendMessage(messagePause);

        delay(5000);

        device.sendMessage(messageStart);
        device2.sendMessage(messageStart);

        delay(5000);

        assertFalse(gridView.getGameHandler().stopGame());
        assertTrue(gridView.isStarted());

        device.sendMessage(messagePause);
        device2.sendMessage(messagePause);

        delay(1000);

        assertTrue(gridView.getGameHandler().stopGame());
        assertFalse(gridView.isStarted());

        device.detachDevice();
        device2.detachDevice();

        delay(5000);

        assertFalse(gridView.getGameHandler().isConnected());
    }


    @Test
    public void testCloseCommunication(){

        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);

        setCell();
        doSwipe(GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT);

        device.sendSwipe(PinchInfo.Direction.LEFT,0,400);

        delay(1000);

        assertTrue(gridView.getGameHandler().isConnected());
        assertTrue(gridView.getGameHandler().stopGame());

        device.addQueue();

        delay(1000);

        device.sendMessage(messageStart);

        delay(5000);

        assertFalse(gridView.getGameHandler().stopGame());

        device.sendMessage(messageClose);

        delay(3000);

        assertTrue(gridView.getGameHandler().stopGame());
        assertFalse(gridView.getGameHandler().isConnected());

        delay(1000);
    }



    @Test
    public void testCloseCommunicationAfterSendCells(){

        JSONObject message;

        gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                .findViewById(android.R.id.content)).getChildAt(0);


        setCell();

        doSwipe(GeneralLocation.CENTER,GeneralLocation.CENTER_RIGHT);

        device.sendSwipe(PinchInfo.Direction.LEFT,0,400);

        delay(1000);

        assertTrue(gridView.getGameHandler().isConnected());
        assertTrue(gridView.getGameHandler().stopGame());


        device.addQueue();

        delay(1000);

        device.sendMessage(messageStart);

        delay(500);

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
            message.put("type","cells");
            message.put("cellsList",list);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        device.sendMessage(message);

        delay(4000);

        device.sendMessage(messageClose);

        delay(1000);

        assertTrue(gridView.getGameHandler().stopGame());
        assertFalse(gridView.getGameHandler().isConnected());

        delay(1000);

    }

    private void delay(long millis){
        try {
            Thread.sleep(millis);
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

    private void doSwipe(GeneralLocation from,GeneralLocation to){
        onView(withId(android.R.id.content)).perform(new GeneralSwipeAction(Swipe.FAST,
                from,to, Press.FINGER));
    }


    private class FakeDevice implements MessageListener{

        private String ipAddress;
        private RabbitMQ rabbitMQ;

        public FakeDevice(String ipAddress){
            this.ipAddress=ipAddress;
            rabbitMQ=new RabbitMQ(Utils.getServerAddress(),"[user]","[user]");
            rabbitMQ.connect();
            rabbitMQ.addSubscribeQueue("broadcast","fanout",this);
        }

        public void sendSwipe(PinchInfo.Direction direction,int x,int y){
            rabbitMQ.sendMessage("broadcast",new PinchInfo(ipAddress,direction,x,y,
                    System.currentTimeMillis(),5,7,240,240).toJSON());
        }


        public void addQueue(){
            rabbitMQ.addQueue(ipAddress+Utils.getIpAddress());
            rabbitMQ.addQueue(Utils.getIpAddress()+ipAddress,this);
        }

        public void detachDevice(){
            sendMessage(messageClose);
            rabbitMQ.close(ipAddress+Utils.getIpAddress());
            rabbitMQ.close(Utils.getIpAddress()+ipAddress);
        }

        public void sendMessage(JSONObject message){
            JSONObject tmp=null;
            try {
                tmp=new JSONObject(message.toString());
                tmp.put(PinchInfo.ADDRESS,ipAddress);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            rabbitMQ.sendMessage(ipAddress+Utils.getIpAddress(),tmp);
        }

        public void closeConnection(){
            rabbitMQ.closeConnection();
        }

        @Override
        public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
        }
    }
}
