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
import java.util.concurrent.locks.ReentrantLock;

public class RabbitMQ{

    private ConnectionFactory factory;
    private Connection connection;
    private AtomicBoolean connected=new AtomicBoolean(false);
    private HashMap<String,Channel> exchange,queue;
    private HashMap<String,Long> timeStampExchange,timeStampQueue;
    private String address,username,password;
    private ReentrantLock lock;

    public RabbitMQ(String address,String username,String password){
        this.address=address;
        this.username=username;
        this.password=password;
        connection=null;
        exchange=new HashMap<>();
        queue=new HashMap<>();
        timeStampExchange=new HashMap<>();
        timeStampQueue=new HashMap<>();
        lock=new ReentrantLock();
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

        lock.lock();
        if(!queue.containsKey(name) && !exchange.containsKey(name)){
            try {
                Channel tmp=createChannel();
                tmp.queueDeclare(name, false, false, false, null);
                queue.put(name,tmp);
                timeStampQueue.put(name,System.currentTimeMillis());
            } catch (IOException e) {
                e.printStackTrace();
            }

            lock.unlock();
            return true;
        }else if(queue.containsKey(name)){
            lock.unlock();
            return true;
        }

        lock.unlock();
        return false;
    }

    public synchronized boolean addQueue(String name, final MessageListener listener){
        if(addQueue(name)) {
            lock.lock();
            addListener(listener, queue.get(name), name);
            lock.unlock();
            return true;
        }

        return false;
    }

    public synchronized boolean addPublishExchange(String name,String mode){

        lock.lock();
        if(!exchange.containsKey(name) && !queue.containsKey(name)){
            try {
                Channel tmp=createChannel();
                tmp.exchangeDeclare(name,mode);
                exchange.put(name,tmp);
                timeStampExchange.put(name,System.currentTimeMillis());
            } catch (IOException e) {
                e.printStackTrace();
            }

            lock.unlock();
            return true;
        }else if(exchange.containsKey(name)){
            lock.unlock();
            return true;
        }

        lock.unlock();
        return false;
    }

    public synchronized boolean addSubscribeQueue(String name,String mode,MessageListener listener){
        if(addPublishExchange(name,mode)) {
            try {
                lock.lock();
                Channel tmp = exchange.get(name);
                String queueName = tmp.queueDeclare().getQueue();
                tmp.queueBind(queueName, name, "");
                addListener(listener, tmp, queueName);
                lock.unlock();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        return false;
    }

    public synchronized void sendMessage(String name,JSONObject message){
        try {
            message.put("timestamp",System.currentTimeMillis());

            lock.lock();
            if (queue.containsKey(name)) {
                queue.get(name).basicPublish("", name, null, message.toString().getBytes());
            } else if (exchange.containsKey(name)) {
                exchange.get(name).basicPublish(name, "", null, message.toString().getBytes());
            }
            lock.unlock();
        }catch (IOException | JSONException e){
            e.printStackTrace();
        }
    }

    public synchronized void closeConnection() {
        lock.lock();
        Set<String> setQueue = queue.keySet();
        Set<String> setExchange = exchange.keySet();

        try {
            for(String tmp : setQueue) {
                /*queue.remove(tmp).close();
                timeStampQueue.remove(tmp);*/
                close(tmp);
            }

            for(String tmp:setExchange){
                /*exchange.remove(tmp).close();
                timeStampExchange.remove(tmp);*/
                close(tmp);
            }

            if(connection!=null) {
                connection.close();
            }
            connected.set(false);
        }catch(IOException e){
            e.printStackTrace();
        }

        lock.unlock();
    }

    public synchronized void close(String name){

        lock.lock();
        try {
            if (queue.containsKey(name)) {
                queue.remove(name).close();
                timeStampQueue.remove(name);
            } else if (exchange.containsKey(name)) {
                exchange.remove(name).close();
                timeStampExchange.remove(name);
            }
        }catch(IOException | TimeoutException e){
            e.printStackTrace();
        }

        lock.unlock();
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

    private void addListener(final MessageListener listener,Channel channel,final String name){

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {

                    Long millis=-1L;

                    lock.lock();

                    if(timeStampQueue.containsKey(name)){
                        millis=timeStampQueue.get(name);
                    }else if(timeStampExchange.containsKey(name)){
                        millis=timeStampExchange.get(name);
                    }

                    lock.unlock();

                    JSONObject message=new JSONObject(new String(body,"UTF-8"));

                    if(message.getLong("timestamp")>=millis) {
                        listener.handleMessage(consumerTag, envelope, properties,message);
                    }
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
