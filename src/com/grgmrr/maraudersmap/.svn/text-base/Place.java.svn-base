package com.grgmrr.maraudersmap;

import java.io.Serializable;

import android.util.Log;

public class Place implements Serializable {

	private static final String LOG = "PlaceClass";
    
    //http://hoskinator.blogspot.com/2006/11/trouble-using-pipe-with-stringsplit.html
    private static final String INFO_DELIMITER = "\\|"; // escaped for regex.
    private static final String BUILDINGFLOOR_DELIMITER = ",";
    
    private String mBuildingFloor;
    private int mSpaceDistance;
	private int mMapX;
    private int mMapY;
    private int mMapW;
    
    /**
     * Given the MM place update output format, build a Place.
     * Sample MM output:
     * "OC00,in,Library|0|74|411|1"
     * @param piped_output
     */
    public Place(String piped_output) {
    	piped_output = piped_output.replaceAll("\\n", "");
    	piped_output = piped_output.replaceAll("\\r", "");
    	String[] place_info = piped_output.split(INFO_DELIMITER);
    	setBuildingFloor(place_info[0]);
    	setSpaceDistance(Integer.parseInt(place_info[1]));
    	setMapX(Integer.parseInt(place_info[2]));
    	setMapY(Integer.parseInt(place_info[3]));
    	setMapW(Integer.parseInt(place_info[4]));
    }
	
	public String toString() {
		String result = String.format("Place: name: %s", getName());
		return result;
	}
	
	public String getName() {
		// location strings look like WH,in,rm309
		String location = this.getBuildingFloor();
        location = location.replace("OC", "MH");
        
        String[] components;
        String building;
        String floor;
        String inside;
        String description;
        try {
        	components = location.split(BUILDINGFLOOR_DELIMITER);
        	building = components[0].substring(0, 2);        	
        	floor = components[0].substring(2, 3);
        	inside = components[1];
        	description = components[2];
        } catch (Exception e) {
        	Log.e(LOG, e.toString());
        	return this.getBuildingFloor();
        }
        
        if (inside.equalsIgnoreCase("in")) {
            inside = "inside";
        } else if (inside.equalsIgnoreCase("out")) {
        	inside = "outside of";
        }

        switch (Integer.parseInt(floor)) {
        	case 1:
        		floor = "1st";
        		break;
        	case 2:
        		floor = "2nd";
        		break;
        	case 3:
        		floor = "3rd";
        		break;
        	case 4:
        		floor = "4th";
        		break;
        	case 0:
        		floor = "LL";
        		break;
        	default:
        		break;
        }

        if (description.contains("rm") ||
        	description.contains("room") || 
        	description.contains("Room") || 
        	description.contains("ROOM"))
        {	
        	description = description.replace("rm", "");
	        description = description.replace("room", "");
	        description = description.replace("Room", "");
	        description = description.replace("ROOM", "");
	        
	        location = inside + " " + building + description;
        } else {
                if (floor != "LL") {
                        location = inside + " " + building + " " + floor + " floor " + description;
                } else {
                        location = inside + " " + building + " (" + floor + ") " + description;
                }
        }
        return location;
	}

	public void setBuildingFloor(String buildingFloor) {
		mBuildingFloor = buildingFloor;
	}

	public String getBuildingFloor() {
		return mBuildingFloor;
	}

	public void setSpaceDistance(int spaceDistance) {
		mSpaceDistance = spaceDistance;
	}

	public int getSpaceDistance() {
		return mSpaceDistance;
	}

	public void setMapX(int mapX) {
		mMapX = mapX;
	}

	public int getMapX() {
		return mMapX;
	}

	public void setMapY(int mapY) {
		mMapY = mapY;
	}

	public int getMapY() {
		return mMapY;
	}

	public void setMapW(int mapW) {
		mMapW = mapW;
	}

	public int getMapW() {
		return mMapW;
	}

}