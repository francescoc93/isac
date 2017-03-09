package isac.gameoflife;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * Created by Francesco on 09/03/2017.
 */

public class ServiceDiscoveryDNS {

    private JmDNS jmdns;

    public void startDiscovery(ServiceListener listener){
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.addServiceListener("_http._tcp.local.", listener);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void close(){
        if(jmdns!=null){
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
