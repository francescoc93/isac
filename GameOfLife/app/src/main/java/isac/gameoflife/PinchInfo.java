package isac.gameoflife;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by luca on 13/03/17.
 */

public class PinchInfo implements Serializable {

    public final static String ADDRESS="address";
    public final static String X_COORDINATE="xcoordinate";
    public final static String Y_COORDINATE="ycoordinate";
    public final static String TIMESTAMP="timestamp";
    public final static String PORTRAIT="portrait";
    public final static String SCREEN_WIDTH="screenWidth";
    public final static String SCREEN_HEIGHT="screenHeight";
    public final static String CONNECTED_DEVICE="connectedDevice";
    private String address;
    private Integer xcoordinate;
    private Integer ycoordinate;
    private boolean portrait;
    private Long timestamp;
    private int screenWidth;
    private int screenHeight;
    private int connectedDevice;

    public PinchInfo(String address, Integer xcoordinate, Integer ycoordinate, boolean portrait, Long timestamp, int screenWidth, int screenHeight,int connectedDevice) {
        this.address = address;
        this.xcoordinate = xcoordinate;
        this.ycoordinate = ycoordinate;
        this.portrait = portrait;
        this.timestamp = timestamp;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.connectedDevice=connectedDevice;
    }

    public String getAddress() {
        return address;
    }

    public Integer getXcoordinate() {
        return xcoordinate;
    }

    public Integer getYcoordinate() {
        return ycoordinate;
    }

    public boolean isPortrait() {
        return portrait;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getConnectedDevice() {
        return connectedDevice;
    }

    public JSONObject toJSON() {

        JSONObject jo = new JSONObject();
        try {
            jo.put(ADDRESS, getAddress());
            jo.put(X_COORDINATE, getXcoordinate());
            jo.put(Y_COORDINATE, getYcoordinate());
            jo.put(TIMESTAMP, getTimestamp());
            jo.put(PORTRAIT, isPortrait());
            jo.put(SCREEN_WIDTH, getScreenWidth());
            jo.put(SCREEN_HEIGHT, getScreenHeight());
            jo.put(CONNECTED_DEVICE,getConnectedDevice());
            jo.put("type","pinch");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo;
    }
}
