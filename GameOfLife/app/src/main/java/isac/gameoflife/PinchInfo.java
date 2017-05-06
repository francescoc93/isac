package isac.gameoflife;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

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

    /**
     *
     * @param address device's IP address
     * @param direction swipe direction
     * @param xcoordinate coordinate of X axis when the swipe is ended
     * @param ycoordinate coordinate of Y axis when the swipe is ended
     * @param timestamp time of when the swipe is occurred
     * @param screenWidth width of the grid in inches
     * @param screenHeight height of the grid in inches
     * @param xDpi the exact physical pixels per inch of the screen in the X dimension
     * @param yDpi the exact physical pixels per inch of the screen in the Y dimension
     */
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

    /**
     *
     * @return device's IP address
     */
    public String getAddress() {
        return address;
    }

    /**
     *
     * @return coordinate of X axis when the swipe is ended
     */
    public Integer getXcoordinate() {
        return xcoordinate;
    }

    /**
     *
     * @return coordinate of Y axis when the swipe is ended
     */
    public Integer getYcoordinate() {
        return ycoordinate;
    }

    /**
     *
     * @return time of when the swipe is occurred
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     *
     * @return width of the screen in inches
     */
    public float getScreenWidth() {
        return screenWidth;
    }

    /**
     *
     * @return height of the screen in inches
     */
    public float getScreenHeight() {
        return screenHeight;
    }

    /**
     *
     * @return swipe direction (UP, DOWN, LEFT, RIGHT)
     */
    public Direction getDirection(){
        return direction;
    }

    /**
     *
     * @return the exact physical pixels per inch of the screen in the X dimension
     */
    public float getXDpi() {return this.myXDpi; }

    /**
     *
     * @return the exact physical pixels per inch of the screen in the Y dimension
     */
    public float getYDpi() {return this.myYDpi; }

    /**
     *
     * @return JSON of all parameters
     */
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
