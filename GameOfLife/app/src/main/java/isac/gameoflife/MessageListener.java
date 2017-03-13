package isac.gameoflife;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONObject;

/**
 * Created by Francesco on 13/03/2017.
 */

public interface MessageListener {

    void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json);
}
