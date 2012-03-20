package com.grgmrr.maraudersmap;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

// I AM GOING TO PUNT ON THIS UNTIL MM MOBILE HAS A BETTER ACCURACY RATE

public class MMService extends Service {

	private final static String LOG = "MMService";
	
	private MaraudersMapAPI mMapAPI;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
}