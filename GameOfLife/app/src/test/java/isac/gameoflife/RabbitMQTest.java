package isac.gameoflife;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Created by Francesco on 23/04/2017.
 */

public class RabbitMQTest {

    private static final int NUMBER_OF_TEST=4;
    private static boolean flag=true;
    private static RabbitMQ rabbitMQ;
    private static int counter=0;

    @Before
    public void setup(){
        if(flag){
            flag=false;
            rabbitMQ=new RabbitMQ("192.168.1.100","[user]","[user]");
            rabbitMQ.connect();
        }
    }

    @After
    public void closeConnection(){
        if(counter==NUMBER_OF_TEST){
            rabbitMQ.closeConnection();
            assertFalse("Connection closed",rabbitMQ.isConnected());
        }
    }

    @Test
    public void connect(){
        assertTrue("Connected",rabbitMQ.isConnected());
        counter++;
    }

    @Test
    public void testQueue(){
        TestQueue bob=new TestQueue("Bob","Ted");
        TestQueue ted=new TestQueue("Ted","Bob");

        bob.addQueue();
        bob.sendMessage();
        ted.addQueue();

        bob.sendMessage();
        ted.sendMessage();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rabbitMQ.close("TedBob");
        rabbitMQ.close("BobTed");
        counter++;
    }


    @Test
    public void testPublishSubscribe(){
        TestPublishSubscribe publish=new TestPublishSubscribe("Ted","Bob");
        TestPublishSubscribe subscribe=new TestPublishSubscribe("Bob","Ted");
        publish.addExchange();
        subscribe.addSubscribe();
        publish.sendMessage();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rabbitMQ.close("broadcast");

        counter++;
    }

    @Test
    public void testPublishSubscribe2(){
        TestPublishSubscribe tmp=new TestPublishSubscribe("John","John");
        tmp.addSubscribe();
        tmp.sendMessage();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        rabbitMQ.close("broadcast");

        counter++;
    }

    private class TestPublishSubscribe implements MessageListener{

        private String name1,name2;

        public TestPublishSubscribe(String name1,String name2){
            this.name1=name1;
            this.name2=name2;
        }

        @Test
        public void addExchange(){
            assertTrue("Exchange added",rabbitMQ.addPublishExchange("broadcast","fanout"));
        }

        @Test
        public void addSubscribe(){
            assertTrue("Subscribe added",rabbitMQ.addSubscribeQueue("broadcast","fanout",this));
        }

        public void sendMessage(){
            try {
                JSONObject message=new JSONObject();
                message.put("sender",name1);
                rabbitMQ.sendMessage("broadcast",message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
            try {
                assertTrue("Message received",json.getString("sender").equals(name2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private class TestQueue implements MessageListener {

        private String queueSender,queueReceiver,name1,name2;

        public TestQueue(String name1,String name2){
            this.name1=name1;
            this.name2=name2;
            queueSender=name1+name2;
            queueReceiver=name2+name1;
        }

        @Test
        public void addQueue(){
            assertTrue("Queue added",rabbitMQ.addQueue(queueSender));
            assertTrue("Queue added",rabbitMQ.addQueue(queueReceiver,this));
        }

        public void sendMessage(){
            try {
                JSONObject message=new JSONObject();
                message.put("sender",name1);
                rabbitMQ.sendMessage(queueSender,message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        @Test
        public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
            try {
                assertTrue("Message received",json.getString("sender").equals(name2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}
