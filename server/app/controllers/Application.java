package controllers;

import play.*;
import play.mvc.*;
import utils.DirectoryZip;
import utils.TripPatternSerializer;
import utils.TripPatternShapeSerializer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.EntityManager;

import jobs.ProcessGisExport;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import au.com.bytecode.opencsv.CSVWriter;

import com.conveyal.transitwand.TransitWandProtos.Upload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import models.*;
import models.transit.Agency;
import models.transit.Route;
import models.transit.RoutePoint;
import models.transit.RouteType;
import models.transit.Stop;
import models.transit.TripPattern;
import models.transit.TripPatternStop;
import models.transit.TripShape;

public class Application extends Controller {

	
    public static void index(Boolean invalid) {
        render(invalid);
    }
    
    public static void export(String unitId) throws InterruptedException {

        Phone p = Phone.find("unitId = ?", unitId).first();

        List<TripPattern> tripPatterns =  TripPattern.find("route.phone = ?", p).fetch();

        ArrayList<Long>  patternIds = new ArrayList<Long>();

        for(TripPattern pattern : tripPatterns) {
            patternIds.add(pattern.id);
        }
        
    	ok();
    }
    
    public static void exportAll() throws InterruptedException {
    	
    	List<TripPattern> tripPatterns =  TripPattern.findAll();
    	/*for(TripPattern tp: tripPatterns) {
    		ProcessGisExport gisExport = new ProcessGisExport(tp.id);
    		gisExport.doJob();
    	}*/
    	
    	ok();
    }

   
    public static void register(String imei, String userName) {
        
    	Phone p = Phone.registerImei(imei, userName);
    	
    	renderJSON(p);
    }
    
    public static void upload(String imei, File data) {
    	
    	try {
    		
    		File pbFile = new File(Play.configuration.getProperty("application.uploadDataDirectory"), imei + "_" + new Date().getTime() + ".pb");
    		Logger.info(pbFile.toString());
    		data.renameTo(pbFile);
 
			byte[] dataFrame = new byte[(int)pbFile.length()];;
			DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(pbFile)));
		
			dataInputStream.read(dataFrame);			
			Upload upload = Upload.parseFrom(dataFrame);
			
			
			Phone phone = Phone.find("imei = ?", imei).first();
			if(phone == null)
				badRequest();
			
			
			for(Upload.Route r : upload.getRouteList()) {
				
				if(r.getPointList().size() <= 1)
					continue;
				
				
				Agency a = Agency.find("gtfsAgencyId = ?", "DEFAULT").first();
				Route route = new Route("", r.getRouteName(), RouteType.BUS, r.getRouteDescription(),  a);
				route.phone = phone;
				route.routeNotes = r.getRouteNotes();
				route.vehicleCapacity = r.getVehicleCapacity();
				route.vehicleType = r.getVehicleType();
				route.captureTime = new Date(r.getStartTime());
				route.save();
				
				List<String> points = new ArrayList<String>();
	        	
				Integer pointSequence = 1;
	        	for(Upload.Route.Point p : r.getPointList()) {
	        		points.add(new Double(p.getLon()).toString() + " " + new Double(p.getLat()).toString());
	        		RoutePoint.addRoutePoint(p, route.id, pointSequence);
	        		pointSequence++;
	        	}
	        	
	        	String linestring = "LINESTRING(" + StringUtils.join(points, ", ") + ")";
	            
	        	BigInteger tripShapeId = TripShape.nativeInsert(TripShape.em(), "", linestring, 0.0);
	        	
	        	TripPattern tp = new TripPattern();
	        	tp.route = route;
	        	tp.headsign = r.getRouteName();
	        	tp.shape = TripShape.findById(tripShapeId.longValue());
	        	tp.save();
	        	
	        	Integer sequenceId = 0;
	        	
	        	for(Upload.Route.Stop s : r.getStopList()) {
	        		BigInteger stopId = Stop.nativeInsert(Stop.em(), s);
	        		
	        		TripPatternStop tps = new TripPatternStop();
	        		tps.stop = Stop.findById(stopId.longValue());
	        		tps.stopSequence = sequenceId;
	        		tps.defaultTravelTime = s.getArrivalTimeoffset();
	        		tps.defaultDwellTime = s.getDepartureTimeoffset() - s.getArrivalTimeoffset();
	        		tps.pattern = tp;
	        		tps.board = s.getBoard();
	        		tps.alight = s.getAlight();
	        		tps.save();
	        		
	        		sequenceId++;
	        	}	   
	        	
	        	//ProcessGisExport gisExport = new ProcessGisExport(tp.id);
	        	//gisExport.doJob();
			}
			
