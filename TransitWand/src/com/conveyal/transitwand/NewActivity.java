package com.conveyal.transitwand;

import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class NewActivity extends Activity {

	private static Intent serviceIntent;
    private CaptureService captureService;

	
	private final ServiceConnection caputreServiceConnection = new ServiceConnection()
    {

        public void onServiceDisconnected(ComponentName name)
        {
        	captureService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service)
        {
        	captureService = ((CaptureService.CaptureServiceBinder) service).getService();
        	//com.conveyal.transitwand.CaptureServiceaptureService.setServiceClient(CaptureActivity.this);
        
        }
    };

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new);
		
		serviceIntent = new Intent(this, CaptureService.class);
        startService(serviceIntent);
        
        bindService(serviceIntent, caputreServiceConnection, Context.BIND_AUTO_CREATE);
		
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				switch (v.getId()) {
				
					case R.id.ContinueButton:
						
						if(!captureService.capturing)
							createNewCapture();
						else {
							updateCapture();
						}
						
						
						Intent settingsIntent = new Intent(NewActivity.this, CaptureActivity.class);
						startActivity(settingsIntent);
				
						break;
						
				
					 default:
						 break;
				}
			}
		   };
		 
		   Button wandButton = (Button) findViewById(R.id.ContinueButton);
		   wandButton.setOnClickListener(listener);
		  	
	}
	
	private void createNewCapture() {
		
		synchronized(this)	{ 
					
			EditText routeName = (EditText) findViewById(R.id.routeName);
			EditText routeDescription = (EditText) findViewById(R.id.routeDescription);
			EditText fieldNotes = (EditText) findViewById(R.id.fieldNotes);
			EditText vehicleCapacity = (EditText) findViewById(R.id.vehicleCapacity);
			EditText vehicleType = (EditText) findViewById(R.id.vehicleType);

			captureService.newCapture(routeName.getText().toString(), routeDescription.getText().toString(), fieldNotes.getText().toString(), vehicleType.getText().toString(), vehicleCapacity.getText().toString());			
		}		
	}
	
	private void updateCapture() {
		
		synchronized(this)	{ 
			
			EditText routeName = (EditText) findViewById(R.id.routeName);
			EditText routeDescription = (EditText) findViewById(R.id.routeDescription);
			EditText fieldNotes = (EditText) findViewById(R.id.fieldNotes);
			EditText vehicleCapacity = (EditText) findViewById(R.id.vehicleCapacity);
			EditText vehicleType = (EditText) findViewById(R.id.vehicleType);
			
			captureService.currentCapture.setRouteName(routeName.getText().toString());
			captureService.currentCapture.description = routeDescription.getText().toString();
			captureService.currentCapture.notes = fieldNotes.getText().toString();
			captureService.currentCapture.vehicleCapacity = vehicleCapacity.getText().toString();
			captureService.currentCapture.vehicleType = vehicleType.getText().toString();
		}		
	}


}
