package isac.gameoflife;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Francesco on 07/03/2017.
 */

public class GridView extends View {

    private final static int SIZE=50;
    private int width,height,row,column,startX,startY,stopX,stopY;;
    private Paint whitePaint = new Paint();
    private boolean[][] cellChecked;
    //se uso i lock, si blocca il thread UI, meglio utilizzare AtomicBoolean che permette
    //di effettuare operazioni thread-safe sui booleani
    private AtomicBoolean started=new AtomicBoolean(false),clear=new AtomicBoolean(false);

    public GridView(Context context) {
        super(context);
        //imposto il colore delle celle
        whitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
    }

    public boolean isStarted(){
        return started.get();
    }

    public void start(){
        //se il gioco non è stato già avviato, lo avvio eseguendo
        //un async task (necessario in quanto il thread UI non si deve bloccare)
        //se l'espressione booleana è false (primo parametro), imposto a true la variabile e proseguo
        if(started.compareAndSet(false,true)){
            new CalculateGeneration().execute();
        }
    }

    //metto in pausa il gioco
    public void pause(){
        //se il gioco è in esecuzione, setto a false la variabile
        started.compareAndSet(true,false);
    }

    public void clear(){
        clear.set(true);
        pause();
    }

    //disegno la griglia e la popolo
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        int x=0;
        int y=0;

        //disegno delle righe per formare la griglia
        while(x<=width){
            canvas.drawLine(x,0,x,height,whitePaint);
            x+=SIZE;
        }

        while(y<=height){
            canvas.drawLine(0,y,width,y,whitePaint);
            y+=SIZE;
        }

        //Random rnd = new Random();

        //setto le cellule vive
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < column; j++) {
                if (cellChecked[i][j]) {
                    //Paint paint=new Paint();
                    //paint.setARGB(255,rnd.nextInt(256),rnd.nextInt(256),rnd.nextInt(256));

                    //disegno un rettangolo in corrispondenza della cellula viva
                    canvas.drawRect(i * SIZE, j * SIZE,(i + 1) * SIZE, (j + 1) * SIZE,whitePaint/*paint*/);
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
                System.out.println("Punto di inizio: "+event.getX()+" "+event.getY());
                startX=(int)event.getX();
                startY=(int)event.getY();
                return true;
            //l'utente sta muovendo il dito
            case (MotionEvent.ACTION_MOVE) :
                System.out.println("In movimento: "+event.getX()+" "+event.getY());
                return true;
            //l'utente ha rilasciato il dito
            case (MotionEvent.ACTION_UP) :
                System.out.println("Fine: "+event.getX()+" "+event.getY());
                stopX=(int)event.getX();
                stopY=(int)event.getY();

                //se la differenza delle coordinate di inizio e fine del movimento è minore di 3, allora
                //l'utente vuole "attivare" una cella della griglia. altrimenti, potrebbe essere uno swipe
                //per lo swipe controllare che il movimento sia lungo solo uno dei due assi e non entrambi
                // (altrimenti mi sto muovendo in diagonale)
                if (Math.abs(startX - stopX) <= 3 && Math.abs(startY - stopY) <= 3 && !started.get()) {
                    int column = (int) (event.getX() / SIZE);
                    int row = (int) (event.getY() / SIZE);

                    cellChecked[column][row] = !cellChecked[column][row];
                    //chiamo il metodo invalidate così forzo la chiamata del metodo onDraw
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

    //async task che si occupa del calcolo delle generazioni di cellule
    private class CalculateGeneration extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void...params) {
            boolean goOn=true;

            while(goOn){
                boolean [][] tmp=new boolean[row][column];
                for(int i=0;i<row;i++){
                    for(int j=0;j<column;j++){
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
                //forzo la chiamata del metodo onDraw
                postInvalidate();

                //se l'utente non ha messo in pausa il gioco
                if(started.get()){
                    //sleep di 1 secondo
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
            return null;
        }

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

                if((j+1)<column){
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

            if((j+1)<column){
                if(cellChecked[i][j+1]){
                    neighbours++;
                }
            }

            if((i+1)<row){
                if((j-1)>=0){
                    if(cellChecked[i+1][j-1]){
                        neighbours++;
                    }
                }

                if(cellChecked[i+1][j]){
                    neighbours++;
                }

                if((j+1)<column){
                    if(cellChecked[i+1][j+1]){
                        neighbours++;
                    }
                }
            }

            return neighbours;
        }

        protected void onPostExecute(Void param) {
            //una volta terminato il task, controllo
            //se l'utente ha richiesto un reset della griglia
            if(clear.compareAndSet(true,false)){
                cellChecked=new boolean[row][column];
                postInvalidate();
            }
        }
    }
}
