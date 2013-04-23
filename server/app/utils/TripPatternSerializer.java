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

public class TripPatternSerializer implements JsonSerializer<TripPattern>  {

		@Override
		public JsonElement serialize(TripPattern arg0, Type arg1, JsonSerializationContext arg2) 
		{
	    	JsonObject jsonObject = new JsonObject();
	    	
	    	    	
	    	jsonObject.addProperty("id", arg0.id);
	    	jsonObject.addProperty("headsign", arg0.headsign);
	    
	    	
	    	return jsonObject;
		}
}
