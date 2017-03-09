package isac.gameoflife;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Created by Francesco on 09/03/2017.
 */

public class ServiceRegistrationDNS/* extends AsyncTask<Void,Void,Void>*/ {


    public ServiceRegistrationDNS(String name){
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.registerService( ServiceInfo.create("_http._tcp.local.", name, 1234, ""));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   /* @Override
    protected Void doInBackground(Void... params) {

        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", name, 1234, "");
            jmdns.registerService(serviceInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }*/
}
