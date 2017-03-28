package isac.gameoflife;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

    private boolean reverseList;
    private boolean portrait;
    private String nameQueueSender,nameQueueReceiver;
    private int orientation; //gradi di rotazione
    private int myWidth, myHeight, width, height,myXCoord,myYCoord,xCoord,yCoord;//TODO: SET MYWIDTH E MYHEIGHT
    private PinchInfo.Direction myDir,dir; //TODO: SET MYDIR
    private float cellSize;
    private int l1, l2, indexFirstCell, indexLastCell;
    private List<Boolean> cellsToSend;

    public ConnectedDeviceInfo(float cellSize,boolean portrait, int xCoord, int yCoord,int width,int height,int myXCoord,int myYCoord, String nameQueueSender, String nameQueueReceiver){
        this.portrait = portrait;
        this.xCoord=xCoord;
        this.yCoord=yCoord;
        this.myYCoord = myYCoord;
        this.myXCoord = myXCoord;
        this.width = width;
        this.height = height;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
        this.cellSize = cellSize;
        this.cellsToSend = new ArrayList<>(); //TEMPORARY
        this.reverseList = false;
    }


    public boolean isPortrait() {
        return portrait;
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
    public void setRelativeOrientation(){

        //my X max
        if(myXCoord > myWidth - 20 && myXCoord <= myWidth){

            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 0;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 180;
            } else if(this.yCoord >= 0 && this.height < 20){
                this.orientation = 90;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
                this.orientation = 270;
            }

        } else if(myXCoord >= 0 && myXCoord < 20){ //my x min

            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 270;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 0;
            } else if(this.yCoord >= 0 && this.yCoord < 20){
                this.orientation = 180;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
                this.orientation = 90;
            }



        } else if (myYCoord > myHeight - 20 && myYCoord <= myHeight){ //my y max

            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 270;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 90;
            } else if(this.yCoord >= 0 && this.yCoord < 20){
                this.orientation = 0;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
                this.orientation = 180;
            }



        } else if(myYCoord >= 0 && myYCoord < 20){ //my y min

            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 90;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 270;
            } else if(this.yCoord >= 0 && this.yCoord < 20){
                this.orientation = 180;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
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
            indexLastCell = (int)((myYCoord + l2)/this.cellSize);
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
            indexLastCell = (int) ((myXCoord + l2)/this.cellSize);
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
