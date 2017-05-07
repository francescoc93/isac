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
    //First pair is timestamp and direction, second is x and y coordinates
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

    /**
     *
     * @return the exact physical pixels per inch of the screen in the X dimension
     */
    public float getXDpi() {return getResources().getDisplayMetrics().xdpi; }

    /**
     *
     * @return the exact physical pixels per inch of the screen in the Y dimension
     */
    public float getYDpi() {return getResources().getDisplayMetrics().ydpi; }

    /**
     *
     * @return  infos about the last swipe performed
     */
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

    /**
     *
     * @return if the game is running
     */
    public boolean isStarted(){
        return started.get();
    }

    /**
     * Starts the game
     */
    public void start(){
        lockAction.lock();

        if(started.compareAndSet(false,true)){

            if(calculateGeneration!=null){
                if(calculateGeneration.isAlive()){
                    //before starting a new generation, waits the end of the previous one
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

    /**
     * Pauses the game
     */
    public void pause(){
        lockAction.lock();

        if(started.get()){
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Pause", Toast.LENGTH_SHORT).show();
                }
            });
        }

        started.compareAndSet(true,false);

        lockAction.unlock();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int count=0;

        //draws the grid

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

        //sets the alive cells
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                if (cellChecked[i+1][j+1]) {
                    canvas.drawRect(j * SIZE, i * SIZE,(j + 1) * SIZE, (i + 1) * SIZE,whitePaint);
                }
            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);

        switch(action) {
            //the finger of the user is on the screen
            case (MotionEvent.ACTION_DOWN) :
                touchDownMs = System.currentTimeMillis();
                //get the coordinates
                startX=(int)event.getX();
                startY=(int)event.getY();
                return true;
            case (MotionEvent.ACTION_MOVE) :
                return true;
            //the user has just released the finger from the screen
            case (MotionEvent.ACTION_UP) :
                //get the coordinates
                int stopX=(int)event.getX();
                int stopY=(int)event.getY();


                /*if the duration of the pressure on the screen is greater than TIME_DOUBLE_TAP
                it may be a double tap (start/pause the game), otherwise the user has just set the state of
                one cell
                */
                if ((System.currentTimeMillis() - touchDownMs) > TIME_DOUBLE_TAP) {
                    numberOfTaps = 0;
                    lastTapTimeMs = 0L;

                    if (Math.abs(startX - stopX) < 10 && Math.abs(startY - stopY) < 10 && !started.get()) {
                        setCell(event.getX(),event.getY());
                    } else {
                        evaluateSwipe(stopX,stopY);
                    }
                }

                //evaluate if the user has just performed a swipe
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
            //get the width and the height of the grid in inches
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

            //create the matrix of cells
            cellChecked = new boolean[row+2][column+2];
        }

    }

    /**
     *
     * @return the matrix of cells
     */
    public boolean[][] getCellMatrix(){
        return this.cellChecked;
    }

    /**
     *
     * @return the handler
     */
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

    /**
     * Send the message of swipe to all device that are running the application
     * @param timeStamp time when the swipe was performed
     * @param direction direction of the swipe
     * @param x X coordinate where the swipe is ended
     * @param y Y coordinate where the swipe is ended
     */
    private void sendBroadcastMessage(Long timeStamp,PinchInfo.Direction direction,int x,int y){
        lockInfoSwipe.lock();

        infoSwipe=new Pair<>(new Pair<>(timeStamp,direction),new Pair<>(x,y));

        lockInfoSwipe.unlock();

        handler.sendBroadcastMessage(new PinchInfo(ipAddress, direction,x,y,timeStamp, width, height,getXDpi(),getYDpi()).toJSON());
    }

    /**
     * Set the state of the cell
     * @param x X coordinate where the user has pressed
     * @param y Y coordinate where the user has pressed
     */
    private void setCell(float x,float y){
        int column = (int) (x / SIZE);
        int row = (int) (y / SIZE);

        if(column<this.column && row<this.row) {
            column+=1;
            row+=1;

            cellChecked[row][column] = !cellChecked[row][column];

            //force the redraw of the grid
            invalidate();
        }
    }

    /**
     * Evaluate if the movement of the finger of the user is a swipe
     * @param stopX X coordinate where the movement is ended
     * @param stopY Y coordinate where the movement is ended
     */
    private void evaluateSwipe(int stopX,int stopY){
        long timeStamp = System.currentTimeMillis();
        PinchInfo.Direction direction=null;

        if (Math.abs(startX - stopX) >=10 && Math.abs(startY - stopY) <= 80){
            //swipe on X axis
            if((stopX - startX) > 0){
                direction=PinchInfo.Direction.RIGHT;
            } else if ((stopX - startX)<0){
                direction=PinchInfo.Direction.LEFT;
            }

            sendBroadcastMessage(timeStamp,direction,stopX,stopY);

        } else if (Math.abs(startX - stopX) <=80 && Math.abs(startY - stopY) >= 10){
            //swipe on Y axis
            if((stopY - startY) > 0){
                direction=PinchInfo.Direction.DOWN;
            } else if ((stopY - startY)<0){
                direction=PinchInfo.Direction.UP;
            }

            sendBroadcastMessage(timeStamp,direction,stopX,stopY);
        }
    }

    /**
     * Evaluate if the user has performed a double tap
     */
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
                //stop the game
                pause();
            }else{
                //begin a new game
                //if the device is connected with someone else
                if(handler.isConnected()){
                    //send the message of start
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

    /**
     * Inner class that calculates the generations of the game
     */
    private class CalculateGeneration extends Thread {

        /**
         * Count how many neighbors are alive
         * @param i Row of the matrix
         * @param j Column of the matrix
         * @return the number of live neighbors
         */
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

        /**
         * Reset the state of the cells sent by the neighbors
         */
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

        /**
         * Calculate the next generation of cells
         */
        private void calculateNextGen(){
            boolean [][] tmp=new boolean[row+2][column+2];

            for(int i=1;i<row+1;i++){
                for(int j=1;j<column+1;j++){
                    int neighbours=neighboursAlive(i,j);

                    if(cellChecked[i][j]) {
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

            cellChecked=tmp;
        }

        /**
         * Send the cells to the neighbors and i wait to receive the cells from they're
         */
        private void sendAndWaitCells(){
            //send the cells
            handler.sendCellsToOthers();
            //waits until receiving all the cells or receiving a "pause" command or the device is not connected with another one anymore
            while(!handler.goOn() && handler.isConnected() && !handler.stopGame()){
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Sends a message that indicates that the application is ready to continue and waits to receive the same
         * message from his neighbours
         */
        private void sendAndWaitOthers(){
            //send the message
            handler.readyToContinue();

            //wait until receiving all the messages or receiving a "pause" command or the device is not connected with another one anymore
            while (!handler.readyToSendCells() && handler.isConnected() && !handler.stopGame()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            handler.resetReadyReceived();

            //force redraw of the grid
            postInvalidate();
        }

        /**
         * Sends to the neighbours a message that tells to stop the game
         */
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

        /**
         * Wait (if necessary) to receive all the cells from the neighbours and reset their flag
         */
        private void receiveCellsAndReset(){
            while(handler.isConnected() && !handler.cellsReceived()){
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

                    if(handler.isConnected() && !handler.stopGame()) {
                        //if the device receives the cells from all the neighbours and it doesn't receive the command of stop
                        //reset the flag of sent cells
                        handler.resetCellsReceived();
                        //calculate the next generation
                        calculateNextGen();
                        //send to the neighbours the will to continue with the next generation and waits
                        //to receive from all the neighbours the same message
                        sendAndWaitOthers();

                        if(handler.isConnected() && !handler.stopGame()){
                            //the device receives the messages from all the neighbours and it doesn't receive the command of stop

                            //check if the user has stopped the game
                            if(started.get()){
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }else{
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
                    //calculate the next generation of cells
                    calculateNextGen();
                    //force redraw of the grid
                    postInvalidate();

                    //check if it is necessary to stop the game
                    if(started.get()){
                        try {

                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        goOn=false;
                    }
                }
            }
        }
    }
}
