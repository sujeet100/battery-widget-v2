package com.mavedev.battery;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import junit.framework.Assert;

import java.util.Date;

public class BatteryMonitorService extends Service {

	private static final boolean CHARGING = true;
	private static final boolean DISCHARGING = false;
	protected static final long DEFAULT_CHARGING_DELTA = 1*60;
	protected static final long DEFAULT_DISCHARGING_DELTA = 5*60;
	protected static final long DEFAULT_USB_CHARGING_DELTA = 4*60;
	
	private Integer batteryLevel = 0;
	private boolean isCharging = DISCHARGING;
	private boolean previousChargingState = DISCHARGING;
	private int chargePlug;
	private long chargingDelta = DEFAULT_CHARGING_DELTA;
	private long dischargingDelta = DEFAULT_DISCHARGING_DELTA;
	private Date batteryStateTime;
	
	BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {


		@Override
		public void onReceive(Context context, Intent intent) {
			int currentLevel = calculateBatteryLevel(context);
			if(isChargingStateChanged() || batteryStateTime == null){
				if(chargePlug == BatteryManager.BATTERY_PLUGGED_AC){
					chargingDelta = DEFAULT_CHARGING_DELTA;
				}else{
					chargingDelta = DEFAULT_USB_CHARGING_DELTA;
				}
				dischargingDelta = DEFAULT_DISCHARGING_DELTA;
			}else{
				if(currentLevel > batteryLevel){
					chargingDelta = (new Date().getTime() - batteryStateTime.getTime())/(1000);
				}else if(currentLevel < batteryLevel){
					dischargingDelta = (new Date().getTime() - batteryStateTime.getTime())/(1000);
				}
			}
			batteryStateTime = new Date();
			batteryLevel = currentLevel;
			updateViews(context);

		}

	};

	private int calculateBatteryLevel(Context context) {

		Intent batteryIntent = context.getApplicationContext()
				.registerReceiver(null,
						new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
		int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		
		previousChargingState = isCharging;
		isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
				|| status == BatteryManager.BATTERY_STATUS_FULL;

		if(isCharging){
			chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		}
		return level * 100 / scale;
	}

	private boolean isChargingStateChanged(){
		if(previousChargingState == isCharging){
			return false;
		}else{
			return true;
		}
	}

	private void updateViews(Context context) {

        Bitmap bitmap = getBatteryWidgetBitmap(0xFFFFFFFF);

		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.widget_layout);
		views.setImageViewBitmap(R.id.canvas, bitmap);

		ComponentName componentName = new ComponentName(context,
				BatteryWidget.class);
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);

		Intent configIntent = new Intent(context, BatteryWidgetConfigure.class);
		PendingIntent configPendingIntent = PendingIntent.getActivity(context,
				0, configIntent, 0);
		views.setOnClickPendingIntent(R.id.canvas, configPendingIntent);
		appWidgetManager.updateAppWidget(componentName, views);

