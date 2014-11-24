package com.mavedev.battery;

public class Utils {

	public static String getHoursAndMinutes(int seconds){
		if(seconds/3600 > 0){
			int hours = seconds/3600;
			int minutesRemaining =seconds % 3600 / 60;
			return hours+" Hrs, " + minutesRemaining +" minutes";
		}else{
			return seconds%3600/60 +" minutes";
		}
	}
}
