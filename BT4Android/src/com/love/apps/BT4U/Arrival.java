package com.love.apps.BT4U;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.util.Log;

public class Arrival implements Comparable 
{
	
	Calendar arrivalTime;
	String note;
	public Arrival(String time)
	{
		note = "";
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.applyPattern("M/d/y h:m:s a");
		
		try {
			arrivalTime = new GregorianCalendar();
			arrivalTime.setTime(sdf.parse(time));
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}
	public String timeUntil()
	{
		long msuntil = arrivalTime.getTimeInMillis() - System.currentTimeMillis();
		long seconds = (msuntil/(1000))%60;
		long hours   = msuntil/(60*60*1000);
		long minutes = (msuntil/(60*1000))%60;
		return (hours>0?hours+"h":"")+" "+(hours<=0 && minutes<=0 && seconds<30?"Now":(minutes>=1?minutes+"m":"1m"));
	}
	public boolean alreadyHappened()
	{
		long now = System.currentTimeMillis();
		return (now-arrivalTime.getTimeInMillis()>0);
	}
	public void setNote(String text) {
		note = text;
	}
	@Override
	public int compareTo(Object arg0) {
		try{
			return arrivalTime.compareTo(((Arrival)arg0).arrivalTime);
		}catch(Exception e)
		{
			Log.d("Widget","WTF");
			return 0;
		}
	}
}
