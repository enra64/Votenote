package de.oerntec.votenote.helpers.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;

/**
 * This is supposed to recreate all notifications on reboot
 */
public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        List<PercentageTrackerPojo> notificationList =
                DBPercentageTracker.setupInstance(context).getItemsWithNotifications();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.i("uuh", "this is weird");
        }

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