			Logger.info("Routes uploaded: " + upload.getRouteList().size());
			
			dataInputStream.close();
			
	        ok();
    	}
		catch(Exception e) {
			e.printStackTrace();
			badRequest();
		}
	       
    }
    
    public static void view(String unitId) {

    	if(unitId == null)
    		index(true);
    	
    	Phone p = Phone.find("unitId = ?", unitId).first();
    	
    	
    	if(p == null)
    		index(true);
    	
    	List<TripPattern> patterns = TripPattern.find("route.phone = ?", p).fetch();
    	
        render(p, patterns);
        
    }
    
    public static void list(String unitId) {

    	Http.Header hd = new Http.Header();
    	
    	hd.name = "Access-Control-Allow-Origin";
    	hd.values = new ArrayList<String>();
    	hd.values.add("*");
    	
    	Http.Response.current().headers.put("Access-Control-Allow-Origin",hd); 
    	
    	if(unitId == null)
    		badRequest();
    	
    	Phone p = Phone.find("unitId = ?", unitId).first();
    	
    	
    	if(p == null)
    		badRequest();
    	
    	List<TripPattern> patterns = TripPattern.find("route.phone = ?", p).fetch();
    	
    	Gson gson = new GsonBuilder().registerTypeAdapter(TripPattern.class, new TripPatternSerializer()).serializeSpecialFloatingPointValues().serializeNulls().create();
        
    	renderJSON(gson.toJson(patterns));
        
    }
    
    
    public static void pattern(Long patternId) {
    	
    	Http.Header hd = new Http.Header();
    	
    	hd.name = "Access-Control-Allow-Origin";
    	hd.values = new ArrayList<String>();
    	hd.values.add("*");
    	
    	Http.Response.current().headers.put("Access-Control-Allow-Origin",hd); 
    	
    	TripPattern pattern = TripPattern.findById(patternId);
    	
    	Gson gson = new GsonBuilder().registerTypeAdapter(TripPattern.class, new TripPatternShapeSerializer()).serializeSpecialFloatingPointValues().serializeNulls().create();
        
    	renderJSON(gson.toJson(pattern));
    
    }
    
    public static void exportGis(String unitId) throws InterruptedException {
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm");
    	String timestamp = sdf.format(new Date());
 
    	ArrayList<Long> patterns = new ArrayList<Long>();
    	
    	String[] unitIds = unitId.split(",");
    	
    	for(String id : unitIds) {
    		
    		Phone p = Phone.find("unitId = ?", id).first();
   		
    		if(p != null) {
    		
    			List<Route> unitRouteList = Route.find("phone = ?", p).fetch();
        		
    			for(Route r : unitRouteList) {
    				List<TripPattern> tps = TripPattern.find("route = ?", r).fetch();
    				
    				for(TripPattern tp : tps) {
    					patterns.add(tp.id);
    				}
    			}
    		}
    	}
    	
    	ProcessGisExport pge = new ProcessGisExport(patterns, timestamp);
    			
    	pge.doJob();
    	
    	redirect("/public/data/exports/" + timestamp + ".zip");
    }
    
    public static void exportCsv(String unitId) throws IOException {
    	
    	ArrayList<Route> routes = new ArrayList<Route>();
    	
    	String[] unitIds = unitId.split(",");
    	
    	for(String id : unitIds) {
    		
    		Phone p = Phone.find("unitId = ?", id).first();
   		
    		if(p != null) {
    		
    			List<Route> unitRouteList = Route.find("phone = ?", p).fetch();
        		
        		routes.addAll(unitRouteList);
    			
    		}
    		
    	}
    	
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh-mm");
    	String timestamp = sdf.format(new Date());
    	
		File outputDirectory = new File(Play.configuration.getProperty("application.exportDataDirectory"), timestamp);
		File outputZipFile = new File(Play.configuration.getProperty("application.exportDataDirectory"), timestamp + ".zip");
		
		// write routes
		File routesFile = new File(outputDirectory, timestamp + "_routes.csv");
		File stopsFile = new File(outputDirectory, timestamp + "_stops.csv");
		
		if(!outputDirectory.exists())
    	{
    		outputDirectory.mkdir();
    	}

        if(outputZipFile.exists()) {
            outputZipFile.delete();
        }
		
		FileWriter routesCsv = new FileWriter(routesFile);
		CSVWriter rotuesCsvWriter = new CSVWriter(routesCsv);
		
		FileWriter stopsCsv = new FileWriter(stopsFile);
		CSVWriter stopsCsvWriter = new CSVWriter(stopsCsv);
		
		String[] routesHeader = "unit_id, route_id, route_name, route_description, field_notes, vehicle_type, vehicle_capacity, start_capture".split(",");
		
		String[] stopsHeader = "route_id, stop_sequence, lat, lon, travel_time, dwell_time, board, alight".split(",");
		   	 	
		rotuesCsvWriter.writeNext(routesHeader);
		stopsCsvWriter.writeNext(stopsHeader);
   
   	 	for(Route r : routes) {
   	 		
   	 		String[] routeData = new String[routesHeader.length];
   	 		routeData[0] = r.phone.unitId;
   	 		routeData[1] = r.id.toString();
   	 		routeData[2] = r.routeLongName;
   	 		routeData[3] = r.routeDesc;
   	 		routeData[4] = r.routeNotes;
   	 		routeData[5] = r.vehicleType;
   	 		routeData[6] = r.vehicleCapacity;
   	 		routeData[7] = (r.captureTime != null ) ? r.captureTime.toGMTString() : "";
   	 		
   	 		rotuesCsvWriter.writeNext(routeData);
  
   	 		List<TripPatternStop> stops = TripPatternStop.find("pattern.route = ?", r).fetch();
   	 		
   	 		System.out.println("route: " + r.id.toString());
   	 		
   	 		for(TripPatternStop s : stops) {
   	 		
   	 			String[] stopData = new String[stopsHeader.length];
   	 			
   	 			stopData[0] = r.id.toString();
   	 			stopData[1] = s.stopSequence.toString();
   	 			stopData[2] = "" + s.stop.location.getCoordinate().y;
   	 			stopData[3] = "" + s.stop.location.getCoordinate().x;
   	 			stopData[4] = "" + s.defaultTravelTime;
   	 			stopData[5] = "" + s.defaultDwellTime;
   	 			stopData[6] = "" + s.board;
   	 			stopData[7] = "" + s.alight;
   	 			
   	 			stopsCsvWriter.writeNext(stopData);
   	 			
   	 		}
   	 	}
		   	 	
   	 	rotuesCsvWriter.flush();
   	 	rotuesCsvWriter.close();
   	 	
   	 	stopsCsvWriter.flush();
   	 	stopsCsvWriter.close();
   	 	
   	 	DirectoryZip.zip(outputDirectory, outputZipFile);
   	 	FileUtils.deleteDirectory(outputDirectory); 
   	 	
   	 	redirect("/public/data/exports/" + timestamp + ".zip");
    }
}