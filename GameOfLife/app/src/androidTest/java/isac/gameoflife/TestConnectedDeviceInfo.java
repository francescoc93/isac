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
                PinchInfo.Direction.RIGHT,1200,400,5,5,5,5,1200,398,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        assertTrue(device.getNameQueueReceiver().equals("Device2Device1"));
        assertTrue(device.getNameQueueSender().equals("Device1Device2"));
        assertTrue(device.getMyDirection().equals(PinchInfo.Direction.RIGHT));
    }

    @Test
    public void calculateIndex(){

        //TODO: DA COMPLETARE

        //CASO 0 GRADI
        ConnectedDeviceInfo device=new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.LEFT,
                PinchInfo.Direction.RIGHT,1200,400,5,5,5,5,1200,400,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device.calculateInfo();
        int firstIndex=device.getIndexFirstCell();
        int lastIndex=device.getIndexLastCell();

        assertTrue(firstIndex==1);
        assertTrue(lastIndex==10);
        assertTrue(device.getCellsValues().size()==(lastIndex-firstIndex)+1);

        //CASO 90 GRADI
        device=new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.RIGHT,
                PinchInfo.Direction.RIGHT,1200,800,5,5,5,5,1200,400,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device.calculateInfo();
        firstIndex=device.getIndexFirstCell();
        lastIndex=device.getIndexLastCell();

        assertTrue(firstIndex==1);
        assertTrue(lastIndex==10);
        assertTrue(device.getCellsValues().size()==(lastIndex-firstIndex)+1);


        //CASO 180 GRADI
        device=new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.DOWN,
                PinchInfo.Direction.RIGHT,400,1200,5,5,5,5,1200,400,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device.calculateInfo();
        firstIndex=device.getIndexFirstCell();
        lastIndex=device.getIndexLastCell();

        assertTrue(firstIndex==1);
        assertTrue(lastIndex==10);
        assertTrue(device.getCellsValues().size()==(lastIndex-firstIndex)+1);


        //CASO 270 GRADI
        device=new ConnectedDeviceInfo(0.5f,PinchInfo.Direction.UP,
                PinchInfo.Direction.RIGHT,800,1200,5,5,5,5,1200,400,"Device1Device2",
                "Device2Device1",gridView,240,240,240,240);

        device.calculateInfo();
        firstIndex=device.getIndexFirstCell();
        lastIndex=device.getIndexLastCell();

        assertTrue(firstIndex==1);
        assertTrue(lastIndex==10);
        assertTrue(device.getCellsValues().size()==(lastIndex-firstIndex)+1);
    }
}
