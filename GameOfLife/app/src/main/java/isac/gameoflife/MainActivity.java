package isac.gameoflife;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    //private static boolean firstTime=true;
    private /*static*/ GridView gridView;
   // private boolean portrait = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       /* Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay();

        int orientation = display.getRotation();

        if (orientation == Surface.ROTATION_90
                || orientation == Surface.ROTATION_270) {
            this.portrait = false;
        }*/

        Utils.setContext(this);

        //if(firstTime) {
            //firstTime=false;
            gridView=new GridView(this);
        /*}else{
            ((ViewGroup)gridView.getParent()).removeView(gridView);
        }
*/

        setContentView(gridView);
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        gridView=new GridView(this);
        setContentView(gridView);
    }

    @Override
    protected void onStop() {
        super.onStop();

        gridView.closeAllCommunication();
        gridView.closeConnection();

        ((ViewGroup)gridView.getParent()).removeView(gridView);
    }

}
