package isac.gameoflife;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

    private int orientation,x,y;
    private String nameQueueSender,nameQueueReceiver;

    public ConnectedDeviceInfo(int orientation, int x, int y, String nameQueueSender, String nameQueueReceiver){
        this.orientation=orientation;
        this.x=x;
        this.y=y;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
    }


    public int getOrientation() {
        return orientation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getNameQueueSender() {
        return nameQueueSender;
    }

    public String getNameQueueReceiver() {
        return nameQueueReceiver;
    }
}
