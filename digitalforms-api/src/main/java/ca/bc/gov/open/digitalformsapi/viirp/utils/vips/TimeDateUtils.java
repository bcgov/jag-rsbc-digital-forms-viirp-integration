package ca.bc.gov.open.digitalformsapi.viirp.utils.vips;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import ca.bc.gov.open.digitalformsapi.viirp.utils.DigitalFormsConstants;

/**
 * Time Date Utils 
 * 
 * Example ISO 8601 Time/Date
 * 2018-02-26T12:43:01.122-08:00
 * 
 * Example ISO 8601 Date
 * 2018-02-26T00:00:00.000-08:00
 * 
 * @author smillar
 *
 */
public class TimeDateUtils {

	/**
	 * Java Date object to ISO 8601
	 * 
	 * @param date
	 * @return
	 */
	public static String getISO8601FromDate(Date date) {
		DateTime dateTime = new DateTime(date);
		DateTimeFormatter isoFormat = ISODateTimeFormat.dateTime();
		return dateTime.toString(isoFormat);
	}

	/**
	 * String ISO8601 date to Java Date object
	 * 
	 * @param date
	 * @return
	 */
	public static Date getDateFromISO8601(String sdate) {		
		if (null == sdate || "".equals(sdate)) {
			return null; 
		} else {
			Calendar calDate = DatatypeConverter.parseDateTime(sdate);
			return calDate.getTime();
		}
	}
	
	/**
	 * Converts a Java date to ICBC date format (No time, format YYYYMMDD) 
	 * @param date
	 */
	public static String DateToICBCDateFormat(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(DigitalFormsConstants.ICBC_DATE_FORMAT);
		return formatter.format(date);
	}

	/**
	 * Used for debugging etc. 
	 * 
	 * @param date
	 * @return
	 */
	public static String getDisplayDate(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(DigitalFormsConstants.VIPS_DISPLAY_DATE_FORMAT);
		return formatter.format(date);
	}

}
