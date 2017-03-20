package isac.gameoflife;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Francesco on 14/03/2017.
 */

public class Utils {

    private static String ipAddress=null;

    private Utils(){
    }

    public static String getAddress(){
        File docsFolder = new File(Environment.getExternalStorageDirectory() + "/GameOfLife");

        if(!docsFolder.exists()) {
            docsFolder.mkdir();
        }

        File file=new File(docsFolder.getAbsolutePath(),"address.txt");

        if(!file.exists()){

            try {
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file/*, Context.MODE_PRIVATE*/);
                outputStream.write(("192.168.1.105").getBytes());
                outputStream.flush();
                outputStream.close();

                return "192.168.1.105";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String ret = "";

        try {
            FileInputStream inputStream=new FileInputStream(file);

            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString = bufferedReader.readLine();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(receiveString);
            inputStream.close();
            ret=stringBuilder.toString();
        }catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static String getIpAddress() {
        if (ipAddress == null) {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress()) {
                            String sAddr = addr.getHostAddress();
                            //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                            boolean isIPv4 = sAddr.indexOf(':') < 0;
                            ipAddress = sAddr;
                           /* if (useIPv4) {
                                if (isIPv4)
                                    return sAddr;
                            } else {
                                if (!isIPv4) {
                                    int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                    return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                                }
                            }*/
                        }
                    }
                }
            } catch (Exception ex) {
            }
            /*try {
                Enumeration en = NetworkInterface.getNetworkInterfaces();
                InetAddress ia=null;
                while(en.hasMoreElements()){
                    NetworkInterface ni=(NetworkInterface) en.nextElement();
                    Enumeration ee = ni.getInetAddresses();

                    while(ee.hasMoreElements()) {
                        ia= (InetAddress) ee.nextElement();
                    }
                }

                ipAddress=ia.getHostAddress();
            } catch (SocketException e) {
                e.printStackTrace();
            }

        }*/


        }
        System.out.println(ipAddress);
        return ipAddress;
    }
}
