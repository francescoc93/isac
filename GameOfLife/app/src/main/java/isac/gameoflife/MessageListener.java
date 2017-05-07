package isac.gameoflife;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONObject;


public interface MessageListener {

    /**
     *
     * Callback to handle the incoming messages from RabbitMQ's server
     *
     * @param consumerTag
     * @param envelope
     * @param properties
     * @param json incoming message
     */
    void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json);
}
