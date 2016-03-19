package de.oerntec.votenote.Helpers.Notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Created by arne on 3/19/16.
 */
public class NotificationGeneralHelper {
    public static int[] convertFromRecurrenceRule(String recurrence) {
        String[] tmp = recurrence.split(":");
        return new int[]{
                Integer.parseInt(tmp[0]),
                Integer.parseInt(tmp[1]),
                Integer.parseInt(tmp[2])
        };
    }

    public static Calendar getCalendar(String recurrence) {
        int[] tmp = convertFromRecurrenceRule(recurrence);
        return getCalendar(tmp[0], tmp[1], tmp[2]);
    }

    public static Calendar getCalendar(int day, int hour, int minute) {
        //set the calendar to the given time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        //if the day is past, increase the week of year to avoid firing an immediate notification
        if (day < calendar.get(Calendar.DAY_OF_WEEK))
            calendar.add(Calendar.WEEK_OF_YEAR, 1);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.DAY_OF_WEEK, day);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }

    public static PendingIntent getAlarmPendingIntent(Context context, int subjectId, int admissionPercentageId) {
        Intent intent = new Intent(context, NotificationAlarmReceiver.class);
        intent.setAction("de.oerntec.votenote.notifications");
        intent.putExtra("subject_id_notification", subjectId);
        intent.putExtra("admission_percentage_id_notification", admissionPercentageId);

        return PendingIntent.getBroadcast(context, subjectId * 4096 + admissionPercentageId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void removeAlarmForNotification(Context context, int subjectId, int admissionPercentageId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent p = getAlarmPendingIntent(
                context,
                subjectId,
                admissionPercentageId);

        alarmManager.cancel(p);
    }

    public static void setAlarmForNotification(Context context, int subjectId, int admissionPercentageId, int day, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // as of api 19, everything is inexact, so the user will get the notification up to 10
        // minutes later than scheduled
        alarmManager.setInexactRepeating(AlarmManager.RTC,
                NotificationGeneralHelper.getCalendar(day, hour, minute).getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                getAlarmPendingIntent(
                        context,
                        subjectId,
                        admissionPercentageId));
    }
}
