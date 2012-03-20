package com.grgmrr.maraudersmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class WifiTestActivity extends Activity {
    
    private final static String LOG = "WifiTestActivity";

    private UIHandler mUIHandler;
    private Handler mBackgroundHandler;
    
    private ListView mListView;
    private SimpleAdapter mRouterAdapter;
    
    private List<ScanResult> mScanResults;
    
    static final int MESSAGE_WIFI_SCAN_DONE = 1;

    private class UIHandler extends Handler {
        
        private final static String LOG = "WifiTestUIHandler";
        
        private void handleWifiScanDone(Message msg) {
        	Log.v(LOG, "handleWifiScanDone");
        	List<ScanResult> scan_results = mScanResults;
        	if (scan_results == null) {
        		Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.error_no_routers), Toast.LENGTH_LONG).show();
        		scan_results = new ArrayList<ScanResult>();
        		Log.w(LOG, "No routers found!");
        	}
    		ArrayList<HashMap<String, String>> list_elements = new ArrayList<HashMap<String, String>>();
    		for (ScanResult scanresult : scan_results) {
        		HashMap<String, String> item = new HashMap<String, String>();
                item.put("ssid", scanresult.SSID);
                item.put("bssid", scanresult.BSSID);
                item.put("db", Integer.toString(scanresult.level));
                list_elements.add(item);
            }
            mRouterAdapter = new SimpleAdapter(getApplicationContext(), list_elements, R.layout.row_router,
                    new String[] { "ssid", "bssid", "db" }, new int[] {R.id.ssidTextView, R.id.bssidTextView, R.id.dbTextView });
            mListView.setAdapter(mRouterAdapter);
        }
                
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_WIFI_SCAN_DONE:
                    handleWifiScanDone(msg);
                    break;
                default:
                    break;
            }
        }
    };
    
    private void buildBackgroundHandler() {
        // Start up the thread running expensive requests.
        HandlerThread thread = new HandlerThread(LOG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper bgLooper = thread.getLooper();
        mBackgroundHandler = new Handler(bgLooper);
    }
      
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_test);
        
        buildBackgroundHandler();
        mUIHandler = new UIHandler();
        
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setTextFilterEnabled(true);
        
        mBackgroundHandler.post(UpdateScanResultsRunnable);
    }
    
    private Runnable UpdateScanResultsRunnable = new Runnable() {
		private final static String LOG = "UpdateScanResultsRunnable";
		public void run() {
			Log.v(LOG, "ran!");
			WifiManager wifimanager = (WifiManager) getBaseContext().getSystemService(Context.WIFI_SERVICE);
			//FIXME: should better handle starting/getting scan results
			wifimanager.startScan();
			mScanResults = wifimanager.getScanResults();        
        	Message msg = Message.obtain(mUIHandler, MESSAGE_WIFI_SCAN_DONE);
        	mUIHandler.sendMessage(msg);
		}
	};

}