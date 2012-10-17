package com.love.apps.BT4U;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider 
{
	public final static String UPDATE_WIDGET = "BT4ANDROID_UPDATE";
	public static HashMap<Integer,Calendar> lastUpdate = new HashMap<Integer,Calendar>();
	public static HashMap<Integer,ArrayList<Arrival>> arrival_times = new HashMap<Integer,ArrayList<Arrival>>(); 
	public static HashMap<Integer,Object> dontRunFirstTime = new HashMap<Integer,Object>();
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
	{
		
		Log.d("Widget","onUpdate");
		final int N = appWidgetIds.length;
		
		for (int i=0; i<N;i++)
		{
			
			int appWidgetId = appWidgetIds[i];
			Log.d("Widget","Starting the loop for "+appWidgetId);
//			Intent intent = context.get
//			route = b.getString("Route");
//			stop  = b.getString("Stop");
			SharedPreferences prefs = context.getSharedPreferences("widget_"+appWidgetId,0);
			String stop = prefs.getString("stop", "1101");
			String route = prefs.getString("route","HWD");
			int refresh_rate = prefs.getInt("refresh", 30*60);
			boolean smart = prefs.getBoolean("smart", false);
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			updateWidget(views, appWidgetManager, context, appWidgetIds[i],stop,route,refresh_rate,smart);
		}
		
	}

	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		super.onReceive(context, intent);
		Log.d("Widget","receieved broadcast");
		if (UPDATE_WIDGET.equals(intent.getAction())) {
			 Bundle extras = intent.getExtras();
			   if(extras!=null) {
			    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			    ComponentName thisAppWidget = new ComponentName(context.getPackageName(), Widget.class.getName());
			    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

			    onUpdate(context, appWidgetManager, appWidgetIds);
			   }
		} 
	}
	
	public ArrayList<Arrival> getArrivals(String xml) throws XmlPullParserException, IOException, InterruptedException
	{
		ArrayList<Arrival> toReturn = new ArrayList<Arrival>();
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		xpp.setInput( new StringReader ( xml ) );
		int eventType = xpp.getEventType();
		String temp = "";
		
		int i = 0;
		boolean done = false;
		do{
			if(eventType == XmlPullParser.START_DOCUMENT) {
				temp += "";
			} else if(eventType == XmlPullParser.START_TAG) {
	
				String name = xpp.getName();
				if(name.equalsIgnoreCase("adjusteddeparturetime"))
				{
					xpp.next();
					Arrival arrival = new Arrival(xpp.getText());
					toReturn.add(arrival);
//					SimpleDateFormat sdf = new SimpleDateFormat();
//					sdf.applyPattern("h:mm");
//					temp+=sdf.format(arrival.arrivalTime)+"\t"+arrival.timeUntil();
//					
//					temp+="\n";
//					i++;
					
				}else if(name.equalsIgnoreCase("TripNotes"))
				{
					xpp.next();
					toReturn.get(toReturn.size()-1).setNote(xpp.getText());
					//temp+=xpp.getText()+"\n";
				}
			} else if(eventType == XmlPullParser.END_DOCUMENT) {
				done = true;
			} else if(eventType == XmlPullParser.TEXT) {
				temp += ("");
			}
			eventType = xpp.next();
		}while(!done);
		
		return toReturn;
	}
	public void updateWidget(RemoteViews views, AppWidgetManager appWidgetManager, Context context, int appWidgetId, String stop, String route, int refresh_rate, boolean smart)
	{
		//Determine if we need to go to the web
		Calendar now = new GregorianCalendar();
		Log.d("Widget", "Update"+appWidgetId+"'s Text @ "+(refresh_rate)+" Last update ago: "+(now.getTimeInMillis() - (lastUpdate.get(appWidgetId)==null?0:lastUpdate.get(appWidgetId).getTimeInMillis()))/1000);
		boolean goToWeb = false;
		
		ArrayList<Arrival> arrivals = arrival_times.get(Integer.valueOf(appWidgetId));
		if (arrivals != null) Collections.sort(arrivals);
		
		Log.d("Widget",arrivals!=null?"arrivals not null":"arrivals null");
		Log.d("Widget",smart?"smart":"not smart");
		if (arrivals!=null) Log.d("Widget","#arrivals "+arrivals.size());
		if (arrivals!=null) Log.d("Widget","time left: "+(arrivals.get(0).arrivalTime.getTimeInMillis() - now.getTimeInMillis()));
		
		if (lastUpdate.get(appWidgetId) == null)
			goToWeb=true;
		else if (now.getTimeInMillis() - lastUpdate.get(appWidgetId).getTimeInMillis()>refresh_rate*1000)
			goToWeb = true;
		else if (arrivals!=null && smart && arrivals.size()>=1 && (arrivals.get(0).arrivalTime.getTimeInMillis() - now.getTimeInMillis()<8*60*1000))//under 8 minutes refresh frequently
		{
			goToWeb = true;
			Log.d("Widget","Smart update");
		}
		//
		if (goToWeb)
		{
			views.setTextViewText(R.id.widget_departures, route+" "+stop+"\nLoading...");
			appWidgetManager.updateAppWidget(appWidgetId, views);
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpRequest = new HttpGet("http://bt4u.org/BT4U_WebService.asmx/GetNextDepartures?routeShortName="+route+"&stopCode="+stop);
			try {
				Log.d("Widget", "going to ask about"+route+" "+stop);
				HttpResponse response = httpClient.execute(httpRequest);
				InputStream in = response.getEntity().getContent();
				InputStreamReader ir = new InputStreamReader(in);
				BufferedReader bin = new BufferedReader(ir);
				String line = null;
				StringBuffer buff = new StringBuffer();
				while((line = bin.readLine())!=null){
					buff.append(line+"\n");
				}
				bin.close();	
				//String data = printXml(buff.toString());
				arrivals = getArrivals(buff.toString());
				Log.d("Widget", "Arrivals #"+arrivals.size());
				Collections.sort(arrivals);
				arrival_times.put(Integer.valueOf(appWidgetId), arrivals);
				lastUpdate.put(Integer.valueOf(appWidgetId),new GregorianCalendar());
			} catch (ClientProtocolException e) {
				views.setTextViewText(R.id.widget_departures, route+" "+stop+"\nError");
				e.printStackTrace();
			} catch (IOException e) {
				views.setTextViewText(R.id.widget_departures, route+" "+stop+"\nError");
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				views.setTextViewText(R.id.widget_departures, route+" "+stop+"\nError");
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String data="";
		Log.d("Widget", "Update Text");
		arrivals = arrival_times.get(Integer.valueOf(appWidgetId));
		SimpleDateFormat sdf = new SimpleDateFormat();
		if (arrivals == null)
		{
			
			data = "No data available";
		}
		else if (arrivals.size()==0)
		{
			 data = "No Rides";
		}
		else
		{
			for (Arrival arrival : arrivals)
			{
				if (arrival.alreadyHappened()) arrivals.remove(arrival);
				sdf.applyPattern("h:mm");
				data += sdf.format(arrival.arrivalTime.getTime())+"\t"+arrival.timeUntil()+"\r\n";
			}
		}
		String time = sdf.format(lastUpdate.get(Integer.valueOf(appWidgetId)).getTime());
		views.setTextViewText(R.id.widget_departures, route+" "+stop+" as of "+ time +"\n"+data);
		PendingIntent pi = PendingIntent.getActivity(context,0,new Intent(context, BT4Android.class),PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_departures, pi);
		
		Log.d("Widget","Here");
		views.setInt(R.id.widget_departures, "setBackgroundColor", R.color.widget_background);
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	public void onDisabled(Context context) {
		Log.d("Widget","Killed Alarm");
		if (alarmManager!=null)
			alarmManager.cancel(pendingIntent);
		alarmManager = null;
	}
	static AlarmManager alarmManager;
	static PendingIntent pendingIntent;
	
	public static void SaveAlarmManager(AlarmManager talarmManager,	PendingIntent tpendingIntent) {
		alarmManager = talarmManager;
		pendingIntent = tpendingIntent;
	}
	
}
