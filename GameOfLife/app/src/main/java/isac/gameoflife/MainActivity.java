package isac.gameoflife;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setContext(this);
        initialize();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        initialize();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Handler handler=gridView.getGameHandler();

        if(handler!=null){
            handler.closeDeviceCommunication();
            handler.closeConnection();
        }

        ((ViewGroup)gridView.getParent()).removeView(gridView);
    }

    private void initialize(){

        gridView=new GridView(this);
        setContentView(gridView);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(new SensorEventListener() {

            private float oldX=Float.MAX_VALUE-0.5f,oldY=Float.MAX_VALUE-0.5f;
            private boolean onTable=false;

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

                        Handler handler=gridView.getGameHandler();

                        if(handler!=null&&handler.isConnected()){
                            Toast.makeText(getApplicationContext(), "Schermo scollegato", Toast.LENGTH_SHORT).show();
                            handler.closeDeviceCommunication();
                        }
                    }
                }

                oldX=x;
                oldY=y;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

        }, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

}
