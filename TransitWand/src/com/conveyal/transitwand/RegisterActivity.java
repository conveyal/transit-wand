package com.conveyal.transitwand;

import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class RegisterActivity extends Activity {

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
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		
		serviceIntent = new Intent(this, CaptureService.class);
        startService(serviceIntent);
        
        bindService(serviceIntent, caputreServiceConnection, Context.BIND_AUTO_CREATE);
        CaptureService.boundToService = true;
		
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				switch (v.getId()) {
				
					case R.id.RegisterButton:
						
						if(CaptureService.imei == null)
				        {
				        	TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
				    		CaptureService.imei = telephonyManager.getDeviceId();
				        }
						
						RequestParams params = new RequestParams();
				    	
				    	params.put("imei", CaptureService.imei);
				    	
				    	EditText userNameET = (EditText) findViewById(R.id.userNameTextBox);
				    	final String userName = userNameET.getText().toString().trim();
				    	
				    	params.put("userName", userName);
				    	
						AsyncHttpClient client = new AsyncHttpClient();
						client.setUserAgent("tw");
						client.get(CaptureService.URL_BASE + "register", params,  new JsonHttpResponseHandler() {
						    
							@Override
						    public void onSuccess(JSONObject response) {
						    	
						    	try {
						    		String unitId = response.getString("unitId");
						    		
						    		Toast.makeText(RegisterActivity.this, "Phone registered.", Toast.LENGTH_SHORT).show();
						    		
						    		SharedPreferences prefsManager = PreferenceManager.getDefaultSharedPreferences(RegisterActivity.this);
						    		prefsManager.edit().putBoolean("registered", true).putString("unitId", unitId).putString("userName", userName).commit();
						    		
						    		Intent uploadIntent = new Intent(RegisterActivity.this, UploadActivity.class);
									startActivity(uploadIntent);
									
									RegisterActivity.this.finish();
						    		
						    	}
						    	catch(Exception e) {		    		
						    		Toast.makeText(RegisterActivity.this, "Unable to register phone, check network connection.", Toast.LENGTH_SHORT).show();
						    	}
						    }
						    
						    public void onFailure(Throwable error, String content) {
						    	
						    	Toast.makeText(RegisterActivity.this, "Unable to register phone, check network connection.", Toast.LENGTH_SHORT).show();
						    }
						});	
						
						
						
						
						break;
				
					 default:
						 break;
				}
			}
		   };
		 
		   Button registerButton = (Button) findViewById(R.id.RegisterButton);
		   registerButton.setOnClickListener(listener);
		   
	}


}
