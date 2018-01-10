package isac.gameoflife;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class CalculateGeneration {

    private boolean[][] cells;
    private int row,column;
    private GridView gridView;
    private Handler handler;
    private String ipAddress;

    /**
     *
     * @param row
     * @param column
     * @param  gridView
     */
    public CalculateGeneration(int row,int column,GridView gridView){
        cells=new boolean[row+2][column+2];
        this.row=row;
        this.column=column;
        this.gridView=gridView;
        handler=null;
        ipAddress=Utils.getIpAddress();
    }

    /**
     * Set the state of the cell (dead or alive)
     * @param row
     * @param column
     */
    public void setCell(int row,int column){
        cells[row][column]=!cells[row][column];
    }

    /**
     *
     * @return the matrix of cells
     */
    public boolean[][] getCells(){
        return cells;
    }


    /**
     * Sets the outer border cells, where 2 devices are in contact.
     * The direction of the swipe is essential: you need to recognise what portion of screen
     * corresponds to one specific neighbour.
     * @param firstIndex
     * @param lastIndex
     * @param cellsToSet
     * @param direction the direction of the CURRENT device swipe
     */
    public void setPairedCells(int firstIndex, int lastIndex, List<Boolean> cellsToSet, PinchInfo.Direction direction){
        switch(direction){
            case RIGHT:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cells[i][column+1] = cellsToSet.get(j);
                };
                break;
            case LEFT:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cells[i][0] = cellsToSet.get(j);
                };
                break;
            case UP:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cells[0][i] = cellsToSet.get(j);
                };
                break;
            case DOWN:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cells[row+1][i] = cellsToSet.get(j);
                };
                break;
        }
    }

    /**
     * Calculates the generations until receiving a stop command. Here resides the logic of the application.
     */
    public void calculate(){
        if(handler==null){
            handler=gridView.getGameHandler();
        }

        if(handler.isConnected()){
            /*
                stopGame can return true in three different cases:

                1) the device is not connected to anyone anymore
                2) the device has received the stop command from a neighbour
                3) the user has double tapped to stop the computation

                When the game starts again, if the device is still connected to someone, it has to start again from the point it has stopped,
                this to avoid that when the game restarts, the device would send again the cells to the ones it has already sent them to.
            */

            while(!handler.stopGame()){

                //send the cells to the other devices
                handler.sendCellsToOthers();

                //waits until receiving all the cells or the device is not connected with another one anymore, or the game was stopped
                while(handler.isConnected() && !handler.goOn() && !handler.stopGame()){
                    delay(20);
                }

                if(handler.goOn()){
                    /*If the device entered this condition, it means that it has all the cells for calculating the next generation;
                    if this condition holds, then it is necessary to reset all the flags about the devices whom the device has sent the cells to
                    to false, before sending the cells again.
                    */

                    handler.resetCellSent(); //All flags to false.

                    //sets the received cells
                    handler.setCells();

                    calculateNextGen();

                    gridView.postInvalidate();

                    //If the pause was not performed, the device continues with the computation.
                    if(gridView.isStarted()){

                        delay(500);
                    }else{
                        //The pause was performed, so the device forwards the pause message to the neighbours.
                        JSONObject message=new JSONObject();
                        try {
                            message.put("type","pause");
                            message.put(PinchInfo.ADDRESS,ipAddress);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //sends pause message
                        //the variable returned by stopgame is set to true, so the device exits from while.
                        handler.sendCommand(message,null);
                    }
                }

                //reset of ghost cells.
                resetGhostCells();
            }

            gridView.pause();
        }else{ //case when the device is not connected to anyone else.
            while(gridView.isStarted()){
                calculateNextGen();
                gridView.postInvalidate();
                delay(500);
            }
        }
    }

    /**
     * Counts how many neighbors are alive (game of life logic)
     * @param i Row of the matrix
     * @param j Column of the matrix
     * @return the number of live neighbors
     */
    private int neighboursAlive(int i,int j){
        int neighbours=0;

        for(int row_index=i-1;row_index<=i+1;row_index++){
            for(int column_index=j-1;column_index<=j+1;column_index++){
                if(cells[row_index][column_index]){
                    neighbours++;
                }
            }
        }

        return neighbours;
    }

    /**
     * Reset the state of the cells sent by the neighbors
     */
    private void resetGhostCells(){
        for(int i=0;i<column+2;i++){
            cells[0][i]=false;
            cells[row+1][i]=false;
        }

        for(int i=0;i<row+2;i++){
            cells[i][0]=false;
            cells[i][column+1]=false;
        }
    }

    /**
     * Calculate the next generation of cells
     */
    private void calculateNextGen(){
        boolean [][] tmp=new boolean[row+2][column+2];

        for(int i=1;i<row+1;i++){
            for(int j=1;j<column+1;j++){
                int neighbours=neighboursAlive(i,j);

                if(cells[i][j]) {
                    if (neighbours==2 || neighbours==3) {
                        tmp[i][j] = true;
                    }
                }else{
                    if(neighbours==3){
                        tmp[i][j]=true;
                    }
                }
            }
        }

        cells=tmp;
    }


    private void delay(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
