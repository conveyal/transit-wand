package models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import play.db.jpa.Model;

@Entity
public class Phone extends Model {

	public String imei;

	public String unitId;
	
	public String userName;
	
	public Date registeredOn;
	
	public static Phone registerImei(String imei, String userName) {
		
		// check for existing imei record
		Phone phone = Phone.find("imei = ?",  imei).first();
		if(phone != null)
			return phone;
		
		// create a new record
		
		phone = new Phone();
		
		// generate "random" six digit unit ID
		Random generator = new Random();  
		generator.setSeed(System.currentTimeMillis());  
		
		Integer id = 0;

		do {
			id = generator.nextInt(899999) + 100000;
		} while(Phone.count("unitId = ?", id.toString()) > 0);
		
		phone.unitId = id.toString();
		phone.imei = imei;
		phone.userName = userName;
		phone.registeredOn = new Date();
		
		phone.save();
		
		return phone.save();
	}

}
 