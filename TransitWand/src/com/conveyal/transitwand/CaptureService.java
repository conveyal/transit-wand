package com.conveyal.transitwand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.conveyal.transitwand.TransitWandProtos.Upload;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CaptureService extends Service {

	public final static String 	SERVER 					= "transitwand.com"; // // "; //"192.168.43.137:9000";
	public final static String 	URL_BASE 				= "http://" + SERVER +"/";
	
	public static Boolean boundToService = false;
	
	private static int 		GPS_UPDATE_INTERVAL 		= 5; 	// seconds 
	private static int		MIN_ACCURACY 				= 15; 	// meters
	private static int		MIN_DISTANCE 				= 5; 	// meters
	
	private static Boolean 	gpsStarted 					= false;
	private static Boolean 	registered 					= false;

	public static String 	imei 						= null;
	public static Long 		phoneId						= null;
	
    private static Application appContext;
    
    private static int NOTIFICATION_ID = 234231222;
    
	private final IBinder binder = new CaptureServiceBinder();

	private CaptureLocationListener locationListener;
	
	private LocationManager gpsLocationManager;
	private NotificationManager gpsNotificationManager;
	
	private static boolean gpsEnabled;

	public static RouteCapture currentCapture = null;
	public static RouteStop currentStop = null;
	
	private static Location lastLocation = null;
	
	public static Boolean capturing = false;
	
	private static ICaptureActivity captureActivity;
	
    private SharedPreferences prefsManager = null;
	
	@Override
    public void onCreate() {
        Log.i("CaptureService", "onCreate");
        
        appContext = this.getApplication();
        gpsNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
		prefsManager = PreferenceManager.getDefaultSharedPreferences(this);
        
        if(imei == null)
        {
        	TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    		CaptureService.imei = telephonyManager.getDeviceId();
        }
    }

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public class CaptureServiceBinder extends Binder {
        public CaptureService getService()
        {
            return CaptureService.this;
        }
    }
	
	public static void setCaptureActivity(ICaptureActivity ca)
    {
		captureActivity = ca;
    }
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    	Log.i("CaptureService", "onStartCommand");
    	handleIntent(intent);
    	
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	Log.i("CaptureService", "onDestroy");  
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
    	Log.i("CaptureService", "onLowMemory");
        super.onLowMemory();
    }
       
    private void handleIntent(Intent intent) {
    	// handle received intents
    	
    }
    
    public  void newCapture(String name, String description, String notes) {
    	
    	startGps();
    	
    	showNotificationTray();
    	
    	synchronized(this)	{ 
	    	if(currentCapture != null && capturing)
	    		stopCapture();
	    	
	    	lastLocation = null;
	    	
	    	Integer id = prefsManager.getInt("routeId", 10000);
			
			id++;
			
			prefsManager.edit().putInt("routeId", id).commit();
			
	    	currentCapture = new RouteCapture();
	    	currentCapture.id = id;
	    	currentCapture.setRouteName(name);
	    	currentCapture.description = description;
	    	currentCapture.notes = notes;
    	}
    }
   
    public void startCapture() throws NoCurrentCaptureException {
    	startGps();
    	
    	showNotificationTray();
    	
    	if(currentCapture != null) { 
    		currentCapture.startTime = SystemClock.elapsedRealtime();
    		capturing = true;
    	}
    	else {
    		throw new NoCurrentCaptureException();
    	}
    }
    
    public void stopCapture() {
    	stopGps();
    	
    	hideNotificationTray();
    	
    	currentCapture.stopTime = new Date().getTime();
    	
    	if(currentCapture.points.size() > 0) {
    		
	    	Upload.Route routePb = currentCapture.seralize();
	    	
	    	File file = new File(getExternalFilesDir(null), "route_" + currentCapture.id + ".pb");
	    	
	    	FileOutputStream os;
	    	
			try {
				
				os = new FileOutputStream(file);
				routePb.writeDelimitedTo(os);
				os.close();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}    	
    	}
    	
    	capturing = false;
    	currentCapture = null;
    }
    
    public void ariveAtStop() throws NoGPSFixException {

    	if(currentStop != null)
    		departStopStop(0, 0);
    	
    	if(lastLocation != null) {
    		currentStop = new RouteStop();

    		currentStop.arrivalTime = SystemClock.elapsedRealtime();
    		currentStop.location = lastLocation;
    	}
    	else
    		throw new NoGPSFixException();
    	
    }
    
    
    public void departStopStop(int board, int alight) throws NoGPSFixException {

    	if(lastLocation != null) {
    		
    		if(currentStop == null) {
        		currentStop = new RouteStop();
        		currentStop.arrivalTime = SystemClock.elapsedRealtime();
        		currentStop.location = lastLocation;
        	}
    		
    		currentStop.alight = alight;
    		currentStop.board = board;
    		currentStop.departureTime = SystemClock.elapsedRealtime();
	    
	    	currentCapture.stops.add(currentStop);
	    	
	    	currentStop = null;
    	}
    	else
    		throw new NoGPSFixException();
    	
    }
    
    public boolean atStop() {
    	
    	if(currentStop == null)
    		return false;
    	else
    		return true;
    	
    }
    
    public long distanceFromLocation(Location l1, Location l2) {
    
			LatLng ll1 = new LatLng(l1.getLatitude(), l1.getLongitude());
			LatLng ll2 = new LatLng(l2.getLatitude(), l2.getLongitude());
			
			return Math.round(LatLngTool.distance(ll1, ll2, LengthUnit.METER));
    }
    
    public void onLocationChanged(Location location)
    {
    	Log.i("", "onLocationChanged: " + location);
    	
    	if(atStop() && lastLocation != null && distanceFromLocation(currentStop.location, lastLocation) > MIN_DISTANCE * 2) {
    		captureActivity.triggerTransitStopDepature();
    	}
    	
    	if(currentCapture != null && location.getAccuracy() < MIN_ACCURACY * 2) {
    		
    		RoutePoint rp = new RoutePoint();
    		rp.location = location;
    		rp.time = SystemClock.elapsedRealtime();
    		
    		currentCapture.points.add(rp);
    		
    		if(lastLocation != null) {
    			
    			currentCapture.distance += distanceFromLocation(lastLocation, location);
    			
    			if(captureActivity != null)
    				captureActivity.updateDistance();
    		}    		
    		
    		lastLocation = location;
    		
    		if(captureActivity != null)
				captureActivity.updateGpsStatus();
    	}
    }
     
    private void startGps()
    {
    	Log.i("LocationService", "startGps");
    	
    	if(gpsStarted)
    		return;
    	
    	gpsStarted = true;

    	if (locationListener == null)
        {
    		locationListener = new CaptureLocationListener();
        }
        
        // connect location manager
        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        
        gpsEnabled = gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        
        if(gpsEnabled)
        {	
        	Log.i("LocationService", "startGps attaching listeners");
        	// request gps location and status updates
        	
        	gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_INTERVAL * 1000, MIN_DISTANCE, locationListener);
        	gpsLocationManager.addGpsStatusListener(locationListener);
        	
            // update gps status in main activity
        }
        else
        {
        	Log.e("LocationService", "startGps failed, GPS not enabled");
            // update gps status in main activity
        }
    }
    
    private void stopGps() {
    	Log.i("LocationService", "stopGps");
    	
    	if(gpsLocationManager == null)
    		gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	
    	if(locationListener != null)
    	{
    		gpsLocationManager.removeUpdates(locationListener);
    		gpsLocationManager.removeGpsStatusListener(locationListener);
    	}
    	
    	gpsStarted = false;
    }

    public void restartGps() {
    	Log.i("LocationService", "restartGps");
    
    	stopGps();
    	startGps();
    }
    
    public void stopGpsAndRetry() {

    	Log.d("LocationService", "stopGpsAndRetry");
    	
    	restartGps();
    }

    public String getGpsStatus() {
    	
    	String status = "";
    	
    	if(lastLocation != null)
    		status = "GPS +/-" + Math.round(lastLocation.getAccuracy()) + "m";
    	else
    		status = "GPS Pending";
    	
    	return status;
    }
    
    private void showNotificationTray() {
    	
    	Intent contentIntent = new Intent(this, CaptureActivity.class);
    	
    	PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, contentIntent,
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification notification = new Notification(R.drawable.tray_icon, null, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
    

        notification.setLatestEventInfo(getApplicationContext(), "TransitWand", "", pending);
        
        gpsNotificationManager.notify(NOTIFICATION_ID, notification);
        startForeground(NOTIFICATION_ID, notification);
        
    }
    
    private void hideNotificationTray() { 

        gpsNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
    }
	
	
	
	private class CaptureLocationListener implements LocationListener, GpsStatus.Listener {

		@Override
	    public void onLocationChanged(Location location)
	    {
	        try
	        {
	            if (location != null)
	            {
	            	Log.i("ProbeLocationListener", "onLocationChanged");
	            	CaptureService.this.onLocationChanged(location);
	            }

	        }
	        catch (Exception ex)
	        {
	        	Log.e("ProbeLocationListener", "onLocationChanged failed", ex);
	        }

	    }

		@Override
	    public void onProviderDisabled(String provider) {
	    	Log.i("ProbeLocationListener", "onProviderDisabled");
	    	//this.restartGps();
	    }

		@Override
	    public void onProviderEnabled(String provider) {
	    	Log.i("ProbeLocationListener", "onProviderEnabled");
	    	//locationService.restartGps();
	    }
		
		@Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	       
	    }

	    public void onGpsStatusChanged(int event) {
	        switch (event)
	        {
	            case GpsStatus.GPS_EVENT_FIRST_FIX:
	            	
	                break;

	            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
		        	
	                break;

	            case GpsStatus.GPS_EVENT_STARTED:
	            	
	                break;

	            case GpsStatus.GPS_EVENT_STOPPED:
	            
	                break;

	        }
	    }
	}
}
