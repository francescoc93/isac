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

    /**
     *
     * @param address server's IP address
     * @param username
     * @param password
     */
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

    /**
     * Establishes a connection with the server
     * @return if the connection has established
     */
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

    /**
     * Adds a queue
     * @param name queue's name
     * @return true if the queue was successfully added or it already exists. False otherwise
     */
    public boolean addQueue(String name){

        lock.lock();
        if(!queue.containsKey(name) && !exchange.containsKey(name)){
            try {
                Channel tmp=createChannel();
                tmp.queueDeclare(name, false, false, false, null);
                queue.put(name,tmp);
                timeStampQueue.put(name,System.currentTimeMillis());
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                lock.unlock();
                return true;
            }


        }else if(queue.containsKey(name)){
            lock.unlock();
            return true;
        }

        lock.unlock();
        return false;
    }

    /**
     * Adds a queue with listener for incoming messages
     *
     * @param name queue's name
     * @param listener a MessageListener object.
     * @return true if the queue was successfully or it already exists. False otherwise
     */
    public boolean addQueue(String name, final MessageListener listener){
        if(addQueue(name)) {
            lock.lock();
            addListener(listener, queue.get(name), name,name);
            lock.unlock();
            return true;
        }

        return false;
    }

    /**
     *
     * Adds an exchange to broadcast the messages
     *
     * @param name exchange's name
     * @param mode working mode of the exchange (direct, topic, headers and fanout)
     * @return true if the exchange was successfully added or it already exists. False otherwise
     */
    public boolean addPublishExchange(String name,String mode){

        lock.lock();
        if(!exchange.containsKey(name) && !queue.containsKey(name)){
            try {
                Channel tmp=createChannel();
                tmp.exchangeDeclare(name,mode);
                exchange.put(name,tmp);
                timeStampExchange.put(name,System.currentTimeMillis());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                return true;
            }
        }else if(exchange.containsKey(name)){
            lock.unlock();
            return true;
        }

        lock.unlock();
        return false;
    }

    /**
     *
     * Binds a queue to an exchange
     *
     * @param name exchange's name
     * @param mode working mode of the exchange (direct, topic, headers and fanout)
     * @param listener a MessageListener object.
     * @return true if the queue was successfully bind or it already exists. False otherwise
     */
    public boolean addSubscribeQueue(String name,String mode,MessageListener listener){
        if(addPublishExchange(name,mode)) {
            try {
                lock.lock();
                Channel tmp = exchange.get(name);
                String queueName = tmp.queueDeclare().getQueue();
                tmp.queueBind(queueName, name, "");
                addListener(listener, tmp, queueName,name);
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                lock.unlock();
                return true;
            }
        }

        return false;
    }

    /**
     *
     * Sends a message
     *
     * @param name name of the exchange or the queue
     * @param message message to be sent
     */
    public void sendMessage(String name,JSONObject message){
        try {
            message.put("timestampMessage",System.currentTimeMillis());

            lock.lock();
            if (queue.containsKey(name)) {
                queue.get(name).basicPublish("", name, null, message.toString().getBytes());
            } else if (exchange.containsKey(name)) {
                exchange.get(name).basicPublish(name, "", null, message.toString().getBytes());
            }

        }catch (IOException | JSONException e){
            e.printStackTrace();
        }finally{
            lock.unlock();
        }
    }

    /**
     *
     * Closes connection with the RabbitMQ's server. If there's any channel opened,
     * it will be closed
     *
     */
    public void closeConnection() {
        lock.lock();
        Set<String> setQueue = queue.keySet();
        Set<String> setExchange = exchange.keySet();

        try {
            for(String tmp : setQueue) {
                close(tmp);
            }

            for(String tmp:setExchange){
                close(tmp);
            }

            if(connection!=null) {
                connection.close();
            }
            connected.set(false);
        }catch(IOException e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    /**
     *
     * Closes the channel
     *
     * @param name name of the exchange or the queue
     */
    public void close(String name){

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
        }finally {
            lock.unlock();
        }
    }

    /**
     *
     * @return True if the device is connected to the server. False otherwise
     */
    public boolean isConnected(){
        return connected.get();
    }

    /**
     * Creates a new channel
     * @return the channel that was created
     */
    private Channel createChannel(){
        try {
            return connection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     *
     * Adds a listener for incoming messages from the server
     *
     * @param listener a MessageListener object
     * @param channel channel where to add the listener to
     * @param name name of the queue or the exchange
     */
    private void addListener(final MessageListener listener,Channel channel,String name,final String key){

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                try {

                    Long millis=Long.MAX_VALUE;

                    lock.lock();

                    if(timeStampQueue.containsKey(key)){
                        millis=timeStampQueue.get(key);
                    }else if(timeStampExchange.containsKey(key)){
                        millis=timeStampExchange.get(key);
                    }

                    lock.unlock();

                    JSONObject message=new JSONObject(new String(body,"UTF-8"));

                    //check if the message was sent before channel creation
                    if(message.getLong("timestampMessage")>=millis) {
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
