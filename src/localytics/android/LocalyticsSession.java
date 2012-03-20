/**
 * LocalyticsSession.java
 * Copyright (C) 2009 Char Software Inc., DBA Localytics
 * 
 *  This code is provided under the Localytics Modified BSD License.
 *  A copy of this license has been distributed in a file called LICENSE
 *  with this source code.  
 *  
 *  Please visit www.localytics.com for more information.
 */

package localytics.android;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.util.Locale;
import java.util.UUID;

/**
 * The class which manages creating, collecting, & uploading a Localytics session.
 * Please see the following guides for information on how to best use this
 * library, sample code, and other useful information:
 * <ul>
 * <li><a href="http://wiki.localytics.com/index.php?title=Developer's_Integration_Guide">Main Developer's Integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_2_Minute_Integration">Android 2 minute integration Guide</a></li>
 * <li><a href="http://wiki.localytics.com/index.php?title=Android_Integration_Guide">Android Integration Guide</a></li>
 * </ul>
 * <p>
 * Permissions required or recommended for this class:
 * <ul>
 * <li>android.permission.INTERNET</li> - Required.  Necessary to upload data to the webservice.</li>
 * <li>android.permission.ACCESS_WIFI_STATE</li> - Optional.  Without this users connecting via WIFI will show up as
 * having a connection type of 'unknown' on the webservice</li> 
 * </ul>
 * 
 * <strong>Best Practices</strong>
 * <ul>
 * <li>Instantiate the LocalyticsSession object in onCreate.</li>
 * <li>Open your session and begin your uploads in onCreate. This way the
 * upload has time to complete and it all happens before your users have a
 * chance to begin any data intensive actions of their own.</li>
 * <li>Close the session in onPause.  This is the only terminating function
 * which is guaranteed to be called.  The final close is the only one
 * considered so worrying about activity re-entrance is not a problem.</li>
 * <li>Do not call any Localytics functions inside a loop.  Instead, calls
 * such as <code>tagEvent</code> should follow user actions.  This limits the
 * amount of data which is stored and uploaded.</li>
 * <li>Do not use multiple LocalticsSession objects to upload data with 
 * multiple application keys.  This can cause invalid state.</li>
 * </ul>
 * @author Localytics
 * @version 1.0
 */
public final class LocalyticsSession
{	
	////////////////////////////////////////
	// Member Variables ////////////////////
	////////////////////////////////////////	
	private String _localyticsDirPath;		// Path for this app's Localytics Files
	private String _sessionFilename = null; // Filename for this session
	private String _sessionUUID;			// Unique identifier for this session.
	private String _applicationKey;         // Unique identifier for the instrumented application    
		
	private Context _appContext;			// The context used to access device resources
    	
	private boolean _isSessionOpen = false;	// Whether or not this session has been opened.
	private boolean _isSessionDone = false; // Whether or not the session is done
	
    private static boolean _isUploading = false;  // Only allow one instance of the app to upload at once.   	
    private static boolean _isOptedIn = false;    // Optin/out needs to be shared by all instances of this class. 
    
	////////////////////////////////////////
	// Constants ///////////////////////////
	////////////////////////////////////////      
	private static final String CLIENT_VERSION = "1.0";  // The version of this library.
	private static final int MAX_NUM_SESSIONS = 5; 		 // Number of sessions to store on the disk
	
	// Filename and directory constants.
    private static final String LOCALYTICS_DIR       = "localytics";
    private static final String SESSION_FILE_PREFIX  = "s_";
    private final static String UPLOADER_FILE_PREFIX = "u_";
    private static final String OPTOUT_FILNAME       = "opted_out";
    private static final String DEVICE_ID_FILENAME   = "device_id";    
    private static final String SESSION_ID_FILENAME  = "session_id";
    
    // All session opt-in / opt-out events need to be written to same place to gaurantee ordering on the server.
    private static final String OPT_SESSION = LocalyticsSession.SESSION_FILE_PREFIX + "opt_session";
    
    // The tag used for identifying Localytics Log messages.
    private static final String LOG_TAG = "Localytics_Session";
    
	////////////////////////////////////////
	// Public Methods //////////////////////
	////////////////////////////////////////	
	/**
	 * Creates the Localytics Object.  If Localytics is opted out at the time
	 * this object is created, no data will be collected for the lifetime of
	 * this session.
	 * @param appContext The context used to access resources on behalf of the app.  
	 * It is recommended to use <code>getApplicationContext</code> to avoid the potential
	 * memory leak incurred by maintaining references to activities.
	 * @param applicationKey The key unique for each application generated
	 * at www.localytics.com
	 * @param reconnect Whether this session is reconnecting to the session
	 * which was just closed.  If this is created in <code>onCreate</code> then
	 * this should be set to <code>(savedInstanceState != null)</code> This should be
	 * set to true in any activity launched after the main activity which instantiates a LocalyticsSession. 
	 */
	public LocalyticsSession(final Context appContext, 
							 final String applicationKey,  
							 final boolean reconnect)
	{
		this._appContext = appContext;		
		this._applicationKey = applicationKey;		
		this._localyticsDirPath = appContext.getFilesDir() + "/" 
						            + LocalyticsSession.LOCALYTICS_DIR + "/";
				
		// If there is an opt-out file, everything is opted out.
		File optOutFile = new File(this._localyticsDirPath + LocalyticsSession.OPTOUT_FILNAME);
		if(optOutFile.exists())
		{
			LocalyticsSession._isOptedIn = false;
			return;
		}
		
		// Otherwise, everything is opted in.
		LocalyticsSession._isOptedIn = true;
		
		// The Android activity model causes situations in which an activity may be
		// destroyed and recreated.  If this is the case, resume the destroyed session
		// rather than create a new one.
		if(reconnect)
		{			
			this._sessionUUID = getStoredSessionUUID();
			if(this._sessionUUID != null)
			{			
				this._isSessionOpen = true;
				this._sessionFilename = LocalyticsSession.SESSION_FILE_PREFIX + this._sessionUUID;
			}
		}
	}
	
	/**
	 * Sets the Localytics Optin state for this application.  This
	 * call is not necessary and is provided for people who wish to allow
	 * their users the ability to opt out of data collection.  It can be
	 * called at any time.  Passing false causes all further data collection
	 * to stop, and an opt-out event to be sent to the server so the user's
	 * data is removed from the charts.
	 * <br>
	 * There are very serious implications to the quality of your data when
	 * providing an opt out option.  For example, users who have opted out will appear 
	 * as never returning, causing your new/returning chart to skew.  
	 * <br>
	 * If two instances of the same application are running, and one
	 * is opted in and the second opts out, the first will also become opted
	 * out, and neither will collect any more data.
	 * <br>
	 * If a session was started while the app was opted out, the session open
	 * event has already been lost.  For this reason, all sessions started
	 * while opted out will not collect data even after the user opts back in
	 * or else it taints the comparisons of session lengths and other metrics.
	 * @param optedIn True if the user wishes to be opted in, false if they
	 * wish to be opted out and have all their Localytics data deleted.
	 */
	public void setOptIn(final boolean optedIn)
	{
		// Do nothing if optin is unchanged
		if(optedIn == LocalyticsSession._isOptedIn)
		{
			return;
		}		
		
		LocalyticsSession._isOptedIn = optedIn;		
		File fp;
		
		if(optedIn == true)
		{
			// To opt in, delete the opt out file if it exists.
			fp = new File(this._localyticsDirPath + LocalyticsSession.OPTOUT_FILNAME);			
			fp.delete();
			
			createOptEvent(true);
		}
		else
		{
			// Create the opt-out file.  If it can't be written this is fine because
			// it means session files can't be written either so the user is effectively opted out.
			fp = new File(this._localyticsDirPath);				
			fp.mkdirs();
			fp = new File(this._localyticsDirPath + LocalyticsSession.OPTOUT_FILNAME);
			try 
			{
				fp.createNewFile();
			} 
			catch (IOException e) { }
			
			createOptEvent(false);
		}		
	}
	
	/**
	 * Checks whether or not this session is opted in.
	 * It is not recommended that an application branch on analytics code
	 * because this adds an unnecessary testing burden to the developer.   
	 * However, this function is provided for developers who wish to
	 * pre-populate check boxes in settings menus.
	 * @return True if the user is opted in, false otherwise.
	 */
	public boolean isOptedIn()
	{
		return LocalyticsSession._isOptedIn;
	}
	
	/**
	 * Opens the Localytics session.  The session time as presented on the
	 * website is the time between <code>open</code> and the final <code>close</code> 
	 * so it is recommended to open the session as early as possible, and close
	 * it at the last moment.  The session must be opened before any tags can
	 * be written.  It is recommended that this call be placed in <code>onCreate</code>.
	 * <br>
	 * If for any reason this is called more than once every subsequent open call
	 * will be ignored.
	 */
	public void open()
	{		
		// Allow only one open call to happen.  
		synchronized(LocalyticsSession.class)
		{			
			// do nothing if opted out, if the session is already open, or the session is complete.
			if(LocalyticsSession._isOptedIn == false ||		
			   this._isSessionOpen == true || 	
			   this._isSessionDone == true)    			   
			{
				return; 
			}
			
			this._isSessionOpen = true;
		}
		
		// if there are too many files on the disk already, return w/o doing anything.
		// All other session calls, such as tagEvent and close will return because isSessionOpen == false
		File fp = new File(this._localyticsDirPath);
		if(fp.exists())
		{			
			// Get a list of all the session files.
			FilenameFilter filter = new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.startsWith(LocalyticsSession.SESSION_FILE_PREFIX) || name.startsWith(LocalyticsSession.UPLOADER_FILE_PREFIX);
				}
			};		
			
