package isac.gameoflife;

import android.os.AsyncTask;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Francesco on 11/03/2017.
 */

public class RabbitMQ {

    private BlockingDeque<JSONObject> queue;
    private ConnectionFactory factory;

    public RabbitMQ(){
        try {
            queue = new LinkedBlockingDeque<>();
            factory=new ConnectionFactory();
            factory.setAutomaticRecoveryEnabled(false);
            factory.setUri("");
            new PublishMessage().execute();
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    public void publishMessage(JSONObject message) {
        try {
            queue.putLast(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class PublishMessage extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {

            while(true) {
                try {
                    Connection connection = factory.newConnection();
                    Channel ch = connection.createChannel();
                    ch.confirmSelect();

                    while (true) {
                        JSONObject message = queue.takeFirst();
                        try{
                            ch.basicPublish("amq.fanout", "GameOfLife", null, message.toString().getBytes());
                            ch.waitForConfirmsOrDie();
                        } catch (Exception e){
                            queue.putFirst(message);
                            throw e;
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(5000); //sleep and then try again
                    } catch (InterruptedException e1) {
                        break;
                    }
                }
            }

            return null;
        }
    }
}
