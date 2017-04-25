package isac.gameoflife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

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
    private float scale;
    private boolean cellsReceived, readyReceived;

    public ConnectedDeviceInfo(float cellSize, PinchInfo.Direction dir, PinchInfo.Direction myDir,
                               int xCoord, int yCoord, float width, float height, float myWidth, float myHeight,
                               int myXCoord, int myYCoord, String nameQueueSender, String nameQueueReceiver,GridView gridView,
                               float xdpi,float ydpi,float myXdpi,float myYdpi){

        this.gridView = gridView;
        this.scale = gridView.getScale();
        this.xCoord=Utils.pixelsToInches(xCoord,xdpi);
        this.yCoord=Utils.pixelsToInches(yCoord,ydpi);
        this.myYCoord = Utils.pixelsToInches(myYCoord,myYdpi);
        this.myXCoord = Utils.pixelsToInches(myXCoord,myXdpi);
        //this.width = Utils.pixelsToInches(width,xdpi);
        //this.height = Utils.pixelsToInches(height,ydpi);
        this.width = width;
        this.height = height;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
        this.cellSize = cellSize;//gridView.getCellSize()/gridView.getXDpi();
        this.cellsToSend = new ArrayList<>();
        this.reverseList = false;
       // this.myWidth = Utils.pixelsToInches(myWidth,gridView.getXDpi());
        //this.myHeight = Utils.pixelsToInches(myHeight,gridView.getYDpi());
        this.myWidth = myWidth;
        this.myHeight = myHeight;
        this.myDir = myDir;
        this.dir = dir;

        cellsReceived=false;
        readyReceived=false;

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
        //if(myXCoord > myWidth - 20 && myXCoord <= myWidth){
            if(myDir.equals(PinchInfo.Direction.RIGHT)){
                System.out.println("PINCH AVVENUTO NELLA CELLA: " +(int)(((myYCoord)/this.cellSize)+1) + " RIGHT");

                if(dir.equals(PinchInfo.Direction.LEFT)){
                this.orientation = 0;
            } else if(dir.equals(PinchInfo.Direction.RIGHT)){
                this.orientation = 180;
            } else if(dir.equals(PinchInfo.Direction.DOWN)){
                this.orientation = 90;
            } else if(dir.equals(PinchInfo.Direction.UP)){
                this.orientation = 270;
            }




        } else if(myDir.equals(PinchInfo.Direction.LEFT)){ //LEFT
                 System.out.println("PINCH AVVENUTO NELLA CELLA: " +(int)(((myYCoord)/this.cellSize)+1) + " LEFT");
            if(dir.equals(PinchInfo.Direction.LEFT)){
                this.orientation = 180;
            } else if(dir.equals(PinchInfo.Direction.RIGHT)){
                this.orientation = 0;
            } else if(dir.equals(PinchInfo.Direction.DOWN)){
                this.orientation = 270;
            } else if(dir.equals(PinchInfo.Direction.UP)){
                this.orientation = 90;
            }


        } else if (myDir.equals(PinchInfo.Direction.UP)){
                System.out.println("PINCH AVVENUTO NELLA CELLA: " +(int)(((myXCoord)/this.cellSize)+1) + " UP" );

            if(dir.equals(PinchInfo.Direction.LEFT)){
                this.orientation = 270;
            } else if(dir.equals(PinchInfo.Direction.RIGHT)){
                this.orientation = 90;
            } else if(dir.equals(PinchInfo.Direction.DOWN)){
                this.orientation = 0;
            } else if(dir.equals(PinchInfo.Direction.UP)){
                this.orientation = 180;
            }

        } else if(myDir.equals(PinchInfo.Direction.DOWN)){
                System.out.println("PINCH AVVENUTO NELLA CELLA: " +(int)(((myXCoord)/this.cellSize)+1) + " DOWN");

            if(dir.equals(PinchInfo.Direction.LEFT)){
                this.orientation = 90;
            } else if(dir.equals(PinchInfo.Direction.RIGHT)){
                this.orientation = 270;
            } else if(dir.equals(PinchInfo.Direction.DOWN)){
                this.orientation = 180;
            } else if(dir.equals(PinchInfo.Direction.UP)){
                this.orientation = 0;
            }
        }
        System.out.println("ORIENTATION " + this.orientation);

    }

    /**
     * After evaluating the orientation of the device, this method calculates the lengths of the parts splitted by the swipe points
     */
    private void calculateL1L2(){ //16 casi (TRIMMED TO 8)

        int min;
        //SECOND: CALCULATE L1 AND L2 (length of the portions of screen before and after the swipe point)
        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){
            if(orientation == 0){
                min = Math.min((int)(myYCoord/this.cellSize), (int)(yCoord/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1, (int)((height - yCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
                //this.l1 = Math.min(myYCoord,yCoord);
                //this.l2 = Math.min((myHeight-myYCoord), (height - yCoord));
            } else if (orientation == 90){
                min = Math.min((int)(myYCoord/this.cellSize), (int)((xCoord)/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1, (int)((width-xCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
                //this.l1 = Math.min(myYCoord, (width-xCoord));
                //this.l2 = Math.min((myHeight-myYCoord), xCoord);
            } else if (orientation == 180){
                min = Math.min((int)(myYCoord/this.cellSize),(int)((height-yCoord)/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1, (int)(yCoord/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
                //this.l1 = Math.min(myYCoord,(height-yCoord));
                //this.l2 = Math.min((myHeight-myYCoord), yCoord);
            } else if (orientation == 270){
                min = Math.min((int)(myYCoord/this.cellSize),(int)((width-xCoord)/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1,(int)(xCoord/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
               // this.l1 = Math.min(myYCoord,xCoord);
                //this.l2 = Math.min((myHeight-myYCoord),(width-xCoord));
            }
        } else if (myDir.equals(PinchInfo.Direction.UP) || myDir.equals(PinchInfo.Direction.DOWN)){
            if(orientation == 0){
                min = Math.min((int)(myXCoord/this.cellSize), (int)(xCoord/this.cellSize));
                this.indexFirstCell = (int)(myXCoord/this.cellSize)+1-min;
                min = Math.min((int)((myWidth-myXCoord)/this.cellSize)+1,(int)((width-xCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myXCoord/this.cellSize) + min;
            } else if (orientation == 90){
                min = Math.min((int)(myXCoord/this.cellSize),(int)((height-yCoord)/this.cellSize));
                this.indexFirstCell = (int)(myXCoord/this.cellSize)+1-min;
                min = Math.min((int)((myWidth-myXCoord)/this.cellSize)+1,(int)((yCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myXCoord/this.cellSize) + min;
            } else if (orientation == 180){
                min = Math.min((int)(myXCoord/this.cellSize), (int)((width-xCoord)/this.cellSize));
                this.indexFirstCell = (int)(myXCoord/this.cellSize)+1-min;
                min = Math.min((int)((myWidth-myXCoord)/this.cellSize)+1,(int)(xCoord/this.cellSize)+1);
                this.indexLastCell = (int)(myXCoord/this.cellSize) + min;
            } else if (orientation == 270){
                min = Math.min((int)(myXCoord/this.cellSize), (int)((yCoord)/this.cellSize));
                this.indexFirstCell = (int)(myXCoord/this.cellSize)+1-min;
                min = Math.min((int) ((myWidth-myXCoord)/this.cellSize)+1,(int)((height-yCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myXCoord/this.cellSize) + min;
            }
        }

        /*else if (myDir.equals(PinchInfo.Direction.UP) || myDir.equals(PinchInfo.Direction.DOWN)){
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
        }*/

    }

    //THIRD: calculate the cells to be sent.
    //reverseList is used to know how to manage the array of cells received.
    private void evaluateCells(){

        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){
            //indexFirstCell =(int) Math.ceil((double)(myYCoord - l1)/(double)this.cellSize) +1;
           // int numOfCells = (int) (length/(double)this.cellSize);
           // indexLastCell = indexFirstCell+numOfCells;
            //indexLastCell = (int)((myYCoord + l2)/this.cellSize);
            if(orientation == 0){
                this.reverseList = false;
            } else if (orientation == 90){
                this.reverseList = false;
            } else if (orientation == 180){
                this.reverseList = true;
            } else if (orientation == 270){
                this.reverseList = true;
            }
        } else if (myDir.equals(PinchInfo.Direction.UP) || myDir.equals(PinchInfo.Direction.DOWN)){
           // indexFirstCell =(int) Math.ceil((double)(myXCoord - l1)/(double)this.cellSize) +1;
           // int numOfCells = (int) (length/(double)this.cellSize);
            //indexLastCell = indexFirstCell+numOfCells;
            //indexLastCell = (int) ((myXCoord + l2)/this.cellSize);
            if(orientation == 0){
                this.reverseList = false;
            } else if (orientation == 90){
                this.reverseList = true;
            } else if (orientation == 180){
                this.reverseList = true;
            } else if (orientation == 270){
                this.reverseList = false;
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
        int rows = matrix.length-2;
        int columns = matrix[0].length-2;


        cellsToSend.clear();
        switch(myDir){
            case RIGHT:
                for(int i = this.indexFirstCell; i<=this.indexLastCell; i++){
                    cellsToSend.add(matrix[i][columns]);
                };
                break;
            case LEFT:
                for(int i = this.indexFirstCell; i<=this.indexLastCell; i++){
                    cellsToSend.add(matrix[i][1]);
                };
                break;
            case UP:
                for(int i = this.indexFirstCell; i<=this.indexLastCell; i++){
                    cellsToSend.add(matrix[1][i]);
                };
                break;
            case DOWN:
                for(int i = this.indexFirstCell; i<=this.indexLastCell; i++){
                    cellsToSend.add(matrix[rows][i]);
                };
                break;

        }

        if(reverseList){
            Collections.reverse(cellsToSend);
        }

        System.out.println("LISTA DA INVIARE " + cellsToSend.toString());

        return cellsToSend;
    }


    public boolean isCellsReceived() {
        return cellsReceived;
    }

    public void setCellsReceived(boolean cellsReceived) {
        this.cellsReceived = cellsReceived;
    }

    public boolean isReadyReceived() {
        return readyReceived;
    }

    public void setReadyReceived(boolean readyReceived) {
        this.readyReceived = readyReceived;
    }
}
