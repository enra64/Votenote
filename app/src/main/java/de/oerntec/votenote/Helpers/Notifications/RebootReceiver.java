package de.oerntec.votenote.Helpers.Notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;

/**
 * Created by arne on 3/19/16.
 */
public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        List<AdmissionPercentageMetaPojo> notificationList =
                DBAdmissionPercentageMeta.setupInstance(context).getItemsWithNotifications();

        for (AdmissionPercentageMetaPojo apm : notificationList) {
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
