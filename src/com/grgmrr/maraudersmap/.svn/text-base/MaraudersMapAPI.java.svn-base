package com.grgmrr.maraudersmap;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.net.wifi.ScanResult;
import android.util.Log;

public class MaraudersMapAPI {

    private static final String LOG = "MauradersMapAPI";
    private static final String BASE_URL = "http://acl.olin.edu/map/";
    
    //needs a 1 or 2 on end for upper/lower campus
    private static final String UPDATE_PATH = "ui/map_backend.php?mapw=";
    private static final String UPDATE_PREFIX = "success:";
    private static final String UPDATE_DELIMITER = ";";
    private static final String WRITE_PATH = "update.php?";
    private static final String PLATFORM = "ANDROID";
    
    public MaraudersMapAPI() {}
    
    /**
     * Take the ACL server's response String and return People.
     * @param result
     * @return
     */
    private Vector<Person> parseMapUpdate(String result) {
    	Vector<Person> people_vector = new Vector<Person>();
    	
    	result = result.replaceFirst(UPDATE_PREFIX, "");
    	String[] people_strings = result.split(UPDATE_DELIMITER);
    	
    	Date now = new Date();
    	for (String person_string : people_strings) {
			people_vector.add(new Person(person_string, now));
			Log.d(LOG, "Made a Person.");
    	}
    	
    	return people_vector;
    }
    
    public Person[] getPeople() {
    	Vector<Person> people_vector = new Vector<Person>();
    	try {
	    	people_vector.addAll(parseMapUpdate(getMapUpdate("1")));
	    	Log.d(LOG, "Got Map 1");
	    	people_vector.addAll(parseMapUpdate(getMapUpdate("2")));
	    	Log.d(LOG, "Got Map 2");
    	} catch (Exception e) {
    		Log.e(LOG, e.toString());
    		Log.e(LOG, "Possibly not on Olin LAN?");
    	}
    	
    	Log.d(LOG, "About to sort people");
    	Person[] people_array = sortPeople(people_vector);    	
    	Log.d(LOG, "Sorted People, returning.");
    	return people_array;
    }
    
    private Person[] sortPeople(Vector<Person> people_vector) {
    	Person[] people_array = new Person[people_vector.size()];
    	people_vector.copyInto(people_array);
    	Arrays.sort(people_array, Person.PersonTimeComparator);
    	Log.d(LOG, "array sorted");
    	return people_array;
    }
    
    private String getMapUpdate(String mapid) {
    	String url = BASE_URL + UPDATE_PATH + mapid;
    	HttpGet getMethod = new HttpGet(url);
    	String result = "";
		try {
			HttpClient client = new DefaultHttpClient();
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			result = client.execute(getMethod, responseHandler);
			Log.v(LOG, result);
		} catch (Exception e) {
			Log.e(LOG, e.toString());
		}
		return result;
    }
    
    public void setPlace(String username, Place place) {
    	List<ScanResult> scan_results = new ArrayList<ScanResult>();
    	String placename = place.getBuildingFloor();
    	postMapUpdate(username, scan_results, placename);
    }
    
    public Place[] getPlaces(List<ScanResult> scan_results) {
    	String username = "test";
    	String placename = "test";
    	Vector<Place> place_vector = new Vector<Place>();
    	try {
	    	place_vector.addAll(parsePostMapUpdate(postMapUpdate(username, scan_results, placename)));
    	} catch (Exception e) {
    		Log.e(LOG, e.toString());
    		Log.e(LOG, "Possibly not on Olin LAN?");
    	}

    	Place[] place_array = new Place[place_vector.size()];
    	place_vector.copyInto(place_array);
    	return place_array;
    }
    
    private Vector<Place> parsePostMapUpdate(String result) {
    	Vector<Place> place_vector = new Vector<Place>();
    	
    	result = result.replaceFirst(UPDATE_PREFIX, "");
    	String[] place_strings = result.split(UPDATE_DELIMITER);
    	
    	for (String place_string : place_strings) {
			place_vector.add(new Place(place_string));
    	}
 
    	return place_vector;
    }
        
    private String formatScanResults(List<ScanResult> scan_results) {
    	Vector<String> scan_result_strings = new Vector<String>();
    	for (ScanResult scan_result : scan_results) {
    		//spec calls for (db + 100)
    		scan_result_strings.add(String.format("%s,%s", scan_result.BSSID, (scan_result.level + 100)));
    	}
    	String scan_results_formatted = join(scan_result_strings, ";");
    	return scan_results_formatted;
    }
    
    private String postMapUpdate(String username, List<ScanResult> scan_results, String placename) {
    	Hashtable<String, String> parameters = new Hashtable<String, String>();
    	parameters.put("username", username);
    	parameters.put("platform", PLATFORM);
    	parameters.put("placename", placename);
    	parameters.put("data", this.formatScanResults(scan_results));
    	URI uri = getPostMapUpdateURI(parameters);
    	Log.d(LOG, uri.toString());
    	
    	HttpGet getMethod = new HttpGet(uri.toString());
    	String result = "";
		try {
			HttpClient client = new DefaultHttpClient();
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			result = client.execute(getMethod, responseHandler);
			Log.v(LOG, result);
		} catch (Exception e) {
			Log.e(LOG, e.toString());
		}
    	//// FIXME: MOCK RESULT. FIX WHEN AT OLIN.
    	//String result = "success:OC00,in,Library|0|74|411|1;OC10,in,library|0|130|424|1;OC00,in,Library Workroom|0|137|523|1;OC10,out,library|0|288|415|1;OC00,in,Computer Lab|0|236|600|1;OC20,in,Mezzanine|0|231|487|1;OC20,out,rm227|0|131|590|1;OC30,in,rm332|0|105|510|1;OC30,in,rm325|0|121|584|1;WH10,in,Kitchen|0|511|182|2";
		return result;
    }
    
    private URI getPostMapUpdateURI(Hashtable<String, String> parameters) {
    	Vector<String> url_params = new Vector<String>();    	
    	for (String key : parameters.keySet()) {
    		String new_param = null;
    		try {
    			new_param = URLEncoder.encode(parameters.get(key), "UTF-8");
    		} catch (UnsupportedEncodingException e) {
    			e.printStackTrace();
    		}
    		url_params.add(String.format("%s=%s", key, new_param));
    	}
    	String url = String.format("%s%s%s", BASE_URL, WRITE_PATH, join(url_params, "&"));

    	return URI.create(url);
    }
    
    static String join(Collection<String> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
           builder.append(iter.next());
            if (iter.hasNext()) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
   }
    
}