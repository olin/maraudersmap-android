package com.grgmrr.maraudersmap;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import android.util.Log;


public class Person implements Serializable {

	private static final String LOG = "PersonClass";
    private static final String BASE_URL = "http://acl.olin.edu/map/";
    
    //http://hoskinator.blogspot.com/2006/11/trouble-using-pipe-with-stringsplit.html
    private static final String INFO_DELIMITER = "\\|"; // escaped for regex.
    public static final DateFormat kDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   
    private String mName;
    private String mTime;
	private String mPlace;
    private String mIconPath;
    private long mTimeDelta;
    
    /**
     * Given the MM person update output format, build a Person.
     * Sample MM output:
     * "393|677|Gregory Marra|Inside EH117|2009-05-06 18:14:54|1|p.gif"
     * @param piped_output
     */
    public Person(String piped_output, Date now) {
    	//Log.v(LOG, "Building person from: " + piped_output);
    	piped_output.replaceAll("\n", "");
    	String[] user_info = piped_output.split(INFO_DELIMITER);
    	setName(user_info[2]);
    	setPlace(user_info[3]);
    	setTime(user_info[4]);
    	setIconPath(user_info[6]);
    	setTimeDelta(now);
    }
    
    /**
     * Minutes between Maurader's Map report and now.
     * Dates are like: "2009-05-06 21:07:17"
     * @return
     */
    public long getTimeDelta() {
    	return mTimeDelta;
    }
    
    private void setTimeDelta(Date now) {
    	Date then = now;
    	try {
    		then = kDateFormat.parse(mTime);
		} catch (ParseException e) {
			Log.e(LOG, "Bad time! Tried to parse " + mTime);
		}
		mTimeDelta = (now.getTime() - then.getTime()) / 60000; //60k is a minute in milliseconds
    }
    
    /**
     * A nice string about how many minutes ago last MM update was.
     * @return
     */
    public String getPrettyTime() {
		return String.format("%d minutes ago", getTimeDelta());
    }
    
    public String getName() {
		return mName;
	}
	public void setName(String name) {
		mName = name;
	}
	public String getTime() {
		return mTime;
	}
	public void setTime(String time) {
		mTime = time;
	}
	public String getPlace() {
		return mPlace;
	}
	public void setPlace(String place) {
		mPlace = place;
	}
	public String getIconPath() {
		return mIconPath;
	}
	public void setIconPath(String iconPath) {
		mIconPath = iconPath;
	}
	
	public String toString() {
		String result = String.format("Person: name: %s place: %s time: %s",
				getName(), getPlace(), getTime());
		return result;
	}
	
	public static final Comparator<Person> PersonTimeComparator = new Comparator<Person>() {
		public int compare(Person person1, Person person2) {
			if (person1.getTimeDelta() < person2.getTimeDelta()) {
				return -1;
			} else if (person1.getTimeDelta() == person2.getTimeDelta()){
				return 0;
			} else {
				return 1;
			}
		}
	};

}