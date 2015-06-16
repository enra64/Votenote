package de.oerntec.votenote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

public class MainDialogHelper {
    private static final int ADD_LESSON_CODE = -2;

    /**
     * Show an add lesson dialog
     *
     * @param mActivity Activity to use for references
     * @param groupID   id of the group to add a lesson to
     */
    public static void showAddLessonDialog(final MainActivity mActivity, final int groupID) {
        showLessonDialog(mActivity, groupID, ADD_LESSON_CODE);
    }

    /**
     * Show a dialog to change a lesson entry
     * @param mActivity the activity for reference usage
     * @param groupID the id of the subject
     * @param translatedLessonPosition the id of the lesson, translated (+1) from listview
     */
    public static void showChangeLessonDialog(final MainActivity mActivity, final int groupID, final int translatedLessonPosition) {
        showLessonDialog(mActivity, groupID, translatedLessonPosition);
    }

    /**
     * Shows a dialog to edit or add a lesson
     */
    private static void showLessonDialog(final MainActivity mActivity, final int groupID, final int lessonID) {
        //db access
        final DBEntries entryDB = DBEntries.getInstance();

        int maxVoteValue;
        int myVoteValue;

        //set values according to usage (add or change)
        if (lessonID == ADD_LESSON_CODE) {
            maxVoteValue = entryDB.getPreviousMaximumVote(groupID);
            myVoteValue = entryDB.getPreviousMyVote(groupID);
        } else {
            DBEntries.Lesson oldValues = entryDB.getLesson(groupID, lessonID);
            myVoteValue = oldValues != null ? oldValues.myVotes : 0;
            maxVoteValue = oldValues != null ? oldValues.maxVotes : 0;
        }

        //inflate rootView
        final View rootView = mActivity.getLayoutInflater().inflate(R.layout.mainfragment_dialog_newentry, null);

        //find all necessary views
        final TextView infoView = (TextView) rootView.findViewById(R.id.infoTextView);
        final NumberPicker myVote = (NumberPicker) rootView.findViewById(R.id.pickerMyVote);
        final NumberPicker maxVote = (NumberPicker) rootView.findViewById(R.id.pickerMaxVote);

        //config for the maximum vote picker
        maxVote.setMinValue(0);
        maxVote.setMaxValue(15);
        maxVote.setValue(maxVoteValue);

        //config for the actual vote picker
        myVote.setMinValue(0);
        myVote.setMaxValue(15);
        myVote.setValue(myVoteValue);

        //set the current values of the pickers as explanation text
        infoView.setText(myVote.getValue() + " von " + maxVote.getValue() + " Votierungen");
        infoView.setTextColor(Color.argb(255, 153, 204, 0));//green

        //build alertdialog
        final AlertDialog dialog = new AlertDialog.Builder(mActivity)
                .setTitle(lessonID == ADD_LESSON_CODE ? "Neue Übungsnotiz" : "Übung " + lessonID + " ändern?")
                .setView(rootView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (myVote.getValue() <= maxVote.getValue()) {
                            //decide whether to add an entry or change an existing one
                            if (lessonID == ADD_LESSON_CODE)
                                entryDB.addLesson(groupID, maxVote.getValue(), myVote.getValue());
                            else
                                entryDB.changeLesson(groupID, lessonID, maxVote.getValue(), myVote.getValue());
                            //reload current fragment
                            mActivity.notifyCurrentFragment();
                        } else
                            Toast.makeText(mActivity, "Mehr votiert als möglich!", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
                }).create();

        //listener for the vote picker
        NumberPicker.OnValueChangeListener votePickerListener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                //load new values
                int myVoteValue = myVote.getValue();
                int maxVoteValue = maxVote.getValue();
                infoView.setText(myVoteValue + " von " + maxVoteValue + " Votierungen");
                boolean isValid = myVoteValue <= maxVoteValue;
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isValid);
                infoView.setTextColor(isValid ? Color.argb(255, 153, 204, 0) /*green*/ : Color.argb(255, 204, 0, 0));//red
            }
        };

        //add change listener to update dialog explanation if pickers changed
        myVote.setOnValueChangedListener(votePickerListener);
        maxVote.setOnValueChangedListener(votePickerListener);

        dialog.show();
    }

    /**
     * shows the dialog to change the presentation points
     * @param dataBaseId ID of the group in the database
     * @param activity reference for various stuff
     */
    public static void showPresentationPointDialog(final int dataBaseId, final MainActivity activity) {
        //db access
        final DBGroups groupsDB = DBGroups.getInstance();

        final View inputView = activity.getLayoutInflater().inflate(R.layout.mainfragment_dialog_prespoints, null);
        final NumberPicker presPointPicker = (NumberPicker) inputView.findViewById(R.id.mainfragment_dialog_prespoints_picker);

        presPointPicker.setMinValue(0);
        presPointPicker.setMaxValue(groupsDB.getWantedPresPoints(dataBaseId));
        presPointPicker.setValue(groupsDB.getPresPoints(dataBaseId));

		/*
         * PRESPOINT ALERTDIALOG
		 */
        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setView(inputView)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //write new prespoint count to db
                        groupsDB.setPresPoints(dataBaseId, presPointPicker.getValue());
                        //reload fragment
                        activity.notifyCurrentFragment();
                    }
                }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
        b.setTitle("Erreichte Präsentationspunkte");
        b.create().show();
    }
}
