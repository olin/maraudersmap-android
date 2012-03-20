package com.grgmrr.maraudersmap;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BinderActivity extends Activity {
    
    private final static String LOG = "BinderActivity";

    private UIHandler mUIHandler;
    private Handler mBackgroundHandler;
    
    private Button mBindButton;
    private TextView mResultTextView;
    
    private MaraudersMapAPI mMapAPI;
    
    private List<ScanResult> mScanResults;
    private Place[] mPlaces;
    private int mCurrentPlaceIndex;
    private String mUsername;
    
    private ProgressDialog mLoadingDialog;
    
    static final int MESSAGE_WIFI_SCAN_DONE = 1;
    static final int MESSAGE_MAP_PLACES_RETURNED = 2;
    static final int MESSAGE_MAP_PLACE_SET = 3;
    static final int MESSAGE_NO_USERNAME_ERROR = 4;
    static final int MESSAGE_PLACE_OTHER_ERROR = 5;
    static final int MESSAGE_GOTO_PREFERENCES = 6;
    static final int MESSAGE_SHOW_LOADING_DIALOG = 7;

    private class UIHandler extends Handler {
        
        private final static String LOG = "BinderUIHandler";
        
        private void handleWifiScanDone(Message msg) {
        	Log.v(LOG, "handleWifiScanDone");
        	if (mScanResults == null) {
        		Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.error_no_routers), Toast.LENGTH_LONG).show();
        		mScanResults = new ArrayList<ScanResult>();
        		Log.w(LOG, "No routers found!");
        	}
        }
        
        private void handleMapPlacesReturned(Message msg) {
        	//FIXME: Check if the number of places returned was 0. That probably means no wifi connection.
        	CharSequence[] items = new CharSequence[mPlaces.length + 1];
			for (int i = 0; i < mPlaces.length; i++) {
				items[i] = mPlaces[i].getName();
			}
			items[mPlaces.length] = getApplicationContext().getString(R.string.places_other);
			final CharSequence[] final_items = items;
			
			showPlacePickerDialog(final_items);
        }
        
        private void handleMapPlaceSet(Message msg) {
        	mResultTextView.setText(getApplicationContext().getString(R.string.binder_location_prefix) + " " + mPlaces[mCurrentPlaceIndex].getName());
        	dismissLoadingDialog();
        }
        
        private void handleNoUsernameError(Message msg) {
        	showNoUsernameError();
        }
        
        private void handlePlaceOtherError(Message msg) {
			showOtherPlaceDialog();
        }
        
        private void handleGotoPreferences(Message msg) {
        	Intent gotoPreferencesIntent = new Intent(getApplicationContext(), EditPreferencesActivity.class);
        	gotoPreferencesIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	getApplicationContext().startActivity(gotoPreferencesIntent);
        }
        
        private void handleShowLoadingDialog(Message msg) {
        	showLoadingDialog();
        }
                
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_WIFI_SCAN_DONE:
                    handleWifiScanDone(msg);
                    break;
                case MESSAGE_MAP_PLACES_RETURNED:
                	handleMapPlacesReturned(msg);
                	break;
                case MESSAGE_MAP_PLACE_SET:
                	handleMapPlaceSet(msg);
                	break;
                case MESSAGE_NO_USERNAME_ERROR:
                	handleNoUsernameError(msg);
                	break;
                case MESSAGE_PLACE_OTHER_ERROR:
                	handlePlaceOtherError(msg);
                	break;
                case MESSAGE_GOTO_PREFERENCES:
                	handleGotoPreferences(msg);
                	break;
                case MESSAGE_SHOW_LOADING_DIALOG:
                	handleShowLoadingDialog(msg);
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
        setContentView(R.layout.binder);
        Log.v(LOG, "OnCreate!");
        
        buildBackgroundHandler();
        mUIHandler = new UIHandler();
        mLoadingDialog = new ProgressDialog(this);
        mMapAPI = new MaraudersMapAPI();
        
        //FIXME: It's basically a huge hack that we do this onCreate and hope 
        // it finishes before the user requests a bind. How do we catch that
        // it was successful? -gmm
        mBackgroundHandler.post(UpdateScanResultsRunnable);
        
        mBindButton = (Button) this.findViewById(R.id.buttonBind);
        mBindButton.setOnClickListener(button_listener);
        mResultTextView = (TextView) this.findViewById(R.id.resultsTextView);
    }
    
    public void onResume() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = prefs.getString("username", null);
        super.onResume();
    }
    
    private void showOtherPlaceDialog() {
    	AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setIcon(R.drawable.icon);
        dialog.setTitle(R.string.dialog_place_other_title);
        dialog.setMessage(getApplicationContext().getString(R.string.dialog_place_other_body));
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getApplicationContext().getString(R.string.dialog_place_other_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	// Uhh, do nothing.
            }
        });
        dialog.show();
    }
    
    private void showNoUsernameError() {
    	Message msgPreferences = Message.obtain(mUIHandler, MESSAGE_GOTO_PREFERENCES);
    	
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setIcon(R.drawable.icon);
        dialog.setTitle(R.string.dialog_no_username_title);
        dialog.setMessage(getApplicationContext().getString(R.string.dialog_no_username_body));
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getApplicationContext().getString(R.string.dialog_no_username_positive_button), msgPreferences);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getApplicationContext().getString(R.string.dialog_no_username_negative_button), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	// Uhh, do nothing.
            }
        });
        dialog.show();
    }
    
    private void showPlacePickerDialog(CharSequence[] items) {
		//FIXME: Do we really have to get the context from the Button? -gmm
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getApplicationContext().getString(R.string.binder_location_prompt));			
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	mCurrentPlaceIndex = item;
		    	mBackgroundHandler.post(SetPlaceRunnable);
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
    }
    
    private void showLoadingDialog() {
    	if (mLoadingDialog == null) {
    		mLoadingDialog = new ProgressDialog(this);
    	}
    	
    	Log.d(LOG, mLoadingDialog.toString());
    	
        mLoadingDialog.setTitle(R.string.dialog_loading_title);
        mLoadingDialog.setMessage(this.getText(R.string.dialog_loading_message));
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setIndeterminate(true);
        mLoadingDialog.show();
    }
    
    private void dismissLoadingDialog() {
    	mLoadingDialog.dismiss();
    }
    
    private Runnable UpdateScanResultsRunnable = new Runnable() {
		private final static String LOG = "UpdateScanResultsRunnable";
		public void run() {
			Log.v(LOG, "ran!");
			WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			//FIXME: should better handle starting/getting scan results
			wifimanager.startScan();
			mScanResults = wifimanager.getScanResults();        
        	Message msg = Message.obtain(mUIHandler, MESSAGE_WIFI_SCAN_DONE);
        	mUIHandler.sendMessage(msg);
		}
	};
	
	private Runnable GetMapPlacesRunnable = new Runnable() {
		private final static String LOG = "GetMapPlacesRunnable";
		public void run() {
			Log.v(LOG, "ran!");
    		mPlaces = mMapAPI.getPlaces(mScanResults);
    		Message msg = Message.obtain(mUIHandler, MESSAGE_MAP_PLACES_RETURNED);
        	mUIHandler.sendMessage(msg);
		}
	};
	
	private Runnable SetPlaceRunnable = new Runnable() {
		private final static String LOG = "SetPlaceRunnable";
		public void run() {
			Log.v(LOG, "ran!");
			if (mCurrentPlaceIndex == mPlaces.length) {
				// This means the user picked "Other".
				Message msg = Message.obtain(mUIHandler, MESSAGE_PLACE_OTHER_ERROR);
	        	mUIHandler.sendMessage(msg);
			} else {
				Message msg = Message.obtain(mUIHandler, MESSAGE_SHOW_LOADING_DIALOG);
	        	mUIHandler.sendMessage(msg);
				
	    		mMapAPI.setPlace(mUsername, mPlaces[mCurrentPlaceIndex]);
	    		
	    		msg = Message.obtain(mUIHandler, MESSAGE_MAP_PLACE_SET);
	        	mUIHandler.sendMessage(msg);
			}
		}
	};
    
    public OnClickListener button_listener = new OnClickListener() {
    	private static final String LOG = "ButtonOnClickListener";
		public void onClick(View view) {
			Log.v(LOG, "onClick!");
			if ((mUsername == null) || (mUsername.equals(""))) {
				Message msg = Message.obtain(mUIHandler, MESSAGE_NO_USERNAME_ERROR);
	        	mUIHandler.sendMessage(msg);
			} else {
				mBackgroundHandler.post(UpdateScanResultsRunnable);
				mBackgroundHandler.post(GetMapPlacesRunnable);
			}
		}
	};
    
}