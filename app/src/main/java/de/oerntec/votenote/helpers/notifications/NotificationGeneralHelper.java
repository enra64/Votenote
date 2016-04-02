package de.oerntec.votenote.helpers.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionPercentageMeta;

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
            recurrence = "0:0:0";//baad
        String[] tmp = recurrence.split(":");
        int day = Integer.parseInt(tmp[0]);
        int hour = Integer.parseInt(tmp[1]);
        int minute = Integer.parseInt(tmp[2]);
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
    private static Calendar getCalendar(int day, int hour, int minute) {
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

    private static PendingIntent getAlarmPendingIntent(Context context, int subjectId, int admissionPercentageId) {
        Intent intent = new Intent(context, NotificationAlarmReceiver.class);
        intent.setAction("de.oerntec.votenote.notifications");
        intent.putExtra("subject_id_notification", subjectId);
        intent.putExtra("admission_percentage_id_notification", admissionPercentageId);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("notif", "intent hash: " + intent.filterHashCode() + ", id: " + admissionPercentageId);

        return PendingIntent.getBroadcast(//893110327
                context,
                admissionPercentageId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void removeAlarmForNotification(Context context, int subjectId, int admissionPercentageId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent p = getAlarmPendingIntent(
                context,
                subjectId,
                admissionPercentageId);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("notifications", "removed notification");

        p.cancel();
        alarmManager.cancel(p);
    }

    /**
     * Delete multiple alarms
     *
     * @param context   context object, needed for alarm access
     * @param subjectId if < 0, all alarms found in the db will be deleted. otherwise, only alarms
     *                  with a matching subject id will get deleted
     */
    public static void removeAllAlarmsForSubject(Context context, int subjectId) {
        List<PercentageTrackerPojo> notificationList =
                DBAdmissionPercentageMeta.setupInstance(context).getItemsWithNotifications();

        for (PercentageTrackerPojo apm : notificationList) {
            if (apm.subjectId == subjectId || subjectId < 0)
                NotificationGeneralHelper.removeAlarmForNotification(context, subjectId, apm.id);
        }
    }

    public static void removeAllAlarms(Context context) {
        removeAllAlarmsForSubject(context, -1);
    }

    public static void setAlarmForNotification(Context context, int subjectId, int admissionPercentageId, int day, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // as of api 19, everything is inexact, so the user will get the notification up to 1 shitload
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
        int[] data = convertFromRecurrenceRule(recurrenceString);
        setAlarmForNotification(context, subjectId, admissionPercentageId, data[0], data[1], data[2]);
    }
}
