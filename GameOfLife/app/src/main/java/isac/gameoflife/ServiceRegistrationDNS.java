package isac.gameoflife;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

/**
 * Created by Francesco on 09/03/2017.
 */

public class ServiceRegistrationDNS extends AsyncTask<Void,Void,Void> {

    private String name;
    //private InetAddress address;

    public ServiceRegistrationDNS(String name/*,InetAddress address*/){
       // this.address=address;
        this.name=name;
    }

    @Override
    protected Void doInBackground(Void... params) {

        try {

            Enumeration en = NetworkInterface.getNetworkInterfaces();
            InetAddress ia=null;
            while(en.hasMoreElements()){
                NetworkInterface ni=(NetworkInterface) en.nextElement();
                Enumeration ee = ni.getInetAddresses();

                while(ee.hasMoreElements()) {
                    ia= (InetAddress) ee.nextElement();
                   // System.out.println(ia.getHostAddress());
                }
            }
            System.out.println(ia.getHostAddress());
            JmDNS jmdns = JmDNS.create(/*address*/ia/*InetAddress.getLocalHost()*/);
            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", name, 8080, "");
            jmdns.registerService(serviceInfo);

           // Thread.sleep(40000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
