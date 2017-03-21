package isac.gameoflife;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Francesco on 11/03/2017.
 */

public class RabbitMQ{

    private ConnectionFactory factory;
    private Connection connection;
    private AtomicBoolean connected=new AtomicBoolean(false);
    private HashMap<String,Channel> exchange,queue;
    private String address,username,password;

    public RabbitMQ(String address,String username,String password){
        this.address=address;
        this.username=username;
        this.password=password;
        connection=null;
        exchange=new HashMap<>();
        queue=new HashMap<>();
    }

    public boolean connect(){
        factory = new ConnectionFactory();
        factory.setHost(address);
        factory.setUsername(username);
        factory.setPassword(password);
        try {
            connection = factory.newConnection();
            connected.set(true);
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        } finally{
            return connected.get();
        }
    }

    public synchronized boolean addQueue(String name){
        if(!queue.containsKey(name) && !exchange.containsKey(name)){
            try {
                Channel tmp=createChannel();
                tmp.queueDeclare(name, false, false, false, null);
                queue.put(name,tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }else if(queue.containsKey(name)){
            return true;
        }

        return false;
    }

    public synchronized boolean addQueue(String name, final MessageListener listener){
        if(addQueue(name)) {
            addListener(listener, queue.get(name), name);
            return true;
        }

        return false;
    }

    public synchronized boolean addPublishExchange(String name,String mode){
        if(!exchange.containsKey(name) && !queue.containsKey(name)){
            try {
                Channel tmp=createChannel();
                tmp.exchangeDeclare(name,mode);
                exchange.put(name,tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }else if(exchange.containsKey(name)){
            return true;
        }

        return false;
    }

    public synchronized boolean addSubscribeQueue(String name,String mode,MessageListener listener){
        if(addPublishExchange(name,mode)) {
            try {
                Channel tmp = exchange.get(name);
                String queueName = tmp.queueDeclare().getQueue();
                tmp.queueBind(queueName, name, "");
                addListener(listener, tmp, queueName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }

    public synchronized void sendMessage(String name,JSONObject message){
        try {
            if (queue.containsKey(name)) {
                queue.get(name).basicPublish("", name, null, message.toString().getBytes());
            } else if (exchange.containsKey(name)) {
                exchange.get(name).basicPublish(name, "", null, message.toString().getBytes());
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public synchronized void closeConnection() {
        Set<String> setQueue = queue.keySet();
        Set<String> setExchange = exchange.keySet();

        try {
            for(String tmp : setQueue) {
                queue.remove(tmp).close();
            }

            for(String tmp:setExchange){
                exchange.remove(tmp).close();
            }

            connection.close();
            connected.set(false);
        }catch(IOException | TimeoutException e){
            e.printStackTrace();
        }
    }

    public synchronized void close(String name){
        try {
            if (queue.containsKey(name)) {
                queue.remove(name).close();
            } else if (exchange.containsKey(name)) {
                exchange.remove(name).close();
            }
        }catch(IOException | TimeoutException e){
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return connected.get();
    }

    private Channel createChannel(){
        try {
            return connection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void addListener(final MessageListener listener,Channel channel,String name){

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {
                    listener.handleMessage(consumerTag,envelope,properties,new JSONObject(new String(body,"UTF-8")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            channel.basicConsume(name, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
