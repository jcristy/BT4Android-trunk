package com.love.apps.BT4U;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.love.apps.BT4U.Updates.NewsItem;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

public class WidgetConfigure extends Activity 
{
	private int refresh_seconds = 5*60;
	public void onCreate(Bundle saved)
	{
		super.onCreate(saved);
		this.setContentView(R.layout.widget_configure_layout);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		
		FileRead reader = new FileRead();
		Resources myResource = getResources();
		reader.readFromFile(myResource);
		final Map<String, String> stops = new HashMap<String,String>();
		
		final Map<String, String> routes = new HashMap<String,String>();
		Map<String, String> routes_actual = new HashMap<String,String>();
		Routes.setUpRoutes(routes, routes_actual);
		
		Set<String> set_routes = routes.keySet();
		final Spinner route_select = (Spinner)findViewById(R.id.route_select);
		final ArrayAdapter<String> routes_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
		
		//routes_adapter.addAll(set_routes); //Min SDK Issue
		for (String route: set_routes)
		{
			routes_adapter.add(route);
		}
		routes_adapter.notifyDataSetChanged();
		route_select.setAdapter(routes_adapter);
		routes_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
		final Spinner stop_select = (Spinner) findViewById(R.id.stop_select);
		final ArrayAdapter<String> stops_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
		stops_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		stop_select.setEnabled(false);
		
		route_select.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) 
			{
				String route = routes_adapter.getItem(position);
				route = routes.get(route);
				String url = ("http://www.bt4u.org/BT4U_WebService.asmx/GetScheduledStopCodes?routeShortName=" + route);
				stops_adapter.clear();
				HttpGet httpRequest = new HttpGet(url);
				AsyncTask<HttpGet,Integer,String> at = new AsyncTask<HttpGet,Integer,String>(){

					@Override
					protected String doInBackground(HttpGet... request) 
					{
						try{
							HttpClient httpClient = new DefaultHttpClient();
							HttpResponse response = httpClient.execute(request[0]);
							InputStream in = response.getEntity().getContent();
							InputStreamReader ir = new InputStreamReader(in);
							BufferedReader bin = new BufferedReader(ir);
							String line = null;
							StringBuffer buff = new StringBuffer();
							while((line = bin.readLine())!=null){
								buff.append(line+"\n");
							}
							bin.close();	
							return buff.toString();
						}catch(Exception e)
						{
							
							return null;
						}
					}
					String resultsSoFar = "";
					protected void onPostExecute(String result) {
						resultsSoFar = resultsSoFar + result;
						Log.d("Configure","executed");
						if (result!=null && resultsSoFar.contains("</DocumentElement>"))
						{
							
							try{
								SAXParserFactory factory = SAXParserFactory.newInstance();
								SAXParser saxParser = factory.newSAXParser();
								DefaultHandler handler = new DefaultHandler(){
									String[] data = {"",""};
									
									int index = -1;
									
									public void startElement(String uri, String localName,String qName, 
							                org.xml.sax.Attributes attributes) throws SAXException {
										if (qName.equals("ScheduledStops"))
										{
											data = new String[2];
											for (int i=0; i<data.length; i++)
											{
												data[i] = "";
											}
											index = -1;
										}
										else if (qName.equals("StopCode"))
										{
											index = 0;
										}
										else if (qName.equals("StopName"))
										{
											index = 1;
										}
										else 
										{
											index = -1;
										}
									}
									public void endElement(String uri, String localName, String qName) 
									{
										if (qName.equals("ScheduledStops"))
										{
											String NameToShow = data[0].trim()+" - "+data[1];
											stops_adapter.add(NameToShow);
											
											stops.put(NameToShow, data[0].trim());
											index = -1;
										}
									}
									public void characters(char[] ch, int start, int length)  
									{
										if (index !=-1)
											data[index] = data[index] + new String(ch,start,length);
									}
									@Override
									public void endDocument()
									{
										stops_adapter.notifyDataSetChanged();
										stop_select.setEnabled(true);
										stop_select.setAdapter(stops_adapter);
										Log.d("Configure","document complete");
									}
								};
								
								saxParser.parse(new ByteArrayInputStream(resultsSoFar.getBytes()), handler);
							}catch(Exception e){e.printStackTrace();};
							
						}
						else
						{
							
						}
					}
					
				};
				at.execute(httpRequest);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		final Spinner refresh_select = (Spinner) findViewById(R.id.refresh_select);
		final ArrayAdapter<String> refresh_adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
		String [] timeOptions ={"5 minutes","15 minutes","30 minutes","1 Hour"}; 
		//refresh_adapter.addAll();//Can't use for old phone support
		for (int i=0; i<timeOptions.length;i++)
			refresh_adapter.add(timeOptions[i]);
		refresh_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		refresh_select.setEnabled(true);
		refresh_select.setAdapter(refresh_adapter);
		refresh_seconds = 5*60;
		refresh_select.setOnItemSelectedListener(new OnItemSelectedListener(){
			
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) 
			{
				switch (arg2)
				{
				case 0://5's
					refresh_seconds = 5*60;
					break;
				case 1://15's
					refresh_seconds = 15*60;
					break;
				case 2://30's
					refresh_seconds = 30*60;
					break;
				case 3://60's
					refresh_seconds = 60*60;
					break;
				default://TODO some kind of error message
					
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		if (extras != null) {
		    final int mAppWidgetId = extras.getInt( AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		    
			findViewById(R.id.done_button).setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(WidgetConfigure.this);
					RemoteViews views = new RemoteViews(WidgetConfigure.this.getPackageName(), R.layout.widget_layout);
					appWidgetManager.updateAppWidget(mAppWidgetId, views);
					final Intent resultValue = new Intent();
					resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
					
					SharedPreferences sp = getSharedPreferences("widget_"+mAppWidgetId,0);
					SharedPreferences.Editor ed = sp.edit();
					ed.putString("route",routes.get((String)route_select.getSelectedItem()));
					ed.putString("stop",stops.get((String)stop_select.getSelectedItem()));
					ed.putInt("refresh", refresh_seconds);
					ed.putBoolean("smart", ((CheckBox)findViewById(R.id.smart_checkbox)).isChecked());
					Log.d("Widget", "Refresh set to "+refresh_seconds);
					Log.d("Widget", "Smart "+(((CheckBox)findViewById(R.id.smart_checkbox)).isChecked()?"true":"false"));
					ed.commit();
					
					
					Intent intent = new Intent(Widget.UPDATE_WIDGET);
					   PendingIntent pendingIntent = PendingIntent.getBroadcast(WidgetConfigure.this, 0, intent, 0);
					   AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
					   Calendar calendar = Calendar.getInstance();
					   calendar.setTimeInMillis(System.currentTimeMillis());
					   calendar.add(Calendar.SECOND, 2);
					   //Run every 45 s to update the GUI when the phone is awake.  Let the service determine when to ask the internet based on the settings
					   //alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
					   Widget.lastUpdate.clear();
					   if (Widget.alarmManager==null)
					   {
					   		alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 45*1000, pendingIntent);
					   
					   		Widget.SaveAlarmManager(alarmManager, pendingIntent);
					   }
					
					setResult(RESULT_OK, resultValue);
					finish();
					
				}
				
			});
				
		}
	}
}
