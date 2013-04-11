package com.conveyal.transitwand;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	
	public static Boolean registered = false;
	public static Integer unitId = null;
	public static String userName = null;
	
	private SharedPreferences prefsManager = null;

	SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
	 	@Override
    	public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
	 		
	 		if (key.equals("registered")) {
	 			
	 			updateRegistrationData();
			 	}
		 	}
	 	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		prefsManager = PreferenceManager.getDefaultSharedPreferences(this);
		prefsManager.registerOnSharedPreferenceChangeListener(prefListener);
		
		prefsManager.edit().putBoolean("registered", false).commit();
		
		updateRegistrationData();
		
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				switch (v.getId()) {
				
					case R.id.WandButton:
						
						Intent settingsIntent = new Intent(MainActivity.this, NewActivity.class);
						startActivity(settingsIntent);
						break;
						
					case R.id.ReviewButton:
								
						Intent mailIntent = new Intent(MainActivity.this, ReviewActivity.class);
						startActivity(mailIntent);
						break;
					
					case R.id.UploadButton:
						
						if(registered) {
							Intent uploadIntent = new Intent(MainActivity.this, UploadActivity.class);
							startActivity(uploadIntent);
						}
						else {
							Intent regiseterIntent = new Intent(MainActivity.this, RegisterActivity.class);
							startActivity(regiseterIntent);
						}
						
						break;
						
					 default:
						 break;
				}
			}
		   };
		 
		   ImageButton wandButton = (ImageButton) findViewById(R.id.WandButton);
		   wandButton.setOnClickListener(listener);
		   
		   ImageButton reviewButton = (ImageButton) findViewById(R.id.ReviewButton);
		   reviewButton.setOnClickListener(listener);
		   
		   ImageButton uploadButton = (ImageButton) findViewById(R.id.UploadButton);
		   uploadButton.setOnClickListener(listener);		 
		   
	}
	
	
	
	public void updateRegistrationData() {
		
		if(prefsManager != null) {
			
			registered = prefsManager.getBoolean("registered", false);
			
			TextView userNameText = (TextView) findViewById(R.id.UserNameText);
			userNameText.setText(prefsManager.getString("userName", ""));
			
			TextView unitIdText = (TextView) findViewById(R.id.UnitIdText);
			unitIdText.setText("Unit " + prefsManager.getString("unitId", "unregistered"));
		}
	}

}
