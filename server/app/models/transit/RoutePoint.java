package models.transit;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Query;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.hibernate.annotations.Type;

import com.conveyal.transitwand.TransitWandProtos.Upload;

import play.Logger;
import play.db.jpa.Model;


@JsonIgnoreProperties({"entityId", "persistent"})
@Entity
public class RoutePoint extends Model {

    @ManyToOne
    public Route route;
    
    public Double lat;
    public Double lon;
    
    public Integer sequence;
    
    public Integer timeOffset;
    
    
    
    public static void addRoutePoint(Upload.Route.Point p, Long routeId, Integer sequence)
    {
    	Query idQuery = RoutePoint.em().createNativeQuery("SELECT NEXTVAL('hibernate_sequence');");
    	BigInteger routePointId = (BigInteger)idQuery.getSingleResult();

	
		Query q = RoutePoint.em().createNativeQuery("INSERT INTO routepoint (id, route_id, sequence, lat, lon, timeoffset)" +
	    	"  VALUES(?, ?, ?, ?, ?, ?);");
		
		q.setParameter(1, routePointId.longValue())
		 .setParameter(2, routeId)
		 .setParameter(3, sequence)
		 .setParameter(4, p.getLat())
		 .setParameter(5, p.getLon())
		 .setParameter(6, p.getTimeoffset());

		q.executeUpdate();

    }
}

