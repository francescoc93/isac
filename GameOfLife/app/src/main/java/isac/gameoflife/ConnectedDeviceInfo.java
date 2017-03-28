package isac.gameoflife;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

    private boolean reverseList;
    private int x,y;
    private boolean portrait;
    private int swipeX, swipeY,maxX,maxY;
    private int mySwipeX,mySwipeY;
    private String nameQueueSender,nameQueueReceiver;
    private int orientation; //gradi di rotazione
    private int myWidth, myHeight, width, height,myXCoord,myYCoord,xCoord,yCoord;//x e y mie e sue, height e width mie e sue
    private PinchInfo.Direction myDir,dir;
    private int l1, l2, cellSize, indexFirstCell, indexLastCell;
    private List<Boolean> cellsToSend;

    public ConnectedDeviceInfo(int cellSize,boolean portrait, int x, int y,int maxX,int maxY,int mySwipeX,int mySwipeY, String nameQueueSender, String nameQueueReceiver){
        this.portrait = portrait;
        this.x=x;
        this.y=y;
        this.swipeX =x;
        this.swipeY =y;
        this.maxX = maxX;
        this.maxY = maxY;
        this.mySwipeX = mySwipeX;
        this.mySwipeY = mySwipeY;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
        this.cellSize = cellSize;
        this.cellsToSend = new ArrayList<>(); //TEMPORARY
        this.reverseList = false;
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


    /**
     * The orientation is calculated considering yourself(the device) as in the middle of the origin
     * with its center (0,0). We have then 16 cases, 4 for each border(4).
     */
    public void setRelativeOrientation(int myMaxX, int myMaxY){

        //my X max
        if(mySwipeX > myMaxX - 20 && mySwipeX <= myMaxX){

            if(this.swipeX >= 0 && this.swipeX < 20){
                this.orientation = 0;
            } else if(this.swipeX <= this.maxX && this.swipeX > this.maxX - 20){
                this.orientation = 180;
            } else if(this.swipeY >= 0 && this.swipeY < 20){
                this.orientation = 90;
            } else if(this.swipeY <= this.maxY && this.swipeY > this.maxY - 20){
                this.orientation = 270;
            }

        } else if(mySwipeX >= 0 && mySwipeX < 20){ //my x min

            if(this.swipeX >= 0 && this.swipeX < 20){
                this.orientation = 270;
            } else if(this.swipeX <= this.maxX && this.swipeX > this.maxX - 20){
                this.orientation = 0;
            } else if(this.swipeY >= 0 && this.swipeY < 20){
                this.orientation = 180;
            } else if(this.swipeY <= this.maxY && this.swipeY > this.maxY - 20){
                this.orientation = 90;
            }



        } else if (mySwipeY > myMaxY - 20 && mySwipeY <= myMaxY){ //my y max

            if(this.swipeX >= 0 && this.swipeX < 20){
                this.orientation = 270;
            } else if(this.swipeX <= this.maxX && this.swipeX > this.maxX - 20){
                this.orientation = 90;
            } else if(this.swipeY >= 0 && this.swipeY < 20){
                this.orientation = 0;
            } else if(this.swipeY <= this.maxY && this.swipeY > this.maxY - 20){
                this.orientation = 180;
            }



        } else if(mySwipeY >= 0 && mySwipeY < 20){ //my y min

            if(this.swipeX >= 0 && this.swipeX < 20){
                this.orientation = 90;
            } else if(this.swipeX <= this.maxX && this.swipeX > this.maxX - 20){
                this.orientation = 270;
            } else if(this.swipeY >= 0 && this.swipeY < 20){
                this.orientation = 180;
            } else if(this.swipeY <= this.maxY && this.swipeY > this.maxY - 20){
                this.orientation = 0;
            }



        }


    }
    //NB: I CASI SI RIFERISCONO SEMPRE ALLA POSIZIONE DELL'ALTRO DEVICE
    //SI DA' PER SCONTATO CHE I 16 CASI SONO GIA' STATI CALCOLATI - TODO: INSERIRE CODICE
    //TODO: PORZIONE DI SCHERMO DA RICEVERE E INVIARE

    //calcolo lunghezze ---> punto inizio (altezza inizio)/grandezza celle = indice cella iniziale da inviare
    //(punto inizio + somma lunghezze che indicano la parte di schermo in contatto)/grandezza cella = indice cella finale da inviare

    /**
     * After evaluating the orientation of the device, this method calculates the lengths of the parts splitted by the swipe points
     */
    private void calculateL1L2(){ //16 casi (TRIMMED TO 8)

        //SECOND: CALCULATE L1 AND L2 (length of the portions of screen before and after the swipe point)
        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){
            if(orientation == 0){
                this.l1 = Math.min(myYCoord,yCoord);
                this.l2 = Math.min((myHeight-myYCoord), (height - yCoord));
            } else if (orientation == 90){
                this.l1 = Math.min(myYCoord, (width-xCoord));
                this.l2 = Math.min((myHeight-myYCoord), xCoord);
            } else if (orientation == 180){
                this.l1 = Math.min(myYCoord,(height-yCoord));
                this.l2 = Math.min((myHeight-myYCoord), yCoord);
            } else if (orientation == 270){
                this.l1 = Math.min(myYCoord,xCoord);
                this.l2 = Math.min((myHeight-myYCoord),(width-xCoord));
            }
        } else if (myDir.equals(PinchInfo.Direction.UP) || myDir.equals(PinchInfo.Direction.DOWN)){
            if(orientation == 0){
                this.l1 = Math.min(myXCoord, xCoord);
                this.l2 = Math.min((myWidth-myXCoord),(width-xCoord));
            } else if (orientation == 90){
                this.l1 = Math.min(myXCoord,yCoord );
                this.l2 = Math.min((myWidth-myXCoord),(height-yCoord));
            } else if (orientation == 180){
                this.l1 = Math.min(myXCoord, (width-xCoord));
                this.l2 = Math.min((myWidth-myXCoord),xCoord);
            } else if (orientation == 270){
                this.l1 = Math.min(myXCoord, (height-yCoord));
                this.l2 = Math.min((myWidth-myXCoord),yCoord);
            }
        }

    }

    //THIRD: calculate the cells to send and the ones we expect to receive. The number of these two groups of cells is of course the same.
    private void evaluateCells(){
        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){
            indexFirstCell =(int) Math.ceil((double)(myYCoord - l1)/(double)this.cellSize);
            indexLastCell = (myYCoord + l2)/this.cellSize;
            if(orientation == 0){
                this.reverseList = false;
            } else if (orientation == 90){
                this.reverseList = true;
            } else if (orientation == 180){
                this.reverseList = true;
            } else if (orientation == 270){
                this.reverseList = false;
            }
        } else if (myDir.equals(PinchInfo.Direction.UP) || myDir.equals(PinchInfo.Direction.DOWN)){
            indexFirstCell =(int) Math.ceil((double)(myXCoord - l1)/(double)this.cellSize);
            indexLastCell = (myXCoord + l2)/this.cellSize;
            if(orientation == 0){
                this.reverseList = false;

            } else if (orientation == 90){
                this.reverseList = false;

            } else if (orientation == 180){
                this.reverseList = true;

            } else if (orientation == 270){
                this.reverseList = true;
            }
        }
    }

}
