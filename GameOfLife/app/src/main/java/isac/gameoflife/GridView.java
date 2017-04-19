package isac.gameoflife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

/**
 * Created by Francesco on 07/03/2017.
 */

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
    private int numberOfTaps, numGen;
    private Paint whitePaint = new Paint();
    private boolean[][] cellChecked;
    private boolean onTable;
    private String ipAddress;
    private MainActivity activity;
    private AtomicBoolean started=new AtomicBoolean(false),clear=new AtomicBoolean(false);
    private Long lastTapTimeMs,touchDownMs ;
    //il primo pair sono il timestamp e la direzione, il secondo le coordinate x e y
    private Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> infoSwipe;
    private ReentrantLock lockInfoSwipe,lockAction;
    private CalculateGeneration calculateGeneration;
    float scale;

    public GridView(final Context context) {
        super(context);

        activity=(MainActivity)context;
        //imposto il colore delle celle
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
        ipAddress=Utils.getIpAddress();
        numberOfTaps=0;
        lastTapTimeMs=0L;
        touchDownMs=0L;
        onTable=false;
        numGen = 1;
        calculateGeneration=null;
        scale = getResources().getDisplayMetrics().density;
        SIZE=(DESIRED_DP_VALUE * scale);

        System.out.println("Altezza in pixel " + getResources().getDisplayMetrics().widthPixels + " Larghezza in pixel " +
                getResources().getDisplayMetrics().heightPixels + "densità: " +scale);

        lockInfoSwipe=new ReentrantLock();
        lockAction=new ReentrantLock();

        SensorManager sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(new SensorEventListener() {

            private float oldX=Float.MAX_VALUE-0.5f,oldY=Float.MAX_VALUE-0.5f,oldZ=Float.MAX_VALUE-0.5f;

            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                double g=Math.sqrt(x * x + y * y + z * z);

                x/=g;
                y/=g;
                z/=g;

                int inclination = (int) Math.round(Math.toDegrees(Math.acos(z)));

                if((inclination<=15)&&x>=(oldX-0.5)&&x<=(oldX+0.5)
                        &&y>=(oldY-0.5)&&y<=(oldY+0.5)){
                    if(!onTable) {
                        onTable = true;
                    }
                }else{
                    if(onTable) {
                        onTable = false;

                        if(handler.isConnected()){
                            Toast.makeText(context, "Schermo scollegato", Toast.LENGTH_SHORT).show();
                            handler.closeDeviceCommunication();
                        }
                    }
                }


                oldX=x;
                oldY=y;
                oldZ=z;


            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

        }, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public float getScale(){
        return this.scale;
    }
    public float getCellSize(){
        return this.SIZE;
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

        started.compareAndSet(true,false);

        lockAction.unlock();

        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, "Pause", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void clear(){
        lockAction.lock();

        if(!isStarted()){
            cellChecked=new boolean[row+2][column+2];
            postInvalidate();
        }else {
            clear.set(true);
            pause();
        }


        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, "Reset", Toast.LENGTH_SHORT).show();
            }
        });

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
                        int column = (int) (event.getX() / SIZE);
                        int row = (int) (event.getY() / SIZE);

                        if(column<this.column && row<this.row) {
                            column+=1;
                            row+=1;

                            cellChecked[row][column] = !cellChecked[row][column];
                            //chiamo il metodo invalidate così forzo la chiamata del metodo onDraw
                            invalidate();
                        }

                    } else { //valuto lo swipe
                        long timeStamp = System.currentTimeMillis();
                        PinchInfo.Direction direction=null;

                        if (Math.abs(startX - stopX) >=10 && Math.abs(startY - stopY) <= 50){//se mi sono mosso sulle X
                            if((stopX - startX) > 0){
                                direction=PinchInfo.Direction.RIGHT;
                            } else if ((stopX - startX)<0){
                                direction=PinchInfo.Direction.LEFT;
                            }

                            sendBroadcastMessage(timeStamp,direction,stopX,stopY);

                        } else if (Math.abs(startX - stopX) <=50 && Math.abs(startY - stopY) >= 10){//mi sono mosso sulle Y
                            if((stopY - startY) > 0){
                                direction=PinchInfo.Direction.DOWN;
                            } else if ((stopY - startY)<0){
                                direction=PinchInfo.Direction.UP;
                            }

                            sendBroadcastMessage(timeStamp,direction,stopX,stopY);
                        } else {
                            System.out.println("Mossa in diagonale");
                        }

                    }

                }

                if (numberOfTaps > 0
                        && (System.currentTimeMillis() - lastTapTimeMs) < TIME_DOUBLE_TAP) {
                    numberOfTaps += 1;
                } else {
                    numberOfTaps = 1;
                }

                lastTapTimeMs = System.currentTimeMillis();

                if (numberOfTaps == 3) {
                    clear();

                    if(handler.isConnected()) {

                        JSONObject message = new JSONObject();
                        try {
                            message.put("type", "reset");
                            message.put(PinchInfo.ADDRESS,ipAddress);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        handler.sendCommand(message);
                    }

                } else if (numberOfTaps == 2) {

                    if(isStarted()){
                        pause();
                    }else{
                        handler.stopGame(false);
                        start();

                        if(handler.isConnected()){

                            JSONObject message=new JSONObject();
                            try {
                                message.put("type","start");
                                message.put(PinchInfo.ADDRESS,ipAddress);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            handler.sendCommand(message);
                        }
                    }

                }

                return true;
            case (MotionEvent.ACTION_CANCEL) :
                return true;
            case (MotionEvent.ACTION_OUTSIDE) :
                return true;
            default :
                return super.onTouchEvent(event);
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

            handler=new Handler(this,activity,width,height);
            new AsyncTask<Void,Void,Void>(){

                @Override
                protected Void doInBackground(Void... params) {

                    if(handler.connectToServer()) {
                        System.out.println("Connessione riuscita");
                        handler.bindToBroadcastQueue();
                    }else{
                        System.out.println("Connessione non riuscita");
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
        System.out.println("PRIMO INDICE: " + firstIndex + " LAST INDEX: " + lastIndex );
        switch(direction){
            case RIGHT:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    System.out.println("SWIPE A DESTRA, LISTA RICEVUTA: "+cells.toString());
                    cellChecked[i][column+1] = cells.get(j);
                };
                break;
            case LEFT:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    System.out.println("SWIPE A SINISTRA, LISTA RICEVUTA: "+cells.toString());
                    //cellChecked[0][i] = cells.get(j);
                    //cellChecked[row+1][i] = cells.get(j);
                    cellChecked[i][0] = cells.get(j);
                };
                break;
            case UP:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    System.out.println("SWIPE IN ALTO, LISTA RICEVUTA: "+cells.toString());
                    cellChecked[0][i] = cells.get(j);
                };
                break;
            case DOWN:
                for(int i = firstIndex,j=0; i<lastIndex; i++,j++){
                    System.out.println("SWIPE IN BASSO, LISTA RICEVUTA: "+cells.toString());
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

    private class CalculateGeneration extends Thread {

        private int neighboursAlive(int i,int j){
            //calcolo il numero di vicini vivi
            int neighbours=0;
            if((i-1)>=0){
                if((j-1)>=0){
                    if(cellChecked[i-1][j-1]){
                        neighbours++;
                    }
                }

                if(cellChecked[i-1][j]){
                    neighbours++;
                }

                if((j+1)<column+2){
                    if(cellChecked[i-1][j+1]){
                        neighbours++;
                    }
                }
            }


            if((j-1)>=0){
                if(cellChecked[i][j-1]){
                    neighbours++;
                }
            }

            if((j+1)<column+2){
                if(cellChecked[i][j+1]){
                    neighbours++;
                }
            }

            if((i+1)<row+2){
                if((j-1)>=0){
                    if(cellChecked[i+1][j-1]){
                        neighbours++;
                    }
                }

                if(cellChecked[i+1][j]){
                    neighbours++;
                }

                if((j+1)<column+2){
                    if(cellChecked[i+1][j+1]){
                        neighbours++;
                    }
                }
            }

            return neighbours;
        }

        private void calculateNextGen(){
            boolean [][] tmp=new boolean[row+2][column+2];

            for(int i=0;i<column+2;i++){
                System.out.println("RIGA 0 " + cellChecked[0][i]);
                System.out.println("ULTIMA RIGA "+  cellChecked[row+1][i]);
            }

            for(int i=0;i<row+2;i++){
                System.out.println("COLONNA 0 " + cellChecked[i][0] );
                System.out.println("UTLIMA COLONNA " + cellChecked[i][column+1] );
            }

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

        @Override
        public void run() {
            boolean goOn=true;

            while(goOn){

                if(handler.isConnected()){
                    //invio ai miei vicini le celle
                    handler.sendCellsToOthers();

                    System.out.println("INVIATE LE CELLE ");

                    //controllo se posso proseguire (ovvero ho ricevuto le celle da tutti i vicini)
                    while(!handler.goOn() && handler.isConnected() && !handler.stopGame()){
                        // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    System.out.println("HO RICEVUTO LE CELLE");

                    //se il while termina perchè ho ricevuto le celle da tutti i device, calcolo la generazione successiva
                    if(handler.isConnected() && !handler.stopGame()) {
                        calculateNextGen(); //calcolo la generazione
                        System.out.println("Generazione numero " + numGen++);
                        System.out.println("HO CALCOLATO LA GENERAZIONE SUCCESSIVA");
                        handler.readyToContinue(); //invio un messaggio ai miei vicini con lo scopo di avvisarli che sono pronto a inviare le mie celle
                        System.out.println("HO COMUNICATO CHE SON PRONTO A CONTINUARE");
                        // faccio il while fino a quando tutti i miei vicini
                        // non hanno terminato di calcolare la propria generazione
                        while (!handler.readyToSendCells() && handler.isConnected()) {
                            // sleep per non tenere di continuo il lock ed evitare una possibile starvation
                            try {
                                Thread.sleep(20);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("GLI ALTRI SONO PRONTI A INVIARE");

                        if(handler.isConnected()){
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
                                pause();

                                if(handler.isConnected()){

                                    JSONObject message=new JSONObject();
                                    try {
                                        message.put("type","pause");
                                        message.put(PinchInfo.ADDRESS,ipAddress);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    handler.sendCommand(message);
                                }
                            }

                        }else{
                            postInvalidate();
                            goOn=false;
                            pause();
                        }
                    }else{
                        //altrimenti resetto le celle fantasma e calcolo la generazione successiva
                        resetGhostCells();

                        goOn=false;

                        pause();

                        if(handler.isConnected() && handler.stopGame()){

                            JSONObject message=new JSONObject();
                            try {
                                message.put("type","pause");
                                message.put(PinchInfo.ADDRESS,ipAddress);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            handler.sendCommand(message);
                        }
                    }
                }else {
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

            if(clear.compareAndSet(true,false)){
                clear();
            }
        }
    }
}
