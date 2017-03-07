package isac.gameoflife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * Created by Francesco on 07/03/2017.
 */

public class GridView extends View {

    private final static int SIZE=50;
    private int width,height,row,column,startX,startY,stopX,stopY;;
    private Paint whitePaint = new Paint();
    private boolean[][] cellChecked;

    public GridView(Context context) {
        super(context);
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int x=0;//SIZE;
        int y=0;//SIZE;

        while(x<=width){
            canvas.drawLine(x,0,x,height,whitePaint);
            x+=SIZE;
        }

        while(y<=height){
            canvas.drawLine(0,y,width,y,whitePaint);
            y+=SIZE;
        }

        //Random rnd = new Random();

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                if (cellChecked[i][j]) {
                    //Paint paint=new Paint();
                    //paint.setARGB(255,rnd.nextInt(256),rnd.nextInt(256),rnd.nextInt(256));
                    canvas.drawRect(i * SIZE, j * SIZE,(i + 1) * SIZE, (j + 1) * SIZE,whitePaint/*paint*/);
                }
            }
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      /*  if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int column = (int)(event.getX() / SIZE);
            int row = (int)(event.getY() / SIZE);

            cellChecked[column][row] = !cellChecked[column][row];
            invalidate();
        }*/

        int action = MotionEventCompat.getActionMasked(event);


        switch(action) {
            case (MotionEvent.ACTION_DOWN) :
                System.out.println("Punto di inizio: "+event.getX()+" "+event.getY());
                startX=(int)event.getX();
                startY=(int)event.getY();
                return true;
            case (MotionEvent.ACTION_MOVE) :
                System.out.println("In movimento: "+event.getX()+" "+event.getY());
                return true;
            case (MotionEvent.ACTION_UP) :
                System.out.println("Fine: "+event.getX()+" "+event.getY());
                stopX=(int)event.getX();
                stopY=(int)event.getY();
               // System.out.println(startX+" "+stopX);
               // System.out.println(startY+" "+stopY);
                if(Math.abs(startX-stopX)<=3&&Math.abs(startY-stopY)<=3){
                    int column = (int)(event.getX() / SIZE);
                    int row = (int)(event.getY() / SIZE);

                    cellChecked[column][row] = !cellChecked[column][row];
                    invalidate();
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
            row = width % SIZE == 0 ? width / SIZE : (width / SIZE) + 1;
            column = height % SIZE == 0 ? height / SIZE : (height / SIZE) + 1;
            cellChecked = new boolean[row][column];
        }
    }
}
