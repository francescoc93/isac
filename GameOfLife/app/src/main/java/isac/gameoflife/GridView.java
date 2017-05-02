package isac.gameoflife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


public class GridView extends View {

    private final static int TIME_DOUBLE_TAP=180;
    private final static float DESIRED_DP_VALUE=80.0f;
    private float SIZE;
    private Handler handler;
    private float width;
    private float height;
    private int row;
    private int column;
    private int startX;
    private int startY;
    private int numberOfTaps;
    private Paint whitePaint = new Paint();
    private boolean[][] cellChecked;
    private String ipAddress;
    private MainActivity activity;
    private AtomicBoolean started=new AtomicBoolean(false);
    private Long lastTapTimeMs,touchDownMs;
    //il primo pair sono il timestamp e la direzione, il secondo le coordinate x e y
    private Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> infoSwipe;
    private ReentrantLock lockInfoSwipe,lockAction,lockHandler;
    private CalculateGeneration calculateGeneration;

    public GridView(final Context context) {
        super(context);
        activity=(MainActivity)context;
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
        ipAddress=Utils.getIpAddress();
        numberOfTaps=0;
        lastTapTimeMs=0L;
        touchDownMs=0L;
        calculateGeneration=null;
        SIZE=DESIRED_DP_VALUE * getResources().getDisplayMetrics().density;
        lockInfoSwipe=new ReentrantLock();
        lockAction=new ReentrantLock();
        lockHandler=new ReentrantLock();
    }

    public float getXDpi() {return getResources().getDisplayMetrics().xdpi; }
    public float getYDpi() {return getResources().getDisplayMetrics().ydpi; }

    public Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> getInfoSwipe(){
        lockInfoSwipe.lock();

        Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> tmp;

        if(infoSwipe!=null) {
            tmp=new Pair<>(new Pair<>(infoSwipe.first.first,infoSwipe.first.second), new Pair<>(infoSwipe.second.first,infoSwipe.second.second));
        }else{
            tmp=null;
        }


        lockInfoSwipe.unlock();

        return tmp;
    }

    public boolean isStarted(){
        return started.get();
    }

    public void start(){
        //se il gioco non è stato già avviato, lo avvio eseguendo
        //un async task (necessario in quanto il thread UI non si deve bloccare)
        //se l'espressione booleana è false (primo parametro), imposto a true la variabile e proseguo
        lockAction.lock();

        if(started.compareAndSet(false,true)){

            if(calculateGeneration!=null){
                if(calculateGeneration.isAlive()){
                    try {
                        calculateGeneration.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            calculateGeneration=new CalculateGeneration();
            calculateGeneration.start();

            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Start", Toast.LENGTH_SHORT).show();
                }
            });
        }

        lockAction.unlock();
    }

