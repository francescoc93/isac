package isac.gameoflife;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


public class ConnectedDeviceInfo {

    private boolean reverseList;
    private String nameQueueSender,nameQueueReceiver;
    private int orientation;
    private float myWidth, myHeight, width, height,myXCoord,myYCoord,xCoord,yCoord;
    private PinchInfo.Direction myDir,dir;
    private float cellSize;
    private int indexFirstCell, indexLastCell;
    private List<Boolean> cellsToSend;
    private CalculateGeneration calculateGeneration;
    private List<List<Boolean>> generations;
    private boolean sent;

    public ConnectedDeviceInfo(float cellSize, PinchInfo.Direction dir, PinchInfo.Direction myDir,
                               int xCoord, int yCoord, float width, float height, float myWidth, float myHeight,
                               int myXCoord, int myYCoord, String nameQueueSender, String nameQueueReceiver,CalculateGeneration calculateGeneration,
                               float xdpi,float ydpi,float myXdpi,float myYdpi){

        this.calculateGeneration = calculateGeneration;
        this.xCoord=Utils.pixelsToInches(xCoord,xdpi);
        this.yCoord=Utils.pixelsToInches(yCoord,ydpi);
        this.myYCoord = Utils.pixelsToInches(myYCoord,myYdpi);
        this.myXCoord = Utils.pixelsToInches(myXCoord,myXdpi);
        this.width = width;
        this.height = height;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
        this.cellSize = cellSize;
        this.cellsToSend = new ArrayList<>();
        this.reverseList = false;
        this.myWidth = myWidth;
        this.myHeight = myHeight;
        this.myDir = myDir;
        this.dir = dir;
        generations=new ArrayList<>();
        sent=false;
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
        calculateIndices();
        evaluateReverseList();
    }
    /**
     * Sets the relative orientation of the devices.
     * The orientation is calculated considering the current device as in the middle of the origin
     * with its center (0,0). We have then 16 cases, 4 for each border(4).
     */
    private void setRelativeOrientation(){


        if(myDir.equals(PinchInfo.Direction.RIGHT)){
            if(dir.equals(PinchInfo.Direction.LEFT)){
                this.orientation = 0;
            } else if(dir.equals(PinchInfo.Direction.RIGHT)){
                this.orientation = 180;
            } else if(dir.equals(PinchInfo.Direction.DOWN)){
                this.orientation = 90;
            } else if(dir.equals(PinchInfo.Direction.UP)){
                this.orientation = 270;
            }
        } else if(myDir.equals(PinchInfo.Direction.LEFT)){
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

    }

    /**
     * Calculates the first and last index of the cells that will have to be sent to the other device.
     */
    private void calculateIndices(){

        int min;
        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){
            if(orientation == 0){
                min = Math.min((int)(myYCoord/this.cellSize), (int)(yCoord/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1, (int)((height - yCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
            } else if (orientation == 90){
                min = Math.min((int)(myYCoord/this.cellSize), (int)((xCoord)/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1, (int)((width-xCoord)/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
            } else if (orientation == 180){
                min = Math.min((int)(myYCoord/this.cellSize),(int)((height-yCoord)/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1, (int)(yCoord/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
            } else if (orientation == 270){
                min = Math.min((int)(myYCoord/this.cellSize),(int)((width-xCoord)/this.cellSize));
                this.indexFirstCell = (int)(myYCoord/this.cellSize)+1-min;
                min = Math.min((int)((myHeight-myYCoord)/this.cellSize)+1,(int)(xCoord/this.cellSize)+1);
                this.indexLastCell = (int)(myYCoord/this.cellSize) + min;
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
    }

    /**
     * Checks if the list of cells to be sent has to ve reversed in order to correspond to the indices of the other device.
     */
    private void evaluateReverseList(){

        if(myDir.equals(PinchInfo.Direction.RIGHT) || myDir.equals(PinchInfo.Direction.LEFT)){

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


    /**
     *
     * @return the index of the first cell
     */
    public int getIndexFirstCell(){
        return this.indexFirstCell;
    }

    /**
     *
     * @return the index of the last cell
     */
    public int getIndexLastCell(){
        return this.indexLastCell;
    }

    /**
     *
     * @return the list of values of the cells to be sent
     */
    public List<Boolean> getCellsValues(){

        boolean[][] matrix = this.calculateGeneration.getCells();
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
        return cellsToSend;
    }

    /**
     * Adds a new list representing the new generation received.
     * */
    public void addGeneration(List<Boolean> generation){
        generations.add(generation);
    }

    /**
     *
     * @return the first element of the list of generations.
     */
    public List<Boolean> getNextGeneration(){
        if(generations.size()!=0){
            return generations.remove(0);
        }

        return null;
    }

    /**
     * This method is used to check whether the device has at least one generation saved from its neighbour.
     * @return size of the list of generations.
     */
    public int getNumberOfGenerations(){
        return generations.size();
    }

    /**
     *
     * @param value true if the correspondent cells has been sent; false otherwise.
     */
    public void cellsSent(boolean value){
        sent=value;
    }

    /**
     *
     * @return true if the cells has been sent to that device, false otherwise.
     */
    public boolean cellsSent(){
        return sent;
    }
}
