package com.grgmrr.maraudersmap;

import java.util.ArrayList;
import java.util.HashMap;

import localytics.android.LocalyticsSession;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class UserListActivity extends Activity {
	
	private final static String LOG = "UserListActivity";
	private final static int MAX_STALE_MINUTES = 120;
	
	private Person[] mPeople;
	private MaraudersMapAPI mAPI;
	
	private UIHandler mUIHandler;
	private Handler mBackgroundHandler;
	
	private ListView mListView;
	private SimpleAdapter mPeopleAdapter;
	private ProgressDialog mLoadingDialog;
	
	private boolean mDebugMode;
	
	private LocalyticsSession localyticsSession;
	private final static String TAG_GET_PEOPLE = "get_people";
	
	static final int MESSAGE_PEOPLE_UPDATE = 1;
	static final int MESSAGE_PEOPLE_NONE = 2;
	
	private class UIHandler extends Handler {
		
		private final static String LOG = "UserListUIHandler";
		
		private void handlePeopleUpdate(Message msg) {
			Log.v(LOG, "handlePeopleUpdate");
			ArrayList<HashMap<String, String>> list_elements = new ArrayList<HashMap<String, String>>();
	        HashMap<String, String> item;
	        for (Person person : mPeople) {
	        	if (person.getTimeDelta() < MAX_STALE_MINUTES) {
	                item = new HashMap<String, String>();
	                item.put("Name", person.getName());
	                //TODO greg 5/6/09 - Fix the layout instead of hackily adding time.
	                item.put("Place", person.getPlace() + " ("+person.getPrettyTime()+")");
	                list_elements.add(item);
	        	}
	        }
	        mPeopleAdapter = new SimpleAdapter(getApplicationContext(), list_elements, R.layout.row_person,
	                                new String[] { "Name", "Place" }, new int[] {R.id.nameTextView, R.id.placeTextView });
	        mListView.setAdapter(mPeopleAdapter);
	    	mLoadingDialog.dismiss();
		}
		
		private void handlePeopleNone(Message msg) {
			//TODO: Deal with zero people so there is an easy to tap refresh list item.
			/*ArrayList<HashMap<String, String>> empty_list = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("no_people", "no_people");
			empty_list.add(item);
			
			mPeopleAdapter = new SimpleAdapter(getApplicationContext(), empty_list, R.layout.row_no_person, new String[] {}, new int[] {});
			mListView.setAdapter(mPeopleAdapter);*/
			
			Toast.makeText(
        			getApplicationContext(), 
        			getApplicationContext().getString(R.string.error_no_people), 
        			Toast.LENGTH_LONG).show();
			mLoadingDialog.dismiss();
		}
		
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_PEOPLE_UPDATE:
                	handlePeopleUpdate(msg);
                	break;
                case MESSAGE_PEOPLE_NONE:
                	handlePeopleNone(msg);
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
	
	private Runnable GetPeopleRunnable = new Runnable() {
		private final static String LOG = "GetPeopleRunnable";
		public void run() {
			Log.v(LOG, "ran!");
	        mPeople = mAPI.getPeople();
	        if (mPeople.length == 0) {
	        	Message msg = Message.obtain(mUIHandler, MESSAGE_PEOPLE_NONE);
	        	mUIHandler.sendMessage(msg);
	        } else {
	        	Message msg = Message.obtain(mUIHandler, MESSAGE_PEOPLE_UPDATE);
	        	mUIHandler.sendMessage(msg);
	        }
	        Log.v(LOG, "finished!");
		}
	};
	  
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Instantiate the object
        this.localyticsSession = new LocalyticsSession(
                       this.getApplicationContext(),
                       getApplicationContext().getString(R.string.localytics_key),
                       savedInstanceState != null);
     
        this.localyticsSession.open();                // open the session
        this.localyticsSession.upload();      // upload any data
        
        buildBackgroundHandler();
        mUIHandler = new UIHandler();
        mAPI = new MaraudersMapAPI();
        
        mLoadingDialog = new ProgressDialog(this);
        
        mListView = (ListView) findViewById(R.id.list_view);
        if (mListView != null) {
        	mListView.setTextFilterEnabled(true);
        } else {
        	Log.e(LOG, "Couldn't find mListView?");
        }
        
        if (savedInstanceState == null) {
        	Log.v(LOG, "Getting People from the internet.");
        	this.localyticsSession.tagEvent(TAG_GET_PEOPLE);
        	showLoadingDialog();
        	mBackgroundHandler.post(GetPeopleRunnable);
        } else {
        	Log.v(LOG, "Getting People from Bundle.");
        	Person[] array = (Person[]) savedInstanceState.get("people");
			mPeople = array;
			if (mPeople.length > 0) {
				Message msg = Message.obtain(mUIHandler, MESSAGE_PEOPLE_UPDATE);
	        	mUIHandler.sendMessage(msg);
			} else {
				Log.v(LOG, "Bundle Empty! Getting People from the internet.");
	        	this.localyticsSession.tagEvent(TAG_GET_PEOPLE);
				showLoadingDialog();
				mBackgroundHandler.post(GetPeopleRunnable);
			}
        }
    }
    
    public void onResume() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	mDebugMode = prefs.getBoolean("debug_mode", false);
    	super.onResume();
    }
    
    @Override
    public void onPause() {
        this.localyticsSession.close();
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
        this.localyticsSession.upload();
        super.onDestroy();
    }
    
    private void showLoadingDialog() {
    	if (mLoadingDialog == null) {
    		mLoadingDialog = new ProgressDialog(getApplicationContext());
    	}
    	
    	Log.d(LOG, mLoadingDialog.toString());
    	
        mLoadingDialog.setTitle(R.string.dialog_loading_title);
        mLoadingDialog.setMessage(this.getText(R.string.dialog_loading_message));
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setIndeterminate(true);
        mLoadingDialog.show();
    }
    
    private static final int MENU_BINDER = 1;
    private static final int MENU_PREFERENCES = 2;
    private static final int MENU_REFRESH = 3;
    private static final int MENU_WIFI_TEST = 99;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	Log.d(LOG, "Menu Drawn!");
            	
    	menu.add(0, MENU_REFRESH, Menu.NONE, this.getString(R.string.menu_refresh)).setIcon(
        		android.R.drawable.ic_menu_recent_history);
        menu.add(0, MENU_BINDER, Menu.NONE, this.getString(R.string.menu_binder)).setIcon(
        		android.R.drawable.ic_menu_compass);
        menu.add(0, MENU_PREFERENCES, Menu.NONE, this.getString(R.string.menu_preferences)).setIcon(
        		android.R.drawable.ic_menu_preferences);
        
        //It's too late to remove menu items later, they get drawn the first time then is static.
        Log.d(LOG, "Debug mode is " + String.valueOf(mDebugMode));
        if (mDebugMode == true) {
        	menu.add(0, MENU_WIFI_TEST, Menu.NONE, this.getString(R.string.menu_wifi_test)).setIcon(
                android.R.drawable.ic_menu_preferences);
        }
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG, "Selecting a menu option: " + Integer.toString(item.getItemId()));
        switch (item.getItemId()) {
            case MENU_WIFI_TEST:
                this.startActivity(new Intent(this.getApplicationContext(), WifiTestActivity.class));
                break;
            case MENU_BINDER:
            	this.startActivity(new Intent(this.getApplicationContext(), BinderActivity.class));
            	break;
            case MENU_PREFERENCES:
            	this.startActivity(new Intent(this.getApplicationContext(), EditPreferencesActivity.class));
            	break;
            case MENU_REFRESH:
            	showLoadingDialog();
            	mBackgroundHandler.post(GetPeopleRunnable);
            	break;
            default:
                break;
        }
        return true;
    }
    
    public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.
    	Log.i(LOG, "onSaveInstanceState");
    	savedInstanceState.putSerializable("people", mPeople);
		super.onSaveInstanceState(savedInstanceState);
		Log.i(LOG, "ending onSaveInstanceState");
	}

}