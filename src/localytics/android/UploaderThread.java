/**
 * UploaderThread.java
 * Copyright (C) 2009 Char Software Inc., DBA Localytics
 * 
 *  This code is provided under the Localytics Modified BSD License.
 *  A copy of this license has been distributed in a file called LICENSE
 *  with this source code.  
 *  
 *  Please visit www.localytics.com for more information.
 */

package localytics.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

/**
 * The thread which handles uploading Localytics data.
 * @author Localytics
 */
public class UploaderThread extends Thread 
{
	private Runnable _completeCallback;	
	private File     _localyticsDir;
	private String   _sessionFilePrefix;
	private String   _uploaderFilePrefix;
	
	// The Tag used in logging.
	private final static String LOG_TAG = "Localytics_uploader";	
	
	//private final static String ANALYTICS_URL = "http://alpha.localytics.com/api/datapoints/bulk";
	private final static String ANALYTICS_URL = "http://analytics.localytics.com/api/datapoints/bulk";
	
	/**
	 * Creates a thread which uploads the session files in the passed Localytics
	 * Directory.  All files starting with sessionFilePrefix are renamed,
	 * uploaded and deleted on upload.  This way the sessions can continue
	 * writing data regardless of whether or not the upload succeeds.  Files
	 * which have been renamed still count towards the total number of Localytics
	 * files which can be stored on the disk.   
	 * @param appContext The context used to access the disk
	 * @param completeCallback A runnable which is called notifying the caller that upload is complete.
	 * @param localyticsDir The directory containing the session files
	 * @param sessionFilePrefix The filename prefix identifying the session files.
	 * @param uploaderfilePrefix The filename prefixed identifying files to be uploaded.
	 */
	public UploaderThread(	
			File localyticsDir,
			String sessionFilePrefix,
			String uploaderFilePrefix,
			Runnable completeCallback)
	{
		this._completeCallback = completeCallback;
		this._localyticsDir = localyticsDir;
		this._sessionFilePrefix = sessionFilePrefix;
		this._uploaderFilePrefix = uploaderFilePrefix;
	}
	
	/**
	 * Renames all the session files (so that other threads can keep writing
	 * datapoints without affecting the upload.  And then uploads them.
	 */
	public void run()
	{				
		if(this._localyticsDir != null && this._localyticsDir.exists())
		{
			// Create a filter to only grab the session files.
			FilenameFilter filter = new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.startsWith(_sessionFilePrefix);
				}
			};						
						
			// Rename or append all the session files.
			String[] originalFiles = this._localyticsDir.list(filter);						
			if(originalFiles.length != 0)
			{										
				String basePath = this._localyticsDir.getAbsolutePath();																				
				renameOrAppendFiles(basePath, originalFiles);
									
				// Create a new filter, grabbing all the renamed files.  This is necessary
				// in order to grab files which were renamed by a previous uploader attempt.
				filter = new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						return name.startsWith(_uploaderFilePrefix);
					}
				};
										
				String uploaderFiles[] = this._localyticsDir.list(filter);
				String postBody = createPostBodyFromFiles(basePath, uploaderFiles);
										
				// Attempt to upload this data.  If successful, delete all the uploaderFiles.			
				if(uploadSessions(postBody.toString()) == true)
				{
					int currentFile;
					File uploadedFile;
					for(currentFile = 0; currentFile < uploaderFiles.length; currentFile++)
					{
						uploadedFile = new File(basePath + "/" + uploaderFiles[currentFile]);
						uploadedFile.delete();
					}
				}
			} // end if there are files to upload.
		} // end if the directory exists	

		// Notify the caller the upload is complete.
		if(this._completeCallback != null) 
		{			
			this._completeCallback.run();
		}
	}
		
	/**
	 * Renames all of the files in the files array, inside the basePath folder.  This way the renamed
	 * files can be uploaded while the original files receive more data.
	 * @param basePath The full path to the directory containing the files to upload
	 * @param files An array of files to be uploaded
	 */
	private void renameOrAppendFiles(final String basePath, final String[] originalFiles)
	{
		Log.v(UploaderThread.LOG_TAG, "In uploader. Dir: " + basePath + " # files: " + originalFiles.length);
		
		int currentFile;
		File originalFile;
		File destinationFile;
		
		for(currentFile = 0; currentFile < originalFiles.length; currentFile++)
		{							
			String originalFileName = basePath + "/" + originalFiles[currentFile];
			String targetFileName = basePath + "/" + this._uploaderFilePrefix + originalFiles[currentFile];				
			originalFile = new File(originalFileName);
			destinationFile = new File(targetFileName);			
			
			// If the target file already exists it means a previous upload did not succeed
			// this session should be appended to that session.
			if(destinationFile.exists())
			{				
				FileOutputStream fOut;
				try 
				{
					// open a stream to the output file.
					fOut = new FileOutputStream(destinationFile, true);
					OutputStreamWriter osw = new OutputStreamWriter(fOut);
					
					// open a stream to the input file
					FileInputStream fIn = new FileInputStream(originalFile);
					InputStreamReader isr = new InputStreamReader(fIn);
					BufferedReader bufReader = new BufferedReader(isr);
					String inputLine;
					
					// Read the input file and write it to the output file.
					synchronized(LocalyticsSession.class)
					{
						while( (inputLine = bufReader.readLine()) != null)
						{
							osw.append(inputLine);
							osw.append("\n");
						}
						
						// delete the original file when we are done with it. All it's data is in the output file now.
						osw.flush();
						originalFile.delete();
					}
				} 
				catch (FileNotFoundException e) 
				{
					Log.v(LOG_TAG, "File not found.");
				}
				catch (IOException e)
				{
					Log.v(LOG_TAG, "IO Exception: " + e.getMessage());
				}
			}
			// If the target file does not exist, rename this file.  This causes all data up until now
			// to be uploaded, but allows the session to keep collecting data.
			else
			{
				originalFile.renameTo(destinationFile);		
			}								
		}
	}
	
	/**
	 * Reads in the input files and cats them together in one big string which makes up the
	 * HTTP request body.
	 * @param basePath The directory to get the files from
	 * @param uploaderFiles the list of files to read
	 * @return A string containing a YML blob which can be uploaded to the webservice.
	 */
	private String createPostBodyFromFiles(final String basePath, final String[] uploaderFiles)
	{
		int currentFile;
		File inputFile;
		StringBuffer postBody = new StringBuffer();
		String inputLine;
		
		// Read each file in to one buffer.  This allows the upload to happen as one
		// large transfer instead of many smaller transfers which is preferable on
		// a mobile device in which the time required to make a connection is often
		// disproprtionately large compared to the time to upload the data.
		for(currentFile = 0; currentFile < uploaderFiles.length; currentFile++)
		{
			inputFile = new File(basePath + "/" + uploaderFiles[currentFile]);
			
			FileInputStream fIn;
			try 
			{
				// open the file
				fIn = new FileInputStream(inputFile);
				InputStreamReader isr = new InputStreamReader(fIn);
				BufferedReader bufReader = new BufferedReader(isr);
				
				// read it in line by line.
				while( (inputLine = bufReader.readLine()) != null)
				{
					postBody.append(inputLine);
					postBody.append("\n");
				}
			} 
			
			// Catch the exceptions but keep trying.
			catch (FileNotFoundException e) 
			{
				Log.v(LOG_TAG, "File Not Found");
			} 
			catch (IOException e) 
			{
				Log.v(LOG_TAG, "IOException: " + e.getMessage());
			} 					
		}

		return postBody.toString();
	}
	
	/**
	 * Uploads the post Body to the webservice
	 * @param ymlBlob String containing the YML to upload
	 * @return True on success, false on failure.
	 */
	private boolean uploadSessions(String ymlBlob)
	{
		Log.v(UploaderThread.LOG_TAG, "Starting upload.");
		DefaultHttpClient client = new DefaultHttpClient();		
		HttpPost method = new HttpPost(ANALYTICS_URL);
		
		try
		{
			StringEntity postBody = new StringEntity(ymlBlob);
			method.setEntity(postBody);
			HttpResponse response = client.execute(method);			
			
			StatusLine status = response.getStatusLine();
			Log.v(UploaderThread.LOG_TAG, "Upload complete. Status: " + status.getStatusCode());
			
			// On any response from the webservice, return true so the local files get
			// deleted.  This avoid an infinite loop in which a bad file keeps getting
			// submitted to the websrvice time and again.
			return true;
		}
		
		// return true for any transportation errors.
		catch (UnsupportedEncodingException e) 
		{ 
			Log.v(LOG_TAG, "UnsuppEncodingException: " + e.getMessage()); 
			return false; 
		}
		catch (ClientProtocolException e) 		
		{ 
			Log.v(LOG_TAG, "ClientProtocolException: " + e.getMessage()); 
			return false; 
		}
		catch (IOException e) 
		{ 
			Log.v(LOG_TAG, "IOException: " + e.getMessage()); 
			return false; 
		}						
	}
}