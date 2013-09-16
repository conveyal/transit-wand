package com.conveyal.transitwand;

import java.text.DecimalFormat;
import java.util.Date;

import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.Menu;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CaptureActivity extends Activity implements ICaptureActivity {
	
	private static Intent serviceIntent;
    private CaptureService captureService;
    
    private Vibrator vibratorService;
    
    private final ServiceConnection caputreServiceConnection = new ServiceConnection()
    {

        public void onServiceDisconnected(ComponentName name)
        {
        	captureService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service)
        {
        	captureService = ((CaptureService.CaptureServiceBinder) service).getService();
        	captureService.setCaptureActivity(CaptureActivity.this);  
        	
        	initButtons();
 		   
        	updateRouteName();
 		   	updateDistance();
 		    updateDuration();
 		   	updateStopCount();
 		   	updatePassengerCountDisplay();
        }
    };

    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
		
		// Start the service in case it isn't already running
		
		serviceIntent = new Intent(this, CaptureService.class);
        startService(serviceIntent);
        
        bindService(serviceIntent, caputreServiceConnection, Context.BIND_AUTO_CREATE);
        CaptureService.boundToService = true;
        
        vibratorService = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        
		
		// setup button listeners
		
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				switch (v.getId()) {
				
					case R.id.StartCaptureButton:
			
						if(!captureService.capturing) {
							startCapture();
						}
						
						break;
						
					case R.id.StopCaptureButton:
						
						if(captureService.capturing) {
						
							DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
							    @Override
							    public void onClick(DialogInterface dialog, int which) {
							        switch (which){
							        case DialogInterface.BUTTON_POSITIVE:
							        	stopCapture();
							            break;
	
							        case DialogInterface.BUTTON_NEGATIVE:
							            //No button clicked
							            break;
							        }
							    }
							};
	
							AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
							builder.setMessage("Do you want to finish capturing?").setPositiveButton("Yes", dialogClickListener)
							    .setNegativeButton("No", dialogClickListener).show();
							
						}
				
						break;
					
					case R.id.transitStopButton:
						
						transitStop();
						
						break;
						
					case R.id.PassengerAlightButton:
						
						passengerAlight();
						
						break;
					
					case R.id.PassengerBoardButton:
						
						passengerBoard();
						
						break;
						
					 default:
						 break;
				}
			}
		   };

		   ImageButton startCaptureButton = (ImageButton) findViewById(R.id.StartCaptureButton);
		   startCaptureButton.setOnClickListener(listener);
		   
		   ImageButton stopCaptureButton = (ImageButton) findViewById(R.id.StopCaptureButton);
		   stopCaptureButton.setOnClickListener(listener);		 
		   
		   ImageButton transitStopButton = (ImageButton) findViewById(R.id.transitStopButton);
		   transitStopButton.setOnClickListener(listener);		 
		   
		   ImageButton passengerAlightButton = (ImageButton) findViewById(R.id.PassengerAlightButton);
		   passengerAlightButton.setOnClickListener(listener);		 
		   
		   ImageButton passengerBoardButton = (ImageButton) findViewById(R.id.PassengerBoardButton);
		   passengerBoardButton.setOnClickListener(listener);
		  		 
		   ImageView passengerImageView = (ImageView) findViewById(R.id.passengerImageView);
		   passengerImageView.setAlpha(128);
		   
		   initButtons();
		   
		   updateRouteName();
		   updateDistance();
		   updateStopCount();
		   updatePassengerCountDisplay();
	}

	
	private void initButtons() {
		
		if(captureService == null) {
			ImageButton startCaptureButton = (ImageButton) findViewById(R.id.StartCaptureButton);
			startCaptureButton.setImageResource(R.drawable.start_button_gray);
			
			ImageButton stopCaptureButton = (ImageButton) findViewById(R.id.StopCaptureButton);
			stopCaptureButton.setImageResource(R.drawable.stop_button_gray);
		}
		
		else if(captureService.capturing) {
			ImageButton startCaptureButton = (ImageButton) findViewById(R.id.StartCaptureButton);
			startCaptureButton.setImageResource(R.drawable.start_button_gray);
			
			ImageButton stopCaptureButton = (ImageButton) findViewById(R.id.StopCaptureButton);
			stopCaptureButton.setImageResource(R.drawable.stop_button);
		}
		else if(!captureService.capturing && captureService.currentCapture == null) {

			ImageButton startCaptureButton = (ImageButton) findViewById(R.id.StartCaptureButton);
			startCaptureButton.setImageResource(R.drawable.start_button_gray);
			
			ImageButton stopCaptureButton = (ImageButton) findViewById(R.id.StopCaptureButton);
			stopCaptureButton.setImageResource(R.drawable.stop_button_gray);
		}
		
		if(!captureService.capturing && captureService.currentCapture == null && captureService.atStop()) {

			ImageButton transitCaptureButton = (ImageButton) findViewById(R.id.transitStopButton);
			transitCaptureButton.setImageResource(R.drawable.transit_stop_button_red);
		}
	}
	
	private void startCapture() {
		
		if(captureService != null)
		{
			try { 
				captureService.startCapture();
			} catch(NoCurrentCaptureException e) {
				Intent settingsIntent = new Intent(CaptureActivity.this, NewActivity.class);
				startActivity(settingsIntent);
				return;
			}
			
			vibratorService.vibrate(100);
			
			Toast.makeText(CaptureActivity.this, "Starting capture..." ,Toast.LENGTH_SHORT).show();
			
			((Chronometer) findViewById(R.id.captureChronometer)).setBase(SystemClock.elapsedRealtime());
			((Chronometer) findViewById(R.id.captureChronometer)).start();
			
			initButtons();
		}
	}
	
	private void stopCapture() {
		
		if(captureService != null) {
			
			vibratorService.vibrate(100);
			
			if(captureService.currentCapture.points.size() > 0)
				Toast.makeText(CaptureActivity.this, "Capture complete.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(CaptureActivity.this, "No data collected, canceling.", Toast.LENGTH_SHORT).show();
			
			captureService.stopCapture();
		}
		// else handle 
		
		
		
		((Chronometer) findViewById(R.id.captureChronometer)).stop();
		
		initButtons();
		
		Intent finishCaptureIntent = new Intent(CaptureActivity.this, MainActivity.class);
		finishCaptureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(finishCaptureIntent);
	}
	
	private void passengerAlight() {
		
		if(captureService != null && captureService.capturing) {
			if(captureService.currentCapture.totalPassengerCount == 0)	
				return;
			
			vibratorService.vibrate(5);
			
			captureService.currentCapture.totalPassengerCount--;
			
			captureService.currentCapture.alightCount++;
			updatePassengerCountDisplay();
		}
	}
	
	private void passengerBoard() { 
		
		if(captureService != null && captureService.capturing) {
			captureService.currentCapture.totalPassengerCount++;
			
			vibratorService.vibrate(5);
			
			captureService.currentCapture.boardCount++;
			updatePassengerCountDisplay();
		}
	}
	
	public void triggerTransitStopDepature() {
		
		if(captureService != null) {
			if(captureService.atStop()) {
				transitStop();
			}
		}
	}
	
	private void transitStop() {
		
		if(captureService != null && captureService.capturing) {
			
			try {
				if(captureService.atStop()) {
					captureService.departStopStop(captureService.currentCapture.boardCount, captureService.currentCapture.alightCount);
					captureService.currentCapture.alightCount = 0;
					captureService.currentCapture.boardCount = 0;
					
					ImageButton transitCaptureButton = (ImageButton) findViewById(R.id.transitStopButton);
					transitCaptureButton.setImageResource(R.drawable.transit_stop_button);
					
					vibratorService.vibrate(25);
					
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					vibratorService.vibrate(25);
					
					Toast.makeText(CaptureActivity.this, "Stop departure", Toast.LENGTH_SHORT).show();
				}
				else {
					captureService.ariveAtStop();
					
					ImageButton transitCaptureButton = (ImageButton) findViewById(R.id.transitStopButton);
					transitCaptureButton.setImageResource(R.drawable.transit_stop_button_red);
					
					vibratorService.vibrate(25);
					
					Toast.makeText(CaptureActivity.this, "Stop arrival", Toast.LENGTH_SHORT).show();
				}
				
				updatePassengerCountDisplay();
				
				updateStopCount();
			} 
			catch(NoGPSFixException e) {
				 Toast.makeText(CaptureActivity.this, "Unable to record stop: GPS lock pending.", Toast.LENGTH_SHORT).show();
			}
		}
	
	}
	
	private void updateRouteName() {
		TextView routeNameText = (TextView) findViewById(R.id.routeNameText);
		
		routeNameText.setText("Capture: " + captureService.currentCapture.name);
	}
	
	private void updateStopCount() {
		TextView stopsText = (TextView) findViewById(R.id.stopsText);
		
		stopsText.setText(captureService.currentCapture.stops.size() + "");
	}
	
	public void updateDistance() {

		TextView distanceText = (TextView) findViewById(R.id.distanceText);
	
		DecimalFormat distanceFormat = new DecimalFormat( "#,##0.00" );
	
		distanceText.setText(distanceFormat.format((double)(captureService.currentCapture.distance) / 1000) + "km");
	
	}
	
	public void updateDuration() {
		if(captureService.capturing) {
			((Chronometer) findViewById(R.id.captureChronometer)).setBase(captureService.currentCapture.startMs);
			((Chronometer) findViewById(R.id.captureChronometer)).start();
		}
		else {
			((Chronometer) findViewById(R.id.captureChronometer)).setBase(SystemClock.elapsedRealtime());
		}
	}
	
	public void updateGpsStatus() {
		TextView distanceText = (TextView) findViewById(R.id.gpsStatus);
		
		distanceText.setText(captureService.getGpsStatus());
	}
	
	private void updatePassengerCountDisplay() {
		
		if(captureService != null && captureService.capturing) {
			
			TextView totalPassengerCount = (TextView) findViewById(R.id.totalPasssengerCount);
			TextView alightingPassengerCount = (TextView) findViewById(R.id.alightingPassengerCount);
			TextView boardingPassengerCount = (TextView) findViewById(R.id.boardingPassengerCount);
			
			totalPassengerCount.setText(captureService.currentCapture.totalPassengerCount.toString());
			
			if(captureService.currentCapture.alightCount > 0)
				alightingPassengerCount.setText("-" + captureService.currentCapture.alightCount.toString());
			else
				alightingPassengerCount.setText("0");
			
			if(captureService.currentCapture.boardCount > 0)
				boardingPassengerCount.setText("+" + captureService.currentCapture.boardCount.toString());
			else
				boardingPassengerCount.setText("0");
		}
	}
}
