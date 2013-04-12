package jobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hibernatespatial.readers.Feature;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.geotools.data.FileDataStoreFinder;

import com.mchange.v2.c3p0.impl.DbAuth;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.operation.linemerge.LineMerger;

import models.transit.Agency;
import models.transit.Route;
import models.transit.ServiceCalendar;
import models.transit.ServiceCalendarDate;
import models.transit.Stop;
import models.transit.StopTime;
import models.transit.TripPattern;
import models.transit.TripPatternStop;
import models.transit.TripShape;
import models.transit.Trip;


import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import utils.DirectoryZip;
import utils.FeatureAttributeFormatter;

public class ProcessGisExport extends Job {

	private Long _patternId;

	
	public ProcessGisExport(Long patternId)
	{
		this._patternId = patternId;
	}
	
	public void doJob() throws InterruptedException {
		
		String exportName = "pattern_" + this._patternId;
		
		TripPattern pattern = null;
		while(pattern == null)
    	{
			Thread.sleep(1000);
			pattern = TripPattern.findById(this._patternId);
    		
    		Logger.info("Waiting for TripPattern object...");
    	}
		
		File outputZipFile = new File(Play.configuration.getProperty("application.publicGisDataDirectory"), exportName + ".zip");
		
		File outputDirectory = new File(Play.configuration.getProperty("application.publicGisDataDirectory"), exportName);
		
        try
        {
        	if(!outputDirectory.exists())
        	{
        		outputDirectory.mkdir();
        	}
        	
        	processStops(outputDirectory, exportName);
        	processRoute(outputDirectory, exportName);
    	
        	DirectoryZip.zip(outputDirectory, outputZipFile);
        	FileUtils.deleteDirectory(outputDirectory); 
        }
        catch(Exception e)
        {	
        	Logger.error("Unable to process GIS export: ", e.toString());
        	e.printStackTrace();
        } 
        
	}
	
	private void processStops(File outputDirectory, String exportName) throws Exception {
		
		File outputShapefile = new File(outputDirectory, exportName + "_stops.shp");
		
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", outputShapefile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		
		ShapefileDataStore dataStore = (ShapefileDataStore)dataStoreFactory.createNewDataStore(params);
		dataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

    	SimpleFeatureType STOP_TYPE = DataUtilities.createType(
                "Stop",                 
                "location:Point:srid=4326," + 
                "id:String," +
                "sequence:Integer," +
                
                "travelTime:Integer," +
                "dwellTime:Integer," +
                
                "arrivalTime:Integer," +
                "departureTime:Integer"
                    
        );
    	
    	SimpleFeatureCollection collection = FeatureCollections.newCollection();

        SimpleFeatureBuilder featureBuilder = null;
        
    	dataStore.createSchema(STOP_TYPE);
    	featureBuilder = new SimpleFeatureBuilder(STOP_TYPE);
    	
    	TripPattern tp = TripPattern.findById(this._patternId);
    	
		List<TripPatternStop> patternStops = TripPatternStop.find("pattern = ? order by stopSequence", tp).fetch();
		
		Integer cumulativeTime = 0;
		
		for(TripPatternStop tps : patternStops)
    	{
    		featureBuilder.add(tps.stop.location);
    		
            featureBuilder.add(tps.stop.id.toString());
            featureBuilder.add(tps.stopSequence);
            
            featureBuilder.add(tps.defaultTravelTime);
            featureBuilder.add(tps.defaultDwellTime);
            
            if(tps.defaultTravelTime != null)
            cumulativeTime += tps.defaultTravelTime;
            featureBuilder.add(cumulativeTime);
            
            if(tps.defaultDwellTime != null)
            cumulativeTime += tps.defaultDwellTime;
            featureBuilder.add(cumulativeTime);
            
            SimpleFeature feature = featureBuilder.buildFeature(null);
            collection.add(feature);	
    	}
    
        Transaction transaction = new DefaultTransaction("create");

        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) 
        {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
           
            featureStore.addFeatures(collection);
            transaction.commit();

            transaction.close();
        } 
        else 
        {
        	throw new Exception(typeName + " does not support read/write access");
        }      
	}
	
	private void processRoute(File outputDirectory, String exportName) throws Exception {
		
		File outputShapefile = new File(outputDirectory, exportName + "_route.shp");
		
		ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
		
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("url", outputShapefile.toURI().toURL());
		params.put("create spatial index", Boolean.TRUE);
		
		ShapefileDataStore dataStore = (ShapefileDataStore)dataStoreFactory.createNewDataStore(params);
		dataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

    	SimpleFeatureType ROUTE_TYPE = DataUtilities.createType(
                "Route",                   // <- the name for our feature type
                "route:LineString:srid=4326," +
                "id:String," +
                "name:String," +
                "desc:String," +
                "notes:String"
                     
        );
    	
    	SimpleFeatureCollection collection = FeatureCollections.newCollection();

        SimpleFeatureBuilder featureBuilder = null;
        
        dataStore.createSchema(ROUTE_TYPE);
       	featureBuilder = new SimpleFeatureBuilder(ROUTE_TYPE);
    	
       	TripPattern tp = TripPattern.findById(this._patternId);
		
       	if(tp.shape == null)
			return;
	
		featureBuilder.add(tp.shape.shape);
		featureBuilder.add(tp.name);
	    featureBuilder.add(tp.route.routeDesc);
	    featureBuilder.add(tp.route.routeNotes);
	      
        SimpleFeature feature = featureBuilder.buildFeature(null);
        collection.add(feature);	
    	
        Transaction transaction = new DefaultTransaction("create");

        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) 
        {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
           
            featureStore.addFeatures(collection);
            transaction.commit();

            transaction.close();
        } 
        else 
        {
        	throw new Exception(typeName + " does not support read/write access");
        }
	}
}

