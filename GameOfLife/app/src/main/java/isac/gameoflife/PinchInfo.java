package isac.gameoflife;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * Created by luca on 13/03/17.
 */

public class PinchInfo implements Serializable {


    public enum Direction implements Serializable{
        UP,DOWN,LEFT,RIGHT
    }

    public final static String ADDRESS="address";
    public final static String DIRECTION="direction";
    public final static String X_COORDINATE="xcoordinate";
    public final static String Y_COORDINATE="ycoordinate";
    public final static String TIMESTAMP="timestamp";
    public final static String SCREEN_WIDTH="screenWidth";
    public final static String SCREEN_HEIGHT="screenHeight";
    public final static String XDPI="xdpi";
    public final static String YDPI="ydpi";
    private Direction direction;
    private String address;
    private Integer xcoordinate;
    private Integer ycoordinate;
    private Long timestamp;
    private float screenWidth;
    private float screenHeight;
    private float myXDpi, myYDpi;

    public PinchInfo(String address, Direction direction,Integer xcoordinate, Integer ycoordinate, Long timestamp, float screenWidth,
                     float screenHeight,float xDpi, float yDpi) {
        this.address = address;
        this.xcoordinate = xcoordinate;
        this.ycoordinate = ycoordinate;
        this.timestamp = timestamp;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.direction=direction;
        this.myXDpi = xDpi;
        this.myYDpi = yDpi;

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

    public Long getTimestamp() {
        return timestamp;
    }

    public float getScreenWidth() {
        return screenWidth;
    }

    public float getScreenHeight() {
        return screenHeight;
    }

    public Direction getDirection(){
        return direction;
    }

    public float getXDpi() {return this.myXDpi; }

    public float getYDpi() {return this.myYDpi; }

    public boolean oppositeDirection(Direction direction){
        switch (direction){
            case UP:return this.direction==Direction.DOWN;
            case DOWN:return this.direction==Direction.UP;
            case LEFT:return this.direction==Direction.RIGHT;
            case RIGHT:return this.direction==Direction.LEFT;
            default:return false;
        }
    }

    public JSONObject toJSON() {

        JSONObject jo = new JSONObject();
        try {
            jo.put(ADDRESS, getAddress());
            jo.put(DIRECTION,getDirection());
            jo.put(X_COORDINATE, getXcoordinate());
            jo.put(Y_COORDINATE, getYcoordinate());
            jo.put(TIMESTAMP, getTimestamp());
            jo.put(SCREEN_WIDTH, getScreenWidth());
            jo.put(SCREEN_HEIGHT, getScreenHeight());
            jo.put(XDPI, getXDpi() );
            jo.put(YDPI, getYDpi() );
            jo.put("type","pinch");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo;
    }
}
