package isac.gameoflife;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

    private boolean portrait;
    private int x,y;
    private String nameQueueSender,nameQueueReceiver;

    public ConnectedDeviceInfo(boolean portrait, int x, int y, String nameQueueSender, String nameQueueReceiver){
        this.portrait = portrait;
        this.x=x;
        this.y=y;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
    }


    public boolean isPortrait() {
        return portrait;
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
