package isac.gameoflife;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static boolean firstTime=true;
    private static Handler handler;
    private GridView gridView;
    private boolean portrait = true,onTable=false;

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

        Utils.setContext(this);
        gridView=new GridView(this);

        if(firstTime) {
            firstTime=false;

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

            SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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

                    if((inclination<=15/* || inclination>165*/)&&x>=(oldX-0.5)&&x<=(oldX+0.5)
                            &&y>=(oldY-0.5)&&y<=(oldY+0.5)){
                        if(!onTable) {
                            onTable = true;
                            System.out.println("sono sul tavolo Z: " + z + " X: " + x + " Y: " + y+" Inclination: "+inclination);
                        }
                    }else{
                        if(onTable) {
                            onTable = false;
                            System.out.println("non sono sul tavolo Z: " + z + " X: " + x + " Y: " + y+" Inclination: "+inclination);

                            if(handler.getConnectedDevice()!=0){
                                Toast.makeText(getApplicationContext(), "Schermo scollegato", Toast.LENGTH_SHORT).show();
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

        setContentView(gridView);
        gridView.setHandler(handler);
        gridView.setActivity(this);

    }

    public boolean isPortrait(){
        return this.portrait;
    }

}
