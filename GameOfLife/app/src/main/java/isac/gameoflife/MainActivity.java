package isac.gameoflife;

import android.app.SearchManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class MainActivity extends AppCompatActivity {

    private static boolean firstTime=true;
    private GridView gridView;
    private boolean portrait = true;
    private RabbitMQ rabbitMQ;
    private InetAddress selfAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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

            new AsyncTask<Void,Void,Void>(){

                @Override
                protected Void doInBackground(Void... params) {
                    rabbitMQ=new RabbitMQ("192.168.1.102","[user]","[user]");
                    rabbitMQ.addPublishExchange("broadcast","fanout");
                    rabbitMQ.addSubscribeQueue("broadcast","fanout", new MessageListener() {
                        @Override
                        public void handleMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, JSONObject json) {
                            System.out.println("Messaggio broadcast ricevuto");
                            try {
                                PinchInfo info = new PinchInfo(json.getString("address"),json.getInt("xcoordinate"),json.getInt("ycoordinate"),
                                        json.getBoolean("portrait"),json.getLong("timestamp"),json.getInt("screenWidth"),json.getInt("screenHeight"));
                                if (info.getTimestamp() > gridView.getTimeStamp() - 20 && info.getTimestamp() < gridView.getTimeStamp() + 20  ){
                                    System.out.println("DEVICE PAIRED WITH " + info.getAddress());
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }



                    });

                    return null;
                }

            }.execute();


            gridView=new GridView(this,rabbitMQ);
            setContentView(gridView);
            gridView.setActivity(this);

            try {
                this.setSelfIpAddress();
            } catch (SocketException e) {
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

    private void setSelfIpAddress() throws SocketException {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        InetAddress ia=null;
        while(en.hasMoreElements()){
            NetworkInterface ni=(NetworkInterface) en.nextElement();
            Enumeration ee = ni.getInetAddresses();

            while(ee.hasMoreElements()) {
                ia= (InetAddress) ee.nextElement();
                selfAddress = ia;
                //System.out.println(ia.getHostAddress());
            }
        }


    }

    public String getSelfIpAdress(){
        return this.selfAddress.getHostAddress();
    }
}
