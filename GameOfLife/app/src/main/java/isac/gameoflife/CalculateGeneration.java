package isac.gameoflife;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Francesco on 14/05/2017.
 */

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
     * Set state of the cell
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
     * Calculate the generations until receive a command of stop
     */
    public void calculate(){
        if(handler==null){
            handler=gridView.getGameHandler();
        }

        if(handler.isConnected()){

            boolean goOn=true;
            /*
                stopGame restituisce true in tre casi:

                - il device non ha più vicini (o si è disconnesso lui dagli altri o mano a mano tutti i
                  device si sono disconnessi da lui)

                - ha ricevuto il messaggio di stop da qualche vicino

                - l'utente ha effettuato lo stop sul device e quando viene inviato il messaggio di stop ai vicini
                  viene settata a true la variabile restituita da questo metodo

            */

            /*
                Alla ripresa del gioco (se il device è ancora connesso deve riprendere dal punto in
                cui si è fermato. questo per evitare che al riavvio del gioco il device mandi di nuovo
                le celle, rendendo impossibile la sincronizzazione con gli altri device qualora si trovasse
                una generazione più avanti dei vicini
            */

            while(goOn){

                //send the cells
                handler.sendCellsToOthers();

                //waits until receiving all the cells or the device is not connected with another one anymore
                while(handler.isConnected() && !handler.goOn() && !handler.stopGame()){
                    delay(20);
                }

                //posso uscire dal while per 3 motivi:
                // 1) ho ricevuto le celle da tutti i vicini
                // 2) non sono più connesso con nessuno (mi sono disconnesso io da loro o gli altri da me
                // 3) ho ricevuto un comando di stop da qualche vicino

                //indipendentemente dal motivo per cui sono uscito dal while, controllo se ho ricevuto
                //le celle da tutti i miei vicini
                if(handler.goOn()){
                    //invoco il metodo dell'handler che setta le celle "fantasma" della griglia
                    handler.setCells();
                    //calcolo la generazione
                    calculateNextGen();
                    //forzo il disegno della griglia sulla view
                    gridView.postInvalidate();

                    //controllo se sul mio device è stata effetuata la pausa
                    if(gridView.isStarted()){
                        //se non è stata effettuata, aspetto mezzo secondo prima di iniziare la generazione
                        //successiva
                        delay(500);
                    }else{
                        //altrimenti invio il messaggio di pausa
                        JSONObject message=new JSONObject();
                        try {
                            message.put("type","pause");
                            message.put(PinchInfo.ADDRESS,ipAddress);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //send pause message
                        //quando invoco questo metodo, la variabile restituita da stopGame viene settata a true
                        //quindi esco dal while
                        handler.sendCommand(message,null);
                    }
                }

                if(handler.stopGame()){
                    //se mi devo fermare, non resetto i flag di tutti i device connessi a cui ho
                    //inviato le celle. così, in caso di restart, le invio solo a chi non le ho inviate
                    //prima. quindi alle nuove code create (ad esempio un device si scollega e poi
                    //si ricollega)
                    goOn=false;
                    //mostro il toast di pausa
                    gridView.pause();
                }else{
                    //vado avanti quindi resetto i flag di tutti i device a cui ho inviato le celle
                    //prima di inviare nuovamente le celle
                    handler.resetCellSent();
                }

                //resetto le celle fantasma. in questo modo resetto le celle di device che si sono
                //disconnessi nella generazione corrente
                resetGhostCells();
            }
        }else{
            while(gridView.isStarted()){
                //calculate the next generation of cells
                calculateNextGen();
                //force redraw of the grid
                gridView.postInvalidate();
                delay(500);
            }
        }
    }

    /**
     * Count how many neighbors are alive
     * @param i Row of the matrix
     * @param j Column of the matrix
     * @return the number of live neighbors
     */
    private int neighboursAlive(int i,int j){
        int neighbours=0;

        if(cells[i-1][j-1]){
            neighbours++;
        }

        if(cells[i-1][j]){
            neighbours++;
        }

        if(cells[i-1][j+1]){
            neighbours++;
        }

        if(cells[i][j-1]){
            neighbours++;
        }

        if(cells[i][j+1]){
            neighbours++;
        }

        if(cells[i+1][j-1]){
            neighbours++;
        }

        if(cells[i+1][j]){
            neighbours++;
        }

        if(cells[i+1][j+1]){
            neighbours++;
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
