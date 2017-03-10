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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class MainActivity extends AppCompatActivity {

    private static boolean firstTime=true;
    private GridView gridView;
    private boolean portrait = true;


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
}