    public void pause(){
        //se il gioco è in esecuzione, setto a false la variabile
        lockAction.lock();

        if(started.get()){
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Pausa", Toast.LENGTH_SHORT).show();
                }
            });
        }

        started.compareAndSet(true,false);

        lockAction.unlock();
    }


    //disegno la griglia e la popolo
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int count=0;
        //System.out.println(" CELLA " + SIZE);
        //disegno delle righe per formare la griglia
        while(count<=column){
            float coordinate=count*SIZE;
            canvas.drawLine(coordinate,0,coordinate,row*SIZE,whitePaint);
            count++;
        }


        count=0;

        while(count<=row){
            float coordinate=count*SIZE;
            canvas.drawLine(0,coordinate,column*SIZE,coordinate,whitePaint);
            count++;
        }

        //setto le cellule vive
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                if (cellChecked[i+1][j+1]) {
                    //disegno un rettangolo in corrispondenza della cellula viva
                    canvas.drawRect(j * SIZE, i * SIZE,(j + 1) * SIZE, (i + 1) * SIZE,whitePaint);
                }
            }
        }

    }

    //metodo che rileva i tocchi sullo schermo
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            //l'utente ha il dito appoggiato sullo schermo
            case (MotionEvent.ACTION_DOWN) :
                touchDownMs = System.currentTimeMillis();
                startX=(int)event.getX();
                startY=(int)event.getY();
                return true;
            //l'utente sta muovendo il dito
            case (MotionEvent.ACTION_MOVE) :
                return true;
            //l'utente ha rilasciato il dito
            case (MotionEvent.ACTION_UP) :
                int stopX=(int)event.getX();
                int stopY=(int)event.getY();


                if ((System.currentTimeMillis() - touchDownMs) > TIME_DOUBLE_TAP) {
                    numberOfTaps = 0;
                    lastTapTimeMs = 0L;

                    //se la differenza delle coordinate di inizio e fine del movimento è minore di 3, allora
                    //l'utente vuole "attivare" una cella della griglia. altrimenti, potrebbe essere uno swipe
                    //per lo swipe controllare che il movimento sia lungo solo uno dei due assi e non entrambi
                    // (altrimenti mi sto muovendo in diagonale)
                    if (Math.abs(startX - stopX) < 10 && Math.abs(startY - stopY) < 10 && !started.get()) {
                        setCell(event.getX(),event.getY());
                    } else { //valuto lo swipe
                        evaluateSwipe(stopX,stopY);
                    }
                }

                evaluateAction();

                return true;
            default : return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(changed) {
            width = getWidth();
            height = getHeight();
            column =(int) (width /SIZE) ;
            row = (int)(height /SIZE);
            width = column*Utils.pixelsToInches(SIZE,getResources().getDisplayMetrics().xdpi);
            height = row*Utils.pixelsToInches(SIZE,getResources().getDisplayMetrics().ydpi);

            lockHandler.lock();

            handler=new Handler(this,activity,width,height,Utils.pixelsToInches(SIZE,getXDpi()));

            lockHandler.unlock();

            new AsyncTask<Void,Void,Void>(){

                @Override
                protected Void doInBackground(Void... params) {

                    if(handler.connectToServer()) {
                        handler.bindToBroadcastQueue();
                    }

                    return null;
                }

            }.execute();

            cellChecked = new boolean[row+2][column+2];
        }

    }


    public boolean[][] getCellMatrix(){
        return this.cellChecked;
    }

    public Handler getGameHandler(){

        lockHandler.lock();
        Handler tmp=handler;
        lockHandler.unlock();

        return tmp;
    }

    /**
     * Sets the outer border cells, where 2 devices are in contact.
     * The direction of the swipe is essential: you need to recognise what portion of screen
     * corresponds to one specific neighbour.
     * @param firstIndex
     * @param lastIndex
     * @param cells
     * @param direction the direction of the CURRENT device swipe
     */
    public void setPairedCells(int firstIndex, int lastIndex, List<Boolean> cells, PinchInfo.Direction direction){
        switch(direction){
            case RIGHT:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cellChecked[i][column+1] = cells.get(j);
                };
                break;
            case LEFT:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cellChecked[i][0] = cells.get(j);
                };
                break;
            case UP:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cellChecked[0][i] = cells.get(j);
                };
                break;
            case DOWN:
                for(int i = firstIndex,j=0; i<=lastIndex; i++,j++){
                    cellChecked[row+1][i] = cells.get(j);
                };
                break;
        }



    }

    private void sendBroadcastMessage(Long timeStamp,PinchInfo.Direction direction,int x,int y){
        lockInfoSwipe.lock();

        infoSwipe=new Pair<>(new Pair<>(timeStamp,direction),new Pair<>(x,y));

        lockInfoSwipe.unlock();

        handler.sendBroadcastMessage(new PinchInfo(ipAddress, direction,x,y,timeStamp, width, height,getXDpi(),getYDpi()).toJSON());
    }

    private void setCell(float x,float y){
        int column = (int) (x / SIZE);
        int row = (int) (y / SIZE);

        if(column<this.column && row<this.row) {
            column+=1;
            row+=1;

            cellChecked[row][column] = !cellChecked[row][column];
            //chiamo il metodo invalidate così forzo la chiamata del metodo onDraw
            invalidate();
        }
    }

    private void evaluateSwipe(int stopX,int stopY){
        long timeStamp = System.currentTimeMillis();
        PinchInfo.Direction direction=null;

        if (Math.abs(startX - stopX) >=10 && Math.abs(startY - stopY) <= 80){//se mi sono mosso sulle X
            if((stopX - startX) > 0){
                direction=PinchInfo.Direction.RIGHT;
            } else if ((stopX - startX)<0){
                direction=PinchInfo.Direction.LEFT;
            }

            sendBroadcastMessage(timeStamp,direction,stopX,stopY);

        } else if (Math.abs(startX - stopX) <=80 && Math.abs(startY - stopY) >= 10){//mi sono mosso sulle Y
            if((stopY - startY) > 0){
                direction=PinchInfo.Direction.DOWN;
            } else if ((stopY - startY)<0){
                direction=PinchInfo.Direction.UP;
            }

            sendBroadcastMessage(timeStamp,direction,stopX,stopY);
        }
    }

    private void evaluateAction(){
        if (numberOfTaps > 0
                && (System.currentTimeMillis() - lastTapTimeMs) < TIME_DOUBLE_TAP) {
            numberOfTaps += 1;
        } else {
            numberOfTaps = 1;
        }

        lastTapTimeMs = System.currentTimeMillis();

        if (numberOfTaps == 2) {
            if(isStarted()){
                pause();
            }else{
                if(handler.isConnected()){
                    JSONObject message=new JSONObject();
                    try {
                        message.put("type","start");
                        message.put(PinchInfo.ADDRESS,ipAddress);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    handler.sendCommand(message,null);
                }
                start();
            }

        }
    }

    private class CalculateGeneration extends Thread {

        private int neighboursAlive(int i,int j){
            int neighbours=0;

            if(cellChecked[i-1][j-1]){
                neighbours++;
            }

            if(cellChecked[i-1][j]){
                neighbours++;
            }

            if(cellChecked[i-1][j+1]){
                neighbours++;
            }

            if(cellChecked[i][j-1]){
                neighbours++;
            }

            if(cellChecked[i][j+1]){
                neighbours++;
            }

            if(cellChecked[i+1][j-1]){
                neighbours++;
            }

            if(cellChecked[i+1][j]){
                neighbours++;
            }

            if(cellChecked[i+1][j+1]){
                neighbours++;
            }

            return neighbours;
        }

        private void resetGhostCells(){
            for(int i=0;i<column+2;i++){
                cellChecked[0][i]=false;
                cellChecked[row+1][i]=false;
            }

            for(int i=0;i<row+2;i++){
                cellChecked[i][0]=false;
                cellChecked[i][column+1]=false;
            }
        }

        private void calculateNextGen(){
            boolean [][] tmp=new boolean[row+2][column+2];

            for(int i=1;i<row+1;i++){
                for(int j=1;j<column+1;j++){
                    int neighbours=neighboursAlive(i,j);

                    //se attualmente la cellula è viva
                    if(cellChecked[i][j]) {
                        //e ha 2 o 3 vicini, continua a vivere
                        if (neighbours==2 || neighbours==3) {
                            tmp[i][j] = true;
                        }
                    }else{
                        //se la cellula è morta e ha esattamente 3 vicini
                        //nella generazione successiva prende vita
                        if(neighbours==3){
                            tmp[i][j]=true;
                        }
                    }
                }
            }

            cellChecked=tmp;
        }

        private void sendAndWaitCells(){
            //invio ai miei vicini le celle
            handler.sendCellsToOthers();
            //controllo se posso proseguire (ovvero ho ricevuto le celle da tutti i vicini)
            while(!handler.goOn() && handler.isConnected() && !handler.stopGame()){
                // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendAndWaitOthers(){
            handler.readyToContinue(); //invio un messaggio ai miei vicini con lo scopo di avvisarli che sono pronto a inviare le mie celle
            // faccio il while fino a quando tutti i miei vicini
            // non hanno terminato di calcolare la propria generazione
            while (!handler.readyToSendCells() && handler.isConnected() && !handler.stopGame()) {
                // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            handler.resetReadyReceived();
            postInvalidate();
        }

        private void stopGame(){
            if(handler.isConnected()){

                JSONObject message=new JSONObject();
                try {
                    message.put("type","pause");
                    message.put(PinchInfo.ADDRESS,ipAddress);
                    message.put("sender",ipAddress);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                handler.sendCommand(message,null);
                receiveCellsAndReset();
            }

            pause();
        }

        private void receiveCellsAndReset(){
            while(handler.isConnected() && !handler.cellsReceived()){
                // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            handler.resetCellsReceived();
            resetGhostCells();
        }

        @Override
        public void run() {
            boolean goOn=true;

            if(handler.isConnected()){
                while(goOn){
                    long initTime = System.currentTimeMillis();

                    sendAndWaitCells();

                    //se il while termina perchè ho ricevuto le celle da tutti i device, calcolo la generazione successiva
                    if(handler.isConnected() && !handler.stopGame()) {
                        handler.resetCellsReceived();
                        calculateNextGen(); //calcolo la generazione
                        sendAndWaitOthers();

                        if(handler.isConnected() && !handler.stopGame()){
                            //se l'utente non ha messo in pausa il gioco
                            if(started.get()){
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                //altrimenti fermo il calcolo delle generazione successiva
                                goOn=false;
                                stopGame();
                            }
                        }else{
                            resetGhostCells();
                            goOn=false;
                            pause();
                        }
                    }else{
                        goOn=false;
                        receiveCellsAndReset();
                        handler.resetReadyReceived();
                        pause();
                    }

                    long endTime = System.currentTimeMillis();
                    System.out.println("Elapsed time from previous generation: " + (endTime-initTime));
                }
            }else{
                while(goOn){
                    calculateNextGen();
                    postInvalidate();

                    //se l'utente non ha messo in pausa il gioco
                    if(started.get()){
                        try {

                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        //altrimenti fermo il calcolo delle generazione successiva
                        goOn=false;
                    }
                }
            }
        }
    }
}
