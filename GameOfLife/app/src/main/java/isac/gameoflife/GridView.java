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
    private String ipAddress;
    private MainActivity activity;
    private AtomicBoolean started=new AtomicBoolean(false);
    private Long lastTapTimeMs,touchDownMs;
    //First pair is timestamp and direction, second is x and y coordinates
    private Pair<Pair<Long,PinchInfo.Direction>,Pair<Integer,Integer>> infoSwipe;
    private ReentrantLock lockInfoSwipe,lockAction,lockHandler;
    private Thread thread;
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
        thread =null;
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

            if(thread!=null){
                if(thread.isAlive()){
                    //before starting a new generation, waits the end of the previous one
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            thread =new Thread(){
                @Override
                public void run(){
                    calculateGeneration.calculate();
                }
            };

            thread.start();

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

        boolean [][] cellChecked=calculateGeneration.getCells();
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

            calculateGeneration=new CalculateGeneration(row,column,this);
            handler=new Handler(this,calculateGeneration,activity,width,height,Utils.pixelsToInches(SIZE,getXDpi()));

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
        }

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

    public CalculateGeneration getCalculateGeneration(){
        lockHandler.lock();
        CalculateGeneration tmp=calculateGeneration;
        lockHandler.unlock();

        return tmp;
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

            calculateGeneration.setCell(row,column);

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
}