		updateNotification();
	}

    private Bitmap getBatteryWidgetBitmap(int outerCircleColor) {
        Bitmap bitmap = Bitmap.createBitmap(300, 300, Config.ARGB_8888);
        final int circleStroke = bitmap.getHeight() / 20;
        final int PADDING = circleStroke * 2;

        // Outer circle style
        Paint outerCirclePaint = new Paint();
        outerCirclePaint.setAntiAlias(true);
        outerCirclePaint.setStyle(Style.STROKE);
        outerCirclePaint.setStrokeWidth(circleStroke);

        // outer circle color based on battery percentage
        outerCirclePaint.setColor(getOuterCircleColor(outerCirclePaint));

        // inner circle style
        Paint innerCirclePaint = new Paint();
        innerCirclePaint.setAntiAlias(true);
        innerCirclePaint.setStyle(Style.STROKE);
        innerCirclePaint.setStrokeWidth(circleStroke / 3);
        innerCirclePaint.setColor(outerCircleColor);

        // battery level text style
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Style.FILL_AND_STROKE);
        textPaint.setStrokeWidth(bitmap.getHeight() / 100);
        int textFontSize = batteryLevel == 100 ? bitmap.getHeight() / 5
                : bitmap.getHeight() / 4;
        textPaint.setTextSize(textFontSize);
        textPaint.setColor(outerCircleColor);

        Canvas canvas = new Canvas(bitmap);
        RectF box = new RectF(0 + PADDING, 0 + PADDING, bitmap.getWidth()
                - PADDING, bitmap.getHeight() - PADDING);

        // inner circle
        Path innerCircle = new Path();
        innerCircle.addArc(box, -90, 360);
        canvas.drawPath(innerCircle, innerCirclePaint);

        // outer circle
        float sweep = 360 * batteryLevel * 0.01f;
        Path outerCircle = new Path();
        outerCircle.addArc(box, -90, sweep);
        canvas.drawPath(outerCircle, outerCirclePaint);

        // battery level
        int textX = bitmap.getHeight() / 15 + PADDING + 2 * circleStroke;
        if (batteryLevel < 10) {
            textX = (int) (textX * 1.5);
        }
        canvas.drawText(batteryLevel + "%", textX, bitmap.getHeight() / 2
                + textFontSize / 2, textPaint);
        return bitmap;
    }

    private int getOuterCircleColor(Paint outerCirclePaint) {
        if (isCharging) {
            return 0xFF92CD00;
        }
        if (batteryLevel >= BatteryWidgetConfigure.warningLevel) {
            return 0xFF2EA8D9;
        } else if (batteryLevel < BatteryWidgetConfigure.warningLevel
                && batteryLevel >= BatteryWidgetConfigure.criticalLevel) {
            return 0xFFFF9900;
        } else if (batteryLevel < BatteryWidgetConfigure.criticalLevel) {
            return 0xFFFF0000;
        }
        return 0;
    }

    private void updateNotification() {
        Bitmap bitmap = getBatteryWidgetBitmap(0xCCB4B4B4);
		int batteryImageId = getDrawable(this, "white_round_"+batteryLevel);
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
        Canvas canvas = new Canvas(bitmap);

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle(getBatteryRemainingText())
				.setContentText(getChargingText())
				.setSmallIcon(batteryImageId)
				.setLargeIcon(scaledBitmap)
				.setOngoing(true);


		// Creates an Intent for the Activity
		Intent intentBatteryUsage = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);

	    PendingIntent resultPendingIntent =
	        PendingIntent.getActivity(
	        this,
	        0,
	        intentBatteryUsage,
	        PendingIntent.FLAG_UPDATE_CURRENT
	    );
	    
	    builder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, builder.build());
	}

	private String getChargingText() {
		if(isCharging && chargePlug == BatteryManager.BATTERY_PLUGGED_AC){
			return "Charging - AC";
		}else if(isCharging && chargePlug == BatteryManager.BATTERY_PLUGGED_USB){
			return "Charging - USB";
		}else if(!isCharging){
			return "Not Charging";
		}else{
			return "Charging";
		}
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startUp(intent);
		return START_STICKY;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		startUp(intent);
	}

	private void startUp(Intent intent) {
		registerListeners();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(batteryLevelReceiver);
	}

	private void registerListeners() {

		IntentFilter batteryLevelFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static int getDrawable(Context context, String name) {
		Assert.assertNotNull(context);
		Assert.assertNotNull(name);

		return context.getResources().getIdentifier(name, "drawable",
				context.getPackageName());
	}

	private String getBatteryRemainingText(){
		if(isCharging){
			int timeTakenToCharge =  (int) (chargingDelta * (100-batteryLevel));
			return "Battery Full in "+Utils.getHoursAndMinutes(timeTakenToCharge);
		}else{
			int timeTakenToDischarge =  (int) (dischargingDelta * batteryLevel);
			return "Battery empty in "+ Utils.getHoursAndMinutes(timeTakenToDischarge);
		}
	}
	
	
	
}
