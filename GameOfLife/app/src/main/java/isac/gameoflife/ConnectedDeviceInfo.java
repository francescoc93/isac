package isac.gameoflife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

    //TODO: SIZE IN POLLICI PER FARE LA DIVISIONE, POLLICI = SIZE/50 (PIXEL/DPI)
    private boolean reverseList;
    private String nameQueueSender,nameQueueReceiver;
    private int orientation; //gradi di rotazione
    private float myWidth, myHeight, width, height,myXCoord,myYCoord,xCoord,yCoord;
    private PinchInfo.Direction myDir,dir;
    private float cellSize;
    private int indexFirstCell, indexLastCell;
    private float l1,l2;
    private List<Boolean> cellsToSend;
    private GridView gridView;
    private String direction;
    private float scale;

    public ConnectedDeviceInfo(float cellSize, PinchInfo.Direction dir, PinchInfo.Direction myDir,
                               int xCoord, int yCoord, int width, int height, int myWidth, int myHeight,
                               int myXCoord, int myYCoord, String nameQueueSender, String nameQueueReceiver0,GridView gridView,
                               float xdpi,float ydpi){
        this.scale = gridView.getScale();
        this.xCoord=Utils.pixelsToInches(xCoord,xdpi);
        this.yCoord=Utils.pixelsToInches(yCoord,ydpi);
        this.myYCoord = Utils.pixelsToInches(myYCoord,gridView.getYDpi());
        this.myXCoord = Utils.pixelsToInches(myXCoord,gridView.getXDpi());
        this.width = Utils.pixelsToInches(width,xdpi);
        this.height = Utils.pixelsToInches(height,ydpi);
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
        this.cellSize = 50/*cellSize*/;
        this.cellsToSend = new ArrayList<>();
        this.reverseList = false;
        System.out.println("MIA ALTEZZA COSTRUTTORE " + myHeight);
        this.myWidth = Utils.pixelsToInches(myWidth,gridView.getXDpi());
        this.myHeight = Utils.pixelsToInches(myHeight,gridView.getYDpi());
        this.myDir = myDir;
        this.dir = dir;
        this.gridView = gridView;
    }

    public String getNameQueueSender() {
        return nameQueueSender;
    }

    public String getNameQueueReceiver() {
        return nameQueueReceiver;
    }

    public PinchInfo.Direction getMyDirection(){
        return this.myDir;
    }

    public void calculateInfo(){
        setRelativeOrientation();
        calculateL1L2();
        evaluateCells();
    }
    /**
     * The orientation is calculated considering yourself(the device) as in the middle of the origin
     * with its center (0,0). We have then 16 cases, 4 for each border(4).
     */
    private void setRelativeOrientation(){

        //RIGHT
        if(myXCoord > myWidth - 20 && myXCoord <= myWidth){
            this.direction = "right";

            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 0;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 180;
            } else if(this.yCoord >= 0 && this.height < 20){
                this.orientation = 90;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
                this.orientation = 270;
            }

        } else if(myXCoord >= 0 && myXCoord < 20){ //LEFT

            this.direction = "left";
            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 270;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 0;
            } else if(this.yCoord >= 0 && this.yCoord < 20){
                this.orientation = 180;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
                this.orientation = 90;
            }

        } else if (myYCoord > myHeight - 20 && myYCoord <= myHeight){ //TOP
            this.direction = "top";

            if(this.xCoord >= 0 && this.xCoord < 20){
                this.orientation = 270;
            } else if(this.xCoord <= this.width && this.xCoord > this.width - 20){
                this.orientation = 90;
            } else if(this.yCoord >= 0 && this.yCoord < 20){
                this.orientation = 0;
            } else if(this.yCoord <= this.height && this.yCoord > this.height - 20){
                this.orientation = 180;
            }

        } else if(myYCoord >= 0 && myYCoord < 20){ //BOTTOM
            this.direction = "bottom";

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

        System.out.println("ORIENTAMENTO: " + this.orientation);

    }
    //NB: I CASI SI RIFERISCONO SEMPRE ALLA POSIZIONE DELL'ALTRO DEVICE

    //calcolo lunghezze ---> punto inizio (altezza inizio)/grandezza celle = indice cella iniziale da inviare
    //(punto inizio + somma lunghezze che indicano la parte di schermo in contatto)/grandezza cella = indice cella finale da inviare

    /**
     * After evaluating the orientation of the device, this method calculates the lengths of the parts splitted by the swipe points
     */
    private void calculateL1L2(){ //16 casi (TRIMMED TO 8)

        System.out.println("MIA COORDINATA Y " + myYCoord + "SUA COORDINATA Y " +yCoord + "MIA ALTEZZA " +myHeight +
        "SUA ALTEZZA " + height + "MIA COORDINATA X " + myXCoord + "SUA COORDINATA X " + xCoord);
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

        System.out.println("L1: "+ this.l1 + "L2: " + this.l2);
    }

    //THIRD: calculate the cells to be sent.
    //reverseList is used to know how to manage the array of cells received.
    private void evaluateCells(){
        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){
            indexFirstCell =(int) Math.ceil((double)(myYCoord - l1)/(double)this.cellSize/50.0);
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
            indexFirstCell =(int) Math.ceil((double)(myXCoord - l1)/(double)this.cellSize/50.0);
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

    public int getIndexFirstCell(){
        return this.indexFirstCell;
    }

    public int getIndexLastCell(){
        return this.indexLastCell;
    }

    public List<Boolean> getCellsValues(){

        boolean[][] matrix = this.gridView.getCellMatrix();
        System.out.println("MATRICE " + matrix.toString());
        int rows = matrix.length;
        int columns = matrix[0].length;

        System.out.println("PRIMO INDICE: +" + this.indexFirstCell + " ULTIMO INDICE " + this.indexLastCell);
        switch(myDir){
            case RIGHT:
                for(int i = this.indexFirstCell; i<this.indexLastCell; i++){
                    cellsToSend.add(matrix[i][columns]);
                };
                break;
            case LEFT:
                for(int i = this.indexFirstCell; i<this.indexLastCell; i++){
                    cellsToSend.add(matrix[i][0]);
                };
                break;
            case UP:
                for(int i = this.indexFirstCell; i<this.indexLastCell; i++){
                cellsToSend.add(matrix[0][i]); //TODO: VERIFY- la riga 0 è in cima o in fondo?
                };
                break;
            case DOWN:
                for(int i = this.indexFirstCell; i<this.indexLastCell; i++){
                cellsToSend.add(matrix[rows][i]); //TODO: VERIFY- la riga 0 è in cima o in fondo?
                };
                break;

        }

        if(reverseList){
            Collections.reverse(cellsToSend);
        }

        return cellsToSend;
    }


}
