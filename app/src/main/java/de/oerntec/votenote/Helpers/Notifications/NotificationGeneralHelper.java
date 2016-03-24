package de.oerntec.votenote.Helpers.Notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.util.Calendar;

/**
 * Bits and pieces to make handling the notifications and their settings easier
 */
public class NotificationGeneralHelper {
    /**
     * Get the recurrence rule out of a recurrence string.
     * If recurrence is null, it returns {0,0,0}. For each part of the recurrence string that is
     * empty, a zero is also returned
     *
     * @param recurrence input string to be converted
     * @return day, hour, minute int array corresponding to the input recurrence string
     */
    public static int[] convertFromRecurrenceRule(@Nullable String recurrence) {
        if (recurrence == null || recurrence.isEmpty())
            recurrence = "::";//baad
        String[] tmp = recurrence.split(":");
        int day = !tmp[0].isEmpty() ? Integer.parseInt(tmp[0]) : 0;
        int hour = !tmp[1].isEmpty() ? Integer.parseInt(tmp[1]) : 0;
        int minute = !tmp[2].isEmpty() ? Integer.parseInt(tmp[2]) : 0;
        return new int[]{day, hour, minute};
    }

    /**
     * Returns a calendar set to the day of week, hour and minute given in the recurrence string
     */
    public static Calendar getCalendar(String recurrence) {
        if (recurrence == null || recurrence.isEmpty())
            return null;
        int[] tmp = convertFromRecurrenceRule(recurrence);
        return getCalendar(tmp[0], tmp[1], tmp[2]);
    }

    /**
     * Returns a calendar set to the day of week, hour and minute given
     */
    public static Calendar getCalendar(int day, int hour, int minute) {
        //set the calendar to the given time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        long nowTime = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.DAY_OF_WEEK, day);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        long alarmTime = calendar.getTimeInMillis();

        //if the day is past, increase the week of year to avoid firing an immediate notification
        if (alarmTime < nowTime)
            calendar.add(Calendar.WEEK_OF_YEAR, 1);


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

    public static void setAlarmForNotification(Context context, int subjectId, int admissionPercentageId, String recurrenceString) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int[] data = convertFromRecurrenceRule(recurrenceString);

        // as of api 19, everything is inexact, so the user will get the notification up to 10
        // minutes later than scheduled
        alarmManager.setInexactRepeating(AlarmManager.RTC,
                NotificationGeneralHelper.getCalendar(data[0], data[1], data[2]).getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7,
                getAlarmPendingIntent(
                        context,
                        subjectId,
                        admissionPercentageId));
    }
}
