package com.example.bureau.testhorlogesimple;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import java.util.Calendar;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    final String[] positionIDAll = {"Famille","Travail","Voyage","Dehors","Joker","Pense à Vous","Maison","Pas de Nouvelles","Veux rentrer"};
    SmsSender smsSender;

    //Preferences
    public static final String MY_PREF="mesPrefs";
    SharedPreferences myVar;
    SharedPreferences.Editor myVarEditor;
    String aigLastPosit;
    String aigNewPosit;//New Hand position

    //Notifications
    String nTitle;
    String nContent;
    String nTicker;

    @Override
    public void onReceive(Context context, Intent intent) {
        int ID = intent.getIntExtra("PositionID",-1);
        Log.d(TAG, "Alarm triggered: "+positionIDAll[ID]);
        //Get the preferences registered
        myVar=context.getSharedPreferences(MY_PREF,context.MODE_PRIVATE);
        Boolean notifDisplay=myVar.getBoolean("notifsEnable",true);
        myVarEditor = myVar.edit();
        //Create sms sender function
        smsSender=new SmsSender(context);
        //Check if Alarm should be triggered
        if(myVar.getBoolean(positionIDAll[ID]+"CalDef",false)){
            boolean startAlarmType=intent.getBooleanExtra("StartAlarmType",false);
            if(startAlarmType){
                Log.i(TAG, "StartEvent");
                myVarEditor.putBoolean(positionIDAll[ID]+"CalIn",true);
                nTitle = "Alarm - Start Event";
            }else{
                Log.i(TAG, "EndEvent");
                myVarEditor.putBoolean(positionIDAll[ID]+"CalIn",false);
                nTitle = "Alarm - End Event";
            }
            nContent = positionIDAll[ID];
            nTicker = positionIDAll[ID];
            myVarEditor.apply();
            new NotificationSender().Build(context,nTitle,nContent,nTicker);
        }else{
            Log.e(TAG,"Alarm triggered but position not define on calendar");
        }

    }

    public void setElapsedAlarm(Context context,int duration,int ID) {
        AlarmManager am =(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, ID, intent, 0);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+duration, 1000 * 60 * 10, pi); // Millisec * Second * Minute
    }

    public void setRepeatedAlarm(Context context,Calendar calendarAlarm,Boolean StartAlarmType,int ID){
        int dayOfWeek=calendarAlarm.get(Calendar.DAY_OF_WEEK);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        myIntent.putExtra("StartAlarmType",StartAlarmType);//Register inside intent if it's the start of the event or it's end
        myIntent.putExtra("PositionID",ID);//Register ID position inside intent
        int cDay=Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int cHour=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int cMin=Calendar.getInstance().get(Calendar.MINUTE);

        int aDay=calendarAlarm.get(Calendar.DAY_OF_WEEK);
        int aHour=calendarAlarm.get(Calendar.HOUR_OF_DAY);
        int aMin=calendarAlarm.get(Calendar.MINUTE);
        // Check we aren't setting it in the past which would trigger it to fire instantly
        if(calendarAlarm.getTimeInMillis() < System.currentTimeMillis()) {
            calendarAlarm.add(Calendar.DAY_OF_YEAR, 7);
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ID*100+dayOfWeek+BooleantoInt(StartAlarmType), myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendarAlarm.getTimeInMillis(), AlarmManager.INTERVAL_DAY * 7, pendingIntent);
    }

    public void cancelAlarm(Context context,int ID) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, ID, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        Log.i(TAG,"All Alarm cancel for "+positionIDAll[ID]);
        for(int i=0;i<10;i++){
            sender = PendingIntent.getBroadcast(context, ID*100+i*10, intent, 0);
            alarmManager.cancel(sender);
            sender = PendingIntent.getBroadcast(context, ID*100+i*10+1, intent, 0);
            alarmManager.cancel(sender);
        }
    }

    //Function to transform Boolean to String 1 or 0
    public static int BooleantoInt(boolean b) {
        return b ? 1 : 2;
    }
}