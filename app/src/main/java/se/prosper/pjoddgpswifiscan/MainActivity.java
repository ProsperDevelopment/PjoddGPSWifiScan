package se.prosper.pjoddgpswifiscan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import se.prosper.pjoddgpswifiscan.Adapter.ListAdapter;
import se.prosper.pjoddgpswifiscan.Javafiles.ImportDialog;

public class MainActivity extends Activity implements OnClickListener {
    Button wifiToggleButton;
    TextView wifiStatus;
    WifiManager wifiManager;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    List<String> listOfProvider;
    ListAdapter adapter;
    ListView listViwProvider;
    static final int REFRESH_TIME_INTERVAL = 2000;


    private android.os.Handler handler = new android.os.Handler();

    ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();

    private Runnable wifiScanRunningTask = new Runnable() {
        public void run() {
            Log.d("wifi","startScan");
            wifiManager.startScan();
            handler.postDelayed(wifiScanRunningTask, REFRESH_TIME_INTERVAL);
        }
    };


    // submit task to threadpool:
    Future runningTaskFuture = threadPoolExecutor.submit(wifiScanRunningTask);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listOfProvider = new ArrayList<String>();

        // setup layout components
        listViwProvider = (ListView) findViewById(R.id.list_view_wifi);
        wifiToggleButton = (Button) findViewById(R.id.btn_wifi);
        wifiStatus = (TextView) findViewById(R.id.wifiSatus);

        // wifi scanned value broadcast receiver
        receiverWifi = new WifiReceiver();


        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiToggleButton.setOnClickListener(this);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager.isWifiEnabled() == true) {
            wifiStatus.setText("ON");
            handler.post(wifiScanRunningTask);

        } else {
            wifiStatus.setText("OFF");

        }

	    	/*opening a detail dialog of provider on click   */
        listViwProvider.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                ImportDialog action = new ImportDialog(MainActivity.this, (wifiList.get(position)).toString());
                action.showDialog();
            }
        });
    }


    @Override
    public void onClick(View arg0) {


        // toggle wifi / on off

        if (wifiManager.isWifiEnabled() == true) {
            wifiManager.setWifiEnabled(false);
            wifiStatus.setText("OFF");

            listViwProvider.setVisibility(ListView.GONE);

            // stop wifi scan.
            runningTaskFuture.cancel(true);
            handler.removeCallbacks(wifiScanRunningTask);

        }


        else if (wifiManager.isWifiEnabled() == false) {
            wifiManager.setWifiEnabled(true);
            wifiStatus.setText("ON");
            listViwProvider.setVisibility(ListView.VISIBLE);
            // start wifi scan
            handler.post(wifiScanRunningTask);

        }
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiverWifi);
    }

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiReceiver extends BroadcastReceiver {

        // This method call when number of wifi connections changed
        public void onReceive(Context c, Intent intent) {
            wifiList = wifiManager.getScanResults();

            Log.d("Wifi", "ScanComplete");
			    /* sorting of wifi provider based on level */
            Collections.sort(wifiList, new Comparator<ScanResult>() {
                @Override
                public int compare(ScanResult lhs, ScanResult rhs) {
                    return (lhs.level > rhs.level ? -1
                            : (lhs.level == rhs.level ? 0 : 1));
                }
            });
            listOfProvider.clear();
            String providerName;
            for (int i = 0; i < wifiList.size(); i++) {
				/* to get SSID and BSSID of wifi provider*/
                providerName = (wifiList.get(i).SSID).toString()
                        + "\n" + (wifiList.get(i).BSSID).toString();
                listOfProvider.add(providerName);
            }
			/*setting list of all wifi provider in a List*/
            adapter = new ListAdapter(MainActivity.this, listOfProvider);
            listViwProvider.setAdapter(adapter);

            adapter.notifyDataSetChanged();
        }
    }
}
