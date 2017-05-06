package isac.gameoflife;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONObject;

/**
 * Created by Francesco on 13/03/2017.
 */

public interface MessageListener {

    /**
     *
     * Callback for handle the incoming message from RabbitMQ's server
     *
     * @param consumerTag
     * @param envelope
     * @param properties
     * @param json incoming message
     */
    void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json);
}
