package com.conveyal.transitwand;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

import com.conveyal.transitwand.TransitWandProtos.Upload;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UploadActivity extends Activity {

	private ByteArrayInputStream dataStream = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
	
		if(getFilesDir().listFiles().length == 0) {
			Toast.makeText(UploadActivity.this, R.string.no_data_upload, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		
		TextView routeCountText = (TextView) findViewById(R.id.routeCountText);		
		routeCountText.setText(getFilesDir().listFiles().length + "");
		
		Long dataSize = 0l;
		
		Upload.Builder uploadBuilder = Upload.newBuilder();
		uploadBuilder.setUnitId(0l);
		uploadBuilder.setUploadId(0);
		
		
		for(File f : getFilesDir().listFiles()) {
			dataSize += f.length();
			
			DataInputStream dataInputStream = null;
			
			try {
				dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
				
				Upload.Route pbRouteData = Upload.Route.parseDelimitedFrom(dataInputStream);
				
				dataInputStream.close();
				
				uploadBuilder.addRoute(pbRouteData);
			
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if(dataInputStream != null) {
					try {
						dataInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		dataStream = new ByteArrayInputStream(uploadBuilder.build().toByteArray());
		
		DecimalFormat distanceFormat = new DecimalFormat( "#,##0.00" );
		
		String dataSizeFormated = "";
		
		if(dataSize / 1024 / 1024 > 1) {
			dataSizeFormated = distanceFormat.format(dataSize / 1024 / 1024) + R.string.kb;
		}
		else {
			dataSizeFormated = distanceFormat.format(dataSize / 1024) + R.string.mb;
		}
		
		TextView dataSizeText = (TextView) findViewById(R.id.dataSizeText);		
		dataSizeText.setText(dataSizeFormated);
		
		
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				switch (v.getId()) {
				
					case R.id.uploadButton:
						
						if(CaptureService.imei == null)
				        {
				        	TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
				    		CaptureService.imei = telephonyManager.getDeviceId();
				        }
						
						ImageButton uploadButton = (ImageButton) findViewById(R.id.uploadButton);
						uploadButton.setVisibility(View.GONE);
						
						ProgressBar progressSpinner = (ProgressBar) findViewById(R.id.progressBar);
						progressSpinner.setVisibility(View.VISIBLE);
						
						RequestParams params = new RequestParams();
				    	
				    	params.put("imei", CaptureService.imei);
				    	params.put("data", dataStream);
				    
						try {
							dataStream.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
				    	
						AsyncHttpClient client = new AsyncHttpClient();
						client.setTimeout(240 * 1000);
						client.setUserAgent("tw");
						client.post(CaptureService.URL_BASE + "upload", params,  new AsyncHttpResponseHandler() {
						    
							@Override
						    public void onSuccess(String response) {
						    	
						    	try {
						    		
						    		Toast.makeText(UploadActivity.this, R.string.data_uploaded, Toast.LENGTH_SHORT).show();
									
						    		 for(File f : getFilesDir().listFiles()) {
						    			f.delete();
						    		} 
						    		
									UploadActivity.this.finish();
						    		
						    	}
						    	catch(Exception e) {		
						    		
						    		ProgressBar progressSpinner = (ProgressBar) findViewById(R.id.progressBar);
									progressSpinner.setVisibility(View.GONE);
							    	
							    	ImageButton uploadButton = (ImageButton) findViewById(R.id.uploadButton);
									uploadButton.setVisibility(View.VISIBLE);
									
						    		Toast.makeText(UploadActivity.this, R.string.unable_to_upload, Toast.LENGTH_SHORT).show();
						    	}
						    }
						    
						    public void onFailure(Throwable error, String content) {
						    	
						    	Log.e("upload", "Upload failed: " + error + " " + content);
						    
						    	
						    	ProgressBar progressSpinner = (ProgressBar) findViewById(R.id.progressBar);
								progressSpinner.setVisibility(View.GONE);
						    	
						    	ImageButton uploadButton = (ImageButton) findViewById(R.id.uploadButton);
								uploadButton.setVisibility(View.VISIBLE);
								
						    	Toast.makeText(UploadActivity.this, R.string.unable_to_upload, Toast.LENGTH_SHORT).show();
						    }
						});	
						
						
						
						
						break;
				
					 default:
						 break;
				}
			}
		   };
		 
		   ImageButton uploadButton = (ImageButton) findViewById(R.id.uploadButton);
		   uploadButton.setOnClickListener(listener);
	}
}
