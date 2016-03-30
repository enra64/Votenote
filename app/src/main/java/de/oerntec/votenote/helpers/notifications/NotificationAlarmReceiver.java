package de.oerntec.votenote.helpers.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;

/**
 * This class takes the broadcasts created by the alarm manager
 */
public class NotificationAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //get data from intent
        int admissionPercentageId = intent.getIntExtra("admission_percentage_id_notification", -1);
        int subjectId = intent.getIntExtra("subject_id_notification", -1);

        if (admissionPercentageId == -1) throw new AssertionError("bad admission percentage id");
        if (subjectId == -1) throw new AssertionError("bad subject id");

        // prepare intent which is triggered if the
        // notification is selected
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("show_add_dialog", true);
        notificationIntent.putExtra("admission_percentage_id_notification_action", admissionPercentageId);
        notificationIntent.putExtra("subject_id_notification_action", subjectId);

        //save the id to the intent so we can cancel the notification if the action button is clicked
        int id = (int) System.currentTimeMillis();
        notificationIntent.putExtra("notification_id", id);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(
                context,
                id,
                notificationIntent,
                0);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("not_al", "received notification alarm key_abc, notification id " + id);

        // build notification
        // the addAction re-use the same intent to keep the example short
        NotificationCompat.Builder b = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.notification_title_vote))
                .setContentText(String.format(context.getString(R.string.notification_text), DBSubjects.setupInstance(context).getItem(subjectId).name))
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(R.drawable.ic_button_add, context.getString(R.string.notifcation_action), pIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id, b.build());
    }
}
