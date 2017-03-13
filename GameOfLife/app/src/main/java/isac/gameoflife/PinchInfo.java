package isac.gameoflife;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by luca on 13/03/17.
 */

public class PinchInfo implements Serializable {

    private String address;
    private Integer xcoordinate;
    private Integer ycoordinate;
    private boolean portrait;
    private Long timestamp;
    private int screenWidth;
    private int screenHeight;

    public PinchInfo(String address, Integer xcoordinate, Integer ycoordinate, boolean portrait, Long timestamp, int screenWidth, int screenHeight) {
        this.address = address;
        this.xcoordinate = xcoordinate;
        this.ycoordinate = ycoordinate;
        this.portrait = portrait;
        this.timestamp = timestamp;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
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

    public JSONObject toJSON() {

        JSONObject jo = new JSONObject();
        try {
            jo.put("address", getAddress());
            jo.put("xcoordinate", getXcoordinate());
            jo.put("ycoordinate", getYcoordinate());
            jo.put("timestamp", getTimestamp());
            jo.put("portrait", isPortrait());
            jo.put("screenWidth", getScreenWidth());
            jo.put("screenHeight", getScreenHeight());
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return jo;
    }
}
