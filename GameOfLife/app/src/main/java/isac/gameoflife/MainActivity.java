package isac.gameoflife;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    private static boolean firstTime=true;
    private static Handler handler;
    private GridView gridView;
    private boolean portrait = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay();
        int orientation = display.getRotation();
        if (orientation == Surface.ROTATION_90
                || orientation == Surface.ROTATION_270) {
            this.portrait = false;
        }

        gridView=new GridView(this);

        if(firstTime) {
            firstTime=false;
            Utils.setContext(this);
            handler=new Handler(gridView,this);

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
        }

        setContentView(gridView);
        gridView.setHandler(handler);
        gridView.setActivity(this);

    }

    public boolean isPortrait(){
        return this.portrait;
    }

}
