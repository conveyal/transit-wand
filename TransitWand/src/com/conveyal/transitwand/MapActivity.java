package com.conveyal.transitwand;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MBTilesFileArchive;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;

import com.conveyal.transitwand.TransitWandProtos.Upload;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MapActivity extends Activity {

	private static Boolean USE_MAPBOX = true;
	
	private static String MAPBOX_URL = "http://a.tiles.mapbox.com/v3/conveyal.map-jc4m5i21/";
	private static String MAPBOX_URL_HIRES = "http://a.tiles.mapbox.com/v3/conveyal.map-o5b3npfa/";
	
	private ITileSource customTileSource;
	private ItemizedIconOverlay<OverlayItem> stopOverlay;
    private PathOverlay capturePath;
	private MapView mapView;
	private MyLocationOverlay locOverlay;
	private DefaultResourceProxyImpl resourceProxy;
	private	RelativeLayout mapContainer;
	
	public static int itemPosition = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
		resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
		setContentView(R.layout.activity_map);
		
		if(USE_MAPBOX) {
			// mapbox hosted tile source
			String tileUrl = MAPBOX_URL;
			
			mapView = new MapView(this, 256);
			
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			
			if(metrics.densityDpi > DisplayMetrics.DENSITY_MEDIUM)
				tileUrl = MAPBOX_URL_HIRES;
				
			customTileSource = new XYTileSource("Mapbox", null, 0, 17, 256, ".png", tileUrl);
			mapView.setTileSource(this.customTileSource);
			
			mapView.getTileProvider().clearTileCache();
		
		}
		else {
			// local mbtiles cache
			
			customTileSource  = new XYTileSource("mbtiles", ResourceProxy.string.offline_mode, 10, 16, 256, ".png", "http://conveyal.com/");		
			
			
        	SimpleRegisterReceiver simpleReceiver = new SimpleRegisterReceiver(this);    		
        	MBTilesFileArchive mbtilesDb = MBTilesFileArchive.getDatabaseFileArchive(null);  // swap null for file      	
        	IArchiveFile[] files = { mbtilesDb };
        	
			MapTileModuleProviderBase moduleProvider = new MapTileFileArchiveProvider(simpleReceiver, this.customTileSource, files);
			
			MapTileProviderArray provider = new MapTileProviderArray(this.customTileSource, null, new MapTileModuleProviderBase[]{ moduleProvider });
			
			mapView = new MapView(this, 256, resourceProxy, provider);
		}
		
		mapContainer = (RelativeLayout) findViewById(R.id.mainMapView);
		
		this.mapView.setLayoutParams(new LinearLayout.LayoutParams(
		          LinearLayout.LayoutParams.FILL_PARENT,
		          LinearLayout.LayoutParams.FILL_PARENT));
		
		mapContainer.addView(this.mapView);
	
		
		File f = getFilesDir().listFiles()[itemPosition];
		DataInputStream dataInputStream = null;
		RouteCapture rc = null;
		try {
			dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
		
		//	byte[] dataFrame = new byte[(int)f.length()];;
			
		//	dataInputStream.read(dataFrame);
			
			Upload.Route pbRouteData = Upload.Route.parseDelimitedFrom(dataInputStream);
			
			rc = RouteCapture.deseralize(pbRouteData);
	
		
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
		
		GeoPoint gp = null;
		
		if(rc != null)
		{
			Paint paint = new Paint();
			paint.setColor(Color.BLUE);
			paint.setAlpha(75);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(10); 
			
			capturePath = new PathOverlay(Color.BLUE, this);
			capturePath.setPaint(paint);
			
			for(RoutePoint p : rc.points) {
				gp = new GeoPoint(p.location.getLatitude(),p.location.getLongitude());
				capturePath.addPoint(gp);
			}
			
			mapView.getOverlays().add(capturePath);
			
			
			ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
			
			for(RouteStop s : rc.stops) {

				OverlayItem itemMarker = new OverlayItem("Board: " + s.board + " Alight: " + s.alight, "", new GeoPoint(s.location.getLatitude(), s.location.getLongitude()));
				
				itemMarker.setMarker(MapActivity.this.getResources().getDrawable(R.drawable.stop));
		
				items.add(itemMarker);
					
			}
			
			
			
			stopOverlay = new ItemizedIconOverlay<OverlayItem>(items,
	                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
	                    @Override
	                    public boolean onItemSingleTapUp(final int index,
	                            final OverlayItem item) {
	                        Toast.makeText(
	                        		MapActivity.this, item.mTitle ,Toast.LENGTH_SHORT).show();
	                        return true;
	                    }
	                    @Override
	                    public boolean onItemLongPress(final int index,
	                            final OverlayItem item) {
	                        Toast.makeText(
	                        		MapActivity.this, item.mTitle,Toast.LENGTH_SHORT).show();
	                        return false;
	                    }
	                }, resourceProxy);
		    
		    mapView.getOverlays().add(stopOverlay);
		    mapView.invalidate();
		    
		    
		    View.OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					
					switch (v.getId()) {
					
						case R.id.trashButton:
							
							DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
							    @Override
							    public void onClick(DialogInterface dialog, int which) {
							        switch (which){
							        case DialogInterface.BUTTON_POSITIVE:
							        	
							        	getFilesDir().listFiles()[MapActivity.itemPosition].delete();
							        	
							        	Intent intent = new Intent(ReviewActivity.DELETE_ITEM_ACTION);
							      	  	LocalBroadcastManager.getInstance(MapActivity.this).sendBroadcast(intent);
							        	
							        	MapActivity.this.finish();
							            break;
	
							        case DialogInterface.BUTTON_NEGATIVE:
							            //No button clicked
							            break;
							        }
							    }
							};
	
							AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
							builder.setMessage(R.string.you_want_delete_route).setPositiveButton(R.string.yes, dialogClickListener)
							    .setNegativeButton(R.string.no, dialogClickListener).show();
							
							break;
							
						 default:
							 break;
					}
				}
			   };
			 
			   ImageButton trashButton = (ImageButton) findViewById(R.id.trashButton);
			   trashButton.setOnClickListener(listener);
			   
			   
			   TextView routeName = (TextView) findViewById(R.id.nameText);
			   routeName.setText(rc.name);
			   
			   TextView descriptionText = (TextView) findViewById(R.id.descriptionText);
			   descriptionText.setText(rc.description);
			   
			   TextView notesText = (TextView) findViewById(R.id.notesText);
			   notesText.setText(rc.notes);
			 
		}
	    

	   /* locOverlay = new MyLocationOverlay(this, mapView);
	    locOverlay.enableMyLocation();
	    locOverlay.enableFollowLocation();
        
	    mapView.getOverlays().add(locOverlay);
	    
        locOverlay.runOnFirstFix(new Runnable() {
		 public void run() {
			 mapView.getController().setCenter(locOverlay.getMyLocation());
		 } 
        });*/
		
		
		mapView.setBuiltInZoomControls(true);
		mapView.setMultiTouchControls(true);
		
		mapView.getController().setZoom(16);
	    
		if(gp != null)
			mapView.getController().setCenter(gp);
		else
			mapView.getController().setCenter(new GeoPoint(10.3021258, 123.89616));
		
		
		
		mapView.invalidate();
	  
		
	}
	
}
