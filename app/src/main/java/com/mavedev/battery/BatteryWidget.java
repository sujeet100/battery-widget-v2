package com.mavedev.battery;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;


public class BatteryWidget extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		updateView(context, appWidgetManager, appWidgetIds[0]);
	}

	public static void updateView(Context context, AppWidgetManager manager, int widgetId){
		context.startService(new Intent(context, BatteryMonitorService.class));
	}
}
