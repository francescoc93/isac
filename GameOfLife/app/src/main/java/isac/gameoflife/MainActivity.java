package isac.gameoflife;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class MainActivity extends AppCompatActivity {

    private static boolean firstTime=true;
    private GridView gridView;
    private boolean portrait = true;
    private static final String EXCHANGE_NAME = "broadcast";
    private Channel channel;
    Connection connection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gridView=new GridView(this);
        setContentView(gridView);

        System.out.println("Game Of Life");

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay();
        int orientation = display.getRotation();
        if (orientation == Surface.ROTATION_90
                || orientation == Surface.ROTATION_270) {
            ///Landscape
            this.portrait = false;
        }
        if(firstTime) {
            firstTime=false;

            //WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            // get the device ip address
            try {

               // final InetAddress deviceIpAddress = null;//InetAddress.getByAddress(BigInteger.valueOf(wifi.getConnectionInfo().getIpAddress()).toByteArray());
              /*  WifiManager.MulticastLock multicastLock = wifi.createMulticastLock(getClass().getName());
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();*/


//System.out.println("INDIRIZZO: "+deviceIpAddress.getHostAddress());
                new ServiceRegistrationDNS("gameOfLife"/*,deviceIpAddress*/).execute();
                new ServiceDiscoveryDNS(/*deviceIpAddress,*/new ServiceListener() {

                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        System.out.println("Service added: " + event.getInfo());
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {
                        System.out.println("Service removed: " + event.getInfo());
                    }

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        System.out.println("Service resolved: " + event.getInfo());
                    }
                }).execute();

            } catch (/*UnknownHost*/Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isPortrait(){
        return this.portrait;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_start:gridView.start();break;
            case R.id.action_pause:gridView.pause();break;
            case R.id.action_clean:gridView.clear();break;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Utility and connection private methods
     */

    private void createFanout() throws java.io.IOException,java.util.concurrent.TimeoutException{
         ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

    }

    private void bindQueueToChannel()throws java.io.IOException {
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
            }
        };
        channel.basicConsume(queueName, true, consumer);
    }

    private void publishFanout(byte[] body) throws java.io.IOException{
        channel.basicPublish(EXCHANGE_NAME, "", null, body);

    }

    private void closeFanout() throws java.io.IOException,java.util.concurrent.TimeoutException{
        channel.close();
        connection.close();

    }
}
