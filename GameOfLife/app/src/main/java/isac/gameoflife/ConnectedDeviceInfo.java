package isac.gameoflife;

/**
 * Created by Francesco on 16/03/2017.
 */

public class ConnectedDeviceInfo {

    private boolean portrait;
    private int x,y;
    private String nameQueueSender,nameQueueReceiver;
    private int orientation; //gradi di rotazione
    private int myWidth, myHeight, width, height,myXCoord,myYCoord,xCoord,yCoord;//x e y mie e sue, height e width mie e sue
    private PinchInfo.Direction myDir,dir;
    private int l1, l2, cellSize, indexFirstCell, indexLastCell;

    public ConnectedDeviceInfo(int cellSize,boolean portrait, int x, int y,int uno,int due,int tre,int quattro, String nameQueueSender, String nameQueueReceiver){
        this.portrait = portrait;
        this.x=x;
        this.y=y;
        this.nameQueueReceiver=nameQueueReceiver;
        this.nameQueueSender=nameQueueSender;
        this.cellSize = cellSize;
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

            } else if (orientation == 90){

            } else if (orientation == 180){

            } else if (orientation == 270){

            }
        } else if (myDir.equals(PinchInfo.Direction.UP) || myDir.equals(PinchInfo.Direction.DOWN)){
            indexFirstCell =(int) Math.ceil((double)(myXCoord - l1)/(double)this.cellSize);
            indexLastCell = (myXCoord + l2)/this.cellSize;
            if(orientation == 0){

            } else if (orientation == 90){

            } else if (orientation == 180){

            } else if (orientation == 270){

            }
        }
    }

}
