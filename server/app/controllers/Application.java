package controllers;

import play.*;
import play.mvc.*;
import utils.TripPatternSerializer;
import utils.TripPatternShapeSerializer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.*;

import javax.persistence.EntityManager;

import jobs.ProcessGisExport;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.conveyal.transitwand.TransitWandProtos.Upload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import models.*;
import models.transit.Agency;
import models.transit.Route;
import models.transit.RouteType;
import models.transit.Stop;
import models.transit.TripPattern;
import models.transit.TripPatternStop;
import models.transit.TripShape;

public class Application extends Controller {

	
    public static void index(Boolean invalid) {
        render(invalid);
    }
    
    public static void export(Long patternId) throws InterruptedException {
    	ProcessGisExport gisExport = new ProcessGisExport(patternId);
    	gisExport.doJob();
    	ok();
    }
    
    public static void exportAll() throws InterruptedException {
    	
    	List<TripPattern> tripPatterns =  TripPattern.findAll();
    	for(TripPattern tp: tripPatterns) {
    		ProcessGisExport gisExport = new ProcessGisExport(tp.id);
    		gisExport.doJob();
    	}
    	
    	ok();
    }

   
    public static void register(String imei, String userName) {
        
    	Phone p = Phone.registerImei(imei, userName);
    	
    	renderJSON(p);
    }
    
    public static void upload(String imei, File data) {
    	
    	try {
			
			byte[] dataFrame = new byte[(int)data.length()];;
			DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(data)));
		
			dataInputStream.read(dataFrame);			
			Upload upload = Upload.parseFrom(dataFrame);
			
			
			Phone phone = Phone.find("imei = ?", imei).first();
			if(phone == null)
				badRequest();
			
			
			for(Upload.Route r : upload.getRouteList()) {
				
				Agency a = Agency.find("gtfsAgencyId = ?", "DEFAULT").first();
				Route route = new Route("", r.getRouteName(), RouteType.BUS, r.getRouteDescription(),  a);
				route.phone = phone;
				route.routeNotes = r.getRouteNotes();
				route.save();
				
				List<String> points = new ArrayList<String>();
	        	
	        	for(Upload.Route.Point p : r.getPointList()) {
	        		points.add(new Double(p.getLon()).toString() + " " + new Double(p.getLat()).toString());
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
	        	
	        	ProcessGisExport gisExport = new ProcessGisExport(tp.id);
	        	gisExport.doJob();
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
    	
    	TripPattern pattern = TripPattern.findById(patternId);
    	
    	Gson gson = new GsonBuilder().registerTypeAdapter(TripPattern.class, new TripPatternShapeSerializer()).serializeSpecialFloatingPointValues().serializeNulls().create();
        
    	renderJSON(gson.toJson(pattern));
    
    }
    
    public static void export() {
        render();
    }

}