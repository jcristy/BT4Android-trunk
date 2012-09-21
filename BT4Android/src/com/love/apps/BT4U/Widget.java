package com.love.apps.BT4U;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;

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
	String route = "HWD";
	String stop = "1216";
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
	{
		Log.d("Widget","onUpdate");
		final int N = appWidgetIds.length;
		
		for (int i=0; i<N;i++)
		{
			Log.d("Widget","Starting the loop");
			int appWidgetId = appWidgetIds[i];
//			Intent intent = context.get
//			route = b.getString("Route");
//			stop  = b.getString("Stop");
			SharedPreferences prefs = context.getSharedPreferences("widget_"+appWidgetId,0);
			stop = prefs.getString("stop", "1101");
			route = prefs.getString("route","HWD");

			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			updateWidget(views, appWidgetManager, context, appWidgetIds[i]);
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
	
	public String printXml(String xml) throws XmlPullParserException, IOException, InterruptedException
	{

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
					SimpleDateFormat sdf = new SimpleDateFormat();
					sdf.applyPattern("h:mm");
					temp+=sdf.format(arrival.arrivalTime)+"\t"+arrival.timeUntil();
					
					temp+="\n";
					i++;
				}else if(name.equalsIgnoreCase("TripNotes"))
				{
					xpp.next();
					temp+=xpp.getText()+"\n";
				}
			} else if(eventType == XmlPullParser.END_DOCUMENT) {
				done = true;
			} else if(eventType == XmlPullParser.TEXT) {
				temp += ("");
			}
			eventType = xpp.next();
		}while(!done);
		if(temp.equals(""))
		{
			temp = ("There is no more route info available for today. \n\nYou should probably start walking.");
		}
		return temp;
	}
	public void updateWidget(RemoteViews views, AppWidgetManager appWidgetManager, Context context, int appWidgetId)
	{
		views.setTextViewText(R.id.widget_departures, route+" "+stop+"\nLoading...");
		appWidgetManager.updateAppWidget(appWidgetId, views);
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpRequest = new HttpGet("http://bt4u.org/BT4U_WebService.asmx/GetNextDepartures?routeShortName="+route+"&stopCode="+stop);
		try {
			Log.d("Widget", "going to ask!");
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
			String data = printXml(buff.toString());
			
			
			views.setTextViewText(R.id.widget_departures, route+" "+stop+"\n"+data);
			PendingIntent pi = PendingIntent.getActivity(context,0,new Intent(context, BT4Android.class),PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_departures, pi);
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
		Log.d("Widget","Here");
		views.setInt(R.id.widget_departures, "setBackgroundColor", R.color.widget_background);
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	public void onDisabled(Context context) {
		alarmManager.cancel(pendingIntent);
	}
	static AlarmManager alarmManager;
	static PendingIntent pendingIntent;
	public static void SaveAlarmManager(AlarmManager talarmManager,
			PendingIntent tpendingIntent) {
		Widget.alarmManager = talarmManager;
		Widget.pendingIntent = tpendingIntent;
		
	}
	
}
