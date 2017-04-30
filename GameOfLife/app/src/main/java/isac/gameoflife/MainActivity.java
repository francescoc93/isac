package isac.gameoflife;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setContext(this);
        gridView=new GridView(this);

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
