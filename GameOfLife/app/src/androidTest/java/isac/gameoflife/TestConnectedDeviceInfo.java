package isac.gameoflife;


import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import static junit.framework.Assert.assertTrue;


/**
 * Created by Francesco on 23/04/2017.
 */

@RunWith(AndroidJUnit4.class)
public class TestConnectedDeviceInfo {

    private static boolean flag=true;
    private static GridView gridView;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);


    @Before
    public void setup(){
        if(flag){
            flag=false;

            gridView=(GridView) ((ViewGroup) mActivityRule.getActivity()
                    .findViewById(android.R.id.content)).getChildAt(0);
        }
    }

    @Test
    public void testGetter(){
        ConnectedDeviceInfo device=new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.RIGHT,1080,400,5,5,5,5,1080,398,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        assertTrue(device.getNameQueueReceiver().equals("Device2Device1"));
        assertTrue(device.getNameQueueSender().equals("Device1Device2"));
        assertTrue(device.getMyDirection().equals(PinchInfo.Direction.RIGHT));
    }

    @Test
    public void calculateIndex(){

        //TODO: DA COMPLETARE

        //CASO 0
        ConnectedDeviceInfo device=new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.RIGHT,0,400,2.71f,4.76f,2.44f,4.37f,1080,800,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        ConnectedDeviceInfo device1 = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.RIGHT,
                PinchInfo.Direction.LEFT,1080,800,2.44f,4.37f,2.71f,4.76f,0,400,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);
        assertTrue(device.getCellsValues().size() == device1.getCellsValues().size());


        //CASO 270
        device = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.DOWN,0,400,2.71f,4.76f,2.44f,4.37f,500,1800,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device1 = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.DOWN,
                PinchInfo.Direction.LEFT,500,1800,2.44f,4.37f,2.71f,4.76f,0,1000,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);
        assertTrue(device.getCellsValues().size() == device1.getCellsValues().size());

        //CASO 180
        device = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.LEFT,0,400,2.71f,4.76f,2.44f,4.37f,0,700,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device1 = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.LEFT,0,700,2.44f,4.37f,2.71f,4.76f,0,400,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);
        assertTrue(device.getCellsValues().size() == device1.getCellsValues().size());


        //CASO 90
        device = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.UP,300,0,2.71f,4.76f,2.44f,4.37f,0,700,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device1 = new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.UP,
                PinchInfo.Direction.LEFT,0,700,2.44f,4.37f,2.71f,4.76f,300,0,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);
        assertTrue(device.getCellsValues().size() == device1.getCellsValues().size());


    }
}