			// If that list is larger than the max number, don't create a new session.
			if( fp.list(filter).length >= LocalyticsSession.MAX_NUM_SESSIONS)
			{
				this._isSessionOpen = false;
				return;
			}			
		}		
			
		// Otherwise, prepare the session.		
		this._sessionUUID = UUID.randomUUID().toString();
		this._sessionFilename = LocalyticsSession.SESSION_FILE_PREFIX + this._sessionUUID;
		
		// It isn't necessary to have each session live in its own file because every event
		// has the session_id in it.  However, this makes maintaining a queue much simpler, 
		// and it simplifies multithreading because different instances write to different files
		fp = getOrCreateFile(this._sessionFilename);		
		if(fp == null) 
		{			
			// returning without creating the file, and setting isSessionOpen to true
			// will cause no data to be written for this session.
			this._isSessionOpen = false;
			return;
		}		
		
		appendDataToFile(fp, getOpenSessionString());		
	}
	
	/**
	 * Closes the Localytics session.  This should be done when the application or
	 * activity is ending.  Because of the way the Android lifecycle works, this
	 * call could end up in a place which gets called multiple times (such as
	 * <code>onPause</code> which is the recommended location).  This is
	 * fine because only the last close is processed by the server.
	 * <br>
	 * If close is not called, the session will still be uploaded but no
	 * events will be processed and the session time will not appear. This is
	 * because the session is not yet closed so it should not be used in
	 * comparison with sessions which are closed.
	 */
	public void close()
	{
		if(LocalyticsSession._isOptedIn == false ||		// do nothing if opted out 
						this._isSessionOpen == false) 	// do nothing if session is not open
		{ 
				return; 
		}
		
		File fp = getOrCreateFile(this._sessionFilename);		
		if(fp != null) 
		{
			// Create the session close blob 
			StringBuffer closeString = new StringBuffer();
			closeString.append(DatapointHelper.CONTROLLER_SESSION);
			closeString.append(DatapointHelper.ACTION_UPDATE);
			closeString.append(DatapointHelper.formatControllerValue(
																	 DatapointHelper.PARAM_UUID, 
																	 this._sessionUUID));
			closeString.append(DatapointHelper.OBJECT_SESSION_DP);
			closeString.append(DatapointHelper.formatDatapoint(
															   DatapointHelper.PARAM_CLIENT_CLOSED_TIME, 
															   DatapointHelper.getTimeAsDatetime()));
			
			appendDataToFile(fp, closeString.toString());			
		}
		
		// Write this session id to disk.  This way if the session is restarted it can easily
		// be picked up again
		fp = getOrCreateFile(LocalyticsSession.SESSION_ID_FILENAME);		
		try
		{
			FileWriter writer = new FileWriter(fp);
			writer.write(this._sessionUUID);		
			writer.flush();
		}
		catch(IOException e)
		{
			Log.v(LocalyticsSession.LOG_TAG, "IO Exceition writing Session ID file: " + e.getMessage());
		}				
		
		this._isSessionOpen = false;  // No more data can be written to this session
		this._isSessionDone = true;   // Need a second variable so calls to open() also return.
	}
	
	/**
	 * Allows a session to tag a particular event as having occurred.  For
	 * example, if a view has three buttons, it might make sense to tag
	 * each button click with the name of the button which was clicked. 
	 * For another example, in a game with many levels it might be valuable
	 * to create a new tag every time the user gets to a new level in order
	 * to determine how far the average user is progressing in the game.
	 * <br>
	 * <strong>Tagging Best Practices</strong>
	 * <ul>
	 * <li>DO NOT use tags to record personally identifiable information.</li>
	 * <li>The best way to use tags is to create all the tag strings as predefined
	 * constants and only use those.  This is more efficient and removes the risk of
	 * collecting personal information.</li>
	 * <li>Do not set tags inside loops or any other place which gets called
	 * frequently.  This can cause a lot of data to be stored and uploaded.</li>
	 * </ul>
	 * <br>
	 * See the tagging guide at: http://docs.localytics.com/
	 * @param event The name of the event which occurred.
	 */
	public void tagEvent(final String event)
	{		
		if(LocalyticsSession._isOptedIn == false ||		// do nothing if opted out
		   this._isSessionOpen == false)	// do nothing if session is not open
		{ 
			return; 
		}
		
		File fp = getOrCreateFile(this._sessionFilename);
		if(fp != null)
		{
			// Create the YML for the event
			StringBuffer eventString = new StringBuffer();
			eventString.append(DatapointHelper.CONTROLLER_EVENT);
			eventString.append(DatapointHelper.ACTION_CREATE);
			eventString.append(DatapointHelper.OBJECT_EVENT_DP);
			eventString.append(DatapointHelper.formatDatapoint(
					DatapointHelper.PARAM_UUID, UUID.randomUUID().toString()));						
			eventString.append(DatapointHelper.formatDatapoint(
					DatapointHelper.PARAM_SESSION_UUID, this._sessionUUID));									
			eventString.append(DatapointHelper.formatDatapoint(
					DatapointHelper.PARAM_CLIENT_TIME, DatapointHelper.getTimeAsDatetime()));
			eventString.append(DatapointHelper.formatDatapoint(
					DatapointHelper.PARAM_EVENT_NAME, event));
			
			appendDataToFile(fp, eventString.toString());
		}				
	}
	
	/**
	 * Creates a low priority thread which uploads any Localytics data already stored 
	 * on the device.  This should be done early in the process life in order to 
	 * guarantee as much time as possible for slow connections to complete.  It
	 * is necessary to do this even if the user has opted out because this is how
	 * the opt out is transported to the webservice.
	 */
	public void upload()
	{
		// Synchronize the check to make sure the upload is not
		// already happening.  This avoids the possibility of two
		// uploader threads being started at once. While this isn't necessary it could
		// conceivably reduce the load on the server
		synchronized(LocalyticsSession.class)
		{
			// Uploading should still happen even if the session is opted out.
			// This way the opt-out event gets sent to the server so we know this
			// user is opted out.  After that, no data will be collected so nothing
			// will get uploaded.
			if(LocalyticsSession._isUploading) 
			{ 
				return; 
			}
			
			LocalyticsSession._isUploading = true;
		}
		
		File fp = new File(this._localyticsDirPath);
		UploaderThread uploader = new UploaderThread(
							      	fp,
							      	LocalyticsSession.SESSION_FILE_PREFIX,
							      	LocalyticsSession.UPLOADER_FILE_PREFIX,							      	
							      	this.uploadComplete);
		uploader.start();		
	}

	////////////////////////////////////////
	// Private Methods /////////////////////
	////////////////////////////////////////    
    /**
     * Gets a file from the application storage, or creates if it isn't there.
     * @param path relative path to create the file in. should not be seperator_terminated
     * @param filename the file to create
     * @return a File object, or null if something goes wrong
     */
    private File getOrCreateFile(final String filename)
    {
    	// Get the file if it already exists
    	File fp = new File( this._localyticsDirPath + filename);
    	if(fp.exists()) 
    	{
    		return fp;
    	}
    	
    	// Otherwise, create any necessary directories, and the file itself.
    	new File(this._localyticsDirPath).mkdirs();    	    	
    	try 
    	{
			if(fp.createNewFile())
			{
				return fp;
			}
		} 
    	catch (IOException e) 
    	{ 
    		Log.v(LocalyticsSession.LOG_TAG, "Unable to get or create file: " + filename);	
		}
    	
    	return null;
    }
    
    /**
     * Uses an OutputStreamWriter to write and flush a string to the end of a text file.
     * @param file Text file to append data to.  
     * @param data String to be appended
     */
    private static void appendDataToFile(final File file, final String data)
    {
    	try
    	{
    		// Only allow one append to happen at a time.  This gaurantees files don't get corrupted by
    		// multiple threads in the same app writing at the same time, and it gaurantees app-wide
    		// like device_id don't get broken by multiple instance apps.
    		synchronized(LocalyticsSession.class) 
    		{
	    		FileOutputStream fOut = new FileOutputStream(file, true);    		
	    		OutputStreamWriter osw = new OutputStreamWriter(fOut);    		
				osw.write(data);
				osw.flush();
				osw.close();
    		}
    	}
    	catch(IOException e) 
    	{ 
    		Log.v(LocalyticsSession.LOG_TAG, "AppendDataToFile failed with IO Exception: " + e.getMessage()); 
		}
    }
    
    /**
     * Creates the YAML string for the open session event.
     * Collects all the basic session datapoints and writes them out as a YAML string.  
     * @return The YAML blob for the open session event.
     */
    private String getOpenSessionString()
    {
    	StringBuffer openString = new StringBuffer();
    	TelephonyManager telephonyManager = (TelephonyManager)this._appContext.getSystemService(Context.TELEPHONY_SERVICE);    	
    	Locale defaultLocale = Locale.getDefault();
    	
    	openString.append(DatapointHelper.CONTROLLER_SESSION);
    	openString.append(DatapointHelper.ACTION_CREATE);
    	openString.append(DatapointHelper.OBJECT_SESSION_DP);
    	
    	// Application and session information
    	openString.append(DatapointHelper.formatDatapoint(
    						DatapointHelper.PARAM_UUID, this._sessionUUID));
    	openString.append(DatapointHelper.formatDatapoint(
    						DatapointHelper.PARAM_APP_UUID, this._applicationKey));
        openString.append(DatapointHelper.formatDatapoint(
        					DatapointHelper.PARAM_APP_VERSION, DatapointHelper.getAppVersion(this._appContext)));
        openString.append(DatapointHelper.formatDatapoint(
        					DatapointHelper.PARAM_LIBRARY_VERSION, LocalyticsSession.CLIENT_VERSION));
        openString.append(DatapointHelper.formatDatapoint(
        					DatapointHelper.PARAM_CLIENT_TIME, DatapointHelper.getTimeAsDatetime()));        
        
        // Other device information
        openString.append(DatapointHelper.formatDatapoint(
        		DatapointHelper.PARAM_DEVICE_UUID, getDeviceId()));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_DEVICE_PLATFORM, "Android"));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_OS_VERSION, Build.ID));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_DEVICE_MODEL, Build.MODEL));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_LOCALE_LANGUAGE, defaultLocale.getLanguage()));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_LOCALE_COUNTRY, defaultLocale.getCountry()));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_DEVICE_COUNTRY, telephonyManager.getSimCountryIso()));
        
        // Network information
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_NETWORK_CARRIER, telephonyManager.getNetworkOperatorName()));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_NETWORK_COUNTRY, telephonyManager.getNetworkCountryIso()));
        openString.append(DatapointHelper.formatDatapoint(
				DatapointHelper.PARAM_DATA_CONNECTION, DatapointHelper.getNetworkType(this._appContext, telephonyManager)));                        
        
    	return openString.toString();
    }    
    
    /**
     * Gets an identifier which is unique to this machine, but generated randomly
     * so it can't be traced.
     * @return Returns the deviceID as a string
     */
    private String getDeviceId()
    {
        // Try and get the global device ID.  If that fails, maintain an id
        // local to this application.  This way it is still possible to tell things like
        // 'new vs returning users' on the webservice. 
    	String deviceId = DatapointHelper.getGlobalDeviceId(this._appContext);
        if(deviceId == null) 
        {
        	deviceId = getLocalDeviceId();
        }
                
        return deviceId;
    }     
    
    /**
     * Gets an identifier unique to this application on this device.  If one is not currently available,
     * a new one is generated and stored.
     * @return An idenitifer unique to this application this device.
     */
    private String getLocalDeviceId()
    {
    	String deviceId = null;    	
    	
    	// Open the device ID file
    	File fp = getOrCreateFile(LocalyticsSession.DEVICE_ID_FILENAME);
    	
    	// if the file doesn't exist, create one.
    	if(fp.length() == 0)
    	{
    		deviceId = UUID.randomUUID().toString();
    		appendDataToFile(fp, deviceId);
    	}
    	else
    	{
    		try
    		{
    			// If it did exist, read the first line, which contains the ID.
    			FileInputStream fIn = new FileInputStream(fp);
    			InputStreamReader isr = new InputStreamReader(fIn);
    			BufferedReader bufReader = new BufferedReader(isr);
    			deviceId = bufReader.readLine();
    			
    			bufReader.close();
    			isr.close();
    			fIn.close();
    		}
    		catch (FileNotFoundException e) { Log.v(LocalyticsSession.LOG_TAG, "GetLocalDeviceID failed with FNF: " + e.getMessage()); }
    		catch (IOException e) { Log.v(LocalyticsSession.LOG_TAG, "GetLocalDeviceId Failed with IO Exception: " + e.getMessage()); }
    	}
    	
    	return deviceId;
    }
    
    /**
     * Creates an event telling the webservice that the user opted in or out.
     * @param optState True if they opted in, false if they opted out.
     */
	private void createOptEvent(boolean optState)
	{
		File fp = getOrCreateFile(LocalyticsSession.OPT_SESSION);		
		if(fp != null) 
		{
			// Create the session close blob 
			StringBuffer optString = new StringBuffer();
			optString.append(DatapointHelper.CONTROLLER_OPT);
			optString.append(DatapointHelper.ACTION_OPTIN);
			optString.append(DatapointHelper.OBJECT_OPT);
			
			optString.append(DatapointHelper.formatDatapoint(
					   DatapointHelper.PARAM_DEVICE_UUID, 
					   getDeviceId()));
			
			optString.append(DatapointHelper.formatDatapoint(
					   DatapointHelper.PARAM_APP_UUID, 
					   this._applicationKey));
			
			optString.append(DatapointHelper.formatDatapoint(
					   DatapointHelper.PARAM_OPT_VALUE, 
					   Boolean.toString(optState)));
						
			appendDataToFile(fp, optString.toString());			
		}
	}
    
    /**
     * Returns the UUID of the last session which was closed.  This is necessary in the case
     * where an activity has been recreated and we wish to reattach to the existing session.
     * @return the UUID of the previous LocalyticsSession or null if none could be returned
     */
    private String getStoredSessionUUID()
    {
    	// Open the stored session id file
    	File fp = new File(this._localyticsDirPath + LocalyticsSession.SESSION_ID_FILENAME);
    	if(fp.exists())
    	{
    		try 
    		{
    			// read in the first line
    			FileInputStream fIn = new FileInputStream(fp);
    			InputStreamReader isr = new InputStreamReader(fIn);
    			BufferedReader bufReader = new BufferedReader(isr);    			
    			String storedId = bufReader.readLine();
    			
    			bufReader.close();
    			isr.close();
    			fIn.close();
    			
    			return storedId;
			} 
    		catch (FileNotFoundException e) 
    		{
    			Log.v(LocalyticsSession.LOG_TAG, "File Not Found opening stored session");
    			return null;
			}    		
    		catch (IOException e)
    		{
    			Log.v(LocalyticsSession.LOG_TAG, "IO Exception getting stored session: " + e.getMessage());
    			return null;
    		}
    	}
    	
    	return null;
    }
	
	/**
	 * Runnable which gets passed to the uploader thread so it can
	 * notify the library when uploads are complete. 
	 */
	private Runnable uploadComplete = new Runnable()
	{
		public void run()
		{
			LocalyticsSession._isUploading = false;
		}
	};
}