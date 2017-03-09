package isac.gameoflife;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class MainActivity extends AppCompatActivity {

    private static boolean firstTime=true;
    private GridView gridView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gridView=new GridView(this);
        setContentView(gridView);

        if(firstTime) {
            firstTime=false;
            new ServiceRegistrationDNS("gameOfLife");//.execute();
            new ServiceDiscoveryDNS().startDiscovery(new ServiceListener() {

                @Override
                public void serviceAdded(ServiceEvent event) {
                    System.out.println("Service added: " + event.getInfo());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getInfo());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Service resolved: " + event.getInfo());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_start:gridView.start();break;
            case R.id.action_pause:gridView.pause();break;
            case R.id.action_clean:gridView.clear();break;
        }

        return super.onOptionsItemSelected(item);
    }
}
