package utils;

import java.lang.reflect.Type;
import java.util.List;

import utils.EncodedPolylineBean;
import utils.PolylineEncoder;

import models.transit.Stop;
import models.transit.TripPattern;
import models.transit.TripPatternStop;
import models.transit.TripShape;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class TripPatternShapeSerializer implements JsonSerializer<TripPattern>  {

		@Override
		public JsonElement serialize(TripPattern arg0, Type arg1, JsonSerializationContext arg2) 
		{
	    	JsonObject jsonObject = new JsonObject();
	    	
	    	    	
	    	jsonObject.addProperty("id", arg0.id);
	    	jsonObject.addProperty("name", arg0.name);
	    	jsonObject.addProperty("headsign", arg0.headsign);
	    	
	    	jsonObject.addProperty("color", arg0.route.agency.color);
	    	jsonObject.addProperty("gtfsAgencyId", arg0.route.agency.gtfsAgencyId);
	    	jsonObject.addProperty("routeShortName", arg0.route.routeShortName);
	    	jsonObject.addProperty("routeLongName", arg0.route.routeLongName);
	    	jsonObject.addProperty("routeDescription", arg0.route.routeDesc);
	    	jsonObject.addProperty("routeNotes", arg0.route.routeNotes);
	    	
	    	jsonObject.addProperty("headsign", arg0.headsign);
	    	
	    	JsonObject jsonBoundsObject = new JsonObject();
	    	
	    	jsonBoundsObject.addProperty("lat1", arg0.shape.shape.getEnvelopeInternal().getMaxY());
	    	jsonBoundsObject.addProperty("lon1", arg0.shape.shape.getEnvelopeInternal().getMaxX());

	    	jsonBoundsObject.addProperty("lat2", arg0.shape.shape.getEnvelopeInternal().getMinY());
	    	jsonBoundsObject.addProperty("lon2", arg0.shape.shape.getEnvelopeInternal().getMinX());
	    	
	    	jsonObject.add("bounds", jsonBoundsObject);
	    	

	    	JsonArray jsonArray = new JsonArray();
	    	
	    	List<TripPatternStop> tpStops = TripPatternStop.find("pattern = ?", arg0).fetch();
	    	
	    	for(TripPatternStop tps : tpStops) {
	    		
	    		JsonObject jsonTpsObject = new JsonObject();
	    		
	    		jsonTpsObject.addProperty("lat", tps.stop.location.getY());
	    		jsonTpsObject.addProperty("lon", tps.stop.location.getX());
	    		jsonTpsObject.addProperty("board", tps.board);
	    		jsonTpsObject.addProperty("alight", tps.alight);
	    		jsonTpsObject.addProperty("travelTime", tps.defaultTravelTime);
	    		
	    		jsonArray.add(jsonTpsObject);
	    	}
	    	
	    	jsonObject.add("stops", jsonArray);
	    	
	    	EncodedPolylineBean polyline = PolylineEncoder.createEncodings(arg0.shape.shape);
	    	
	    	jsonObject.addProperty("shape", polyline.getPoints());
	    	
	    	return jsonObject;
		}
}
