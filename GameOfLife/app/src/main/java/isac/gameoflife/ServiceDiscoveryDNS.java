package isac.gameoflife;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * Created by Francesco on 09/03/2017.
 */

public class ServiceDiscoveryDNS extends AsyncTask<Void,Void,Void>{

    private JmDNS jmdns;
    private ServiceListener listener;
   // private InetAddress address;
    private Object lock;

    public  ServiceDiscoveryDNS(/*InetAddress address,*/ServiceListener listener){
        //this.address=address;
        this.listener=listener;
        lock=new Object();
    }

    public void close(){
        synchronized (lock) {
            if (jmdns != null) {
                try {
                    jmdns.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params) {

        try {
            synchronized (lock) {
            System.out.println("CIAOO");
            Enumeration en = NetworkInterface.getNetworkInterfaces();
            InetAddress ia=null;
            while(en.hasMoreElements()){
                NetworkInterface ni=(NetworkInterface) en.nextElement();
                Enumeration ee = ni.getInetAddresses();

                while(ee.hasMoreElements()) {
                    ia= (InetAddress) ee.nextElement();
                    //System.out.println(ia.getHostAddress());
                }
            }
            System.out.println(ia.getHostAddress());
            System.out.println("INDIRIZZO: "+InetAddress.getLocalHost().getHostAddress());
                jmdns = JmDNS.create(/*address*/ia/*InetAddress.getLocalHost()*/);
                jmdns.addServiceListener("_http._tcp.local.", listener);
       //     Thread.sleep(40000);
            System.out.println("CIAOO");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
