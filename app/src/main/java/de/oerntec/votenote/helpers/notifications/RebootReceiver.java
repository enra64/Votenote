package de.oerntec.votenote.helpers.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionPercentageMeta;

/**
 * This is supposed to recreate all notifications on reboot
 */
public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        List<PercentageTrackerPojo> notificationList =
                DBAdmissionPercentageMeta.setupInstance(context).getItemsWithNotifications();

        for (PercentageTrackerPojo apm : notificationList) {
            int[] tmp = NotificationGeneralHelper.convertFromRecurrenceRule(apm.notificationRecurrenceString);
            NotificationGeneralHelper.setAlarmForNotification(
                    context,
                    apm.subjectId,
                    apm.id,
                    tmp[0],
                    tmp[1],
                    tmp[2]);
        }
    }
}
