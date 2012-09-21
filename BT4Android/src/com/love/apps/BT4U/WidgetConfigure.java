package com.love.apps.BT4U;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RemoteViews;

public class WidgetConfigure extends Activity 
{
	public void onCreate(Bundle saved)
	{
		super.onCreate(saved);
		this.setContentView(R.layout.widget_configure_layout);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
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
					//resultValue.putExtra("Route", "HWD");
					//resultValue.putExtra("Stop", ""+1206);
					//SharedPreferences sp = new SharedPreferences();
					
					
					Intent intent = new Intent(Widget.UPDATE_WIDGET);
					   PendingIntent pendingIntent = PendingIntent.getBroadcast(WidgetConfigure.this, 0, intent, 0);
					   AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
					   Calendar calendar = Calendar.getInstance();
					   calendar.setTimeInMillis(System.currentTimeMillis());
					   calendar.add(Calendar.SECOND, 5);
					   alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 20*1000, pendingIntent);
					   
					   Widget.SaveAlarmManager(alarmManager, pendingIntent);
					   
					
					setResult(RESULT_OK, resultValue);
					finish();
					
				}
				
			});
				
		}
	}
}
