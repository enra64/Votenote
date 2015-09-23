package de.oerntec.votenote.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.NavigationDrawer.NavigationDrawerFragment;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectManagementActivity;

public class SubjectDialogs {
    public static void showSubjectDialog(final Activity activity, final NavigationDrawerFragment navDrawer, final int databaseId) {
        final DBSubjects mSubjectDb = DBSubjects.getInstance();

        //save open rv position
        //undoBarRecyclerViewPosition = recyclerViewPosition;

        //default values
        String nameHint = activity.getString(R.string.subject_add_hint);
        int presentationPointsHint = 0;
        int minimumVotePercentageHint = 50;
        int scheduledAssignmentsPerLesson = 5;
        int scheduledNumberOfLessons = 10;

        //try to get old subject data; returns null if no subject is found
        final Subject knownSubjectData = mSubjectDb.getSubject(databaseId);

        //create a boolean containing whether we create a new subject to ease understanding
        final boolean isNewSubject = databaseId == SubjectManagementActivity.ADD_SUBJECT_CODE;
        final boolean isOldSubject = !isNewSubject;

        //if we only change the entry, get the previously set values.
        if (isOldSubject) {
            //extract subject data
            nameHint = knownSubjectData.subjectName;
            presentationPointsHint = Integer.parseInt(knownSubjectData.subjectWantedPresentationPoints);
            minimumVotePercentageHint = Integer.parseInt(knownSubjectData.subjectMinimumVotePercentage);
            scheduledAssignmentsPerLesson = Integer.parseInt(knownSubjectData.subjectScheduledAssignmentsPerLesson);
            scheduledNumberOfLessons = Integer.parseInt(knownSubjectData.subjectScheduledLessonCount);
        }

        //inflate view with seekbar and name
        final View input = activity.getLayoutInflater().inflate(R.layout.subject_manager_dialog_groupsettings, null);

        final EditText nameInput = (EditText) input.findViewById(R.id.subject_manager_dialog_groupsettings_edit_name);

        final TextView voteInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_min_votierungs);
        final SeekBar minVoteSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_min_votierungs);

        final TextView presInfo = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_prespoints);
        final SeekBar wantedPresentationPointsSeekbar = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_prespoints);

        final TextView estimatedAssignmentsHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_assignments_per_uebung);
        final SeekBar estimatedAssignmentsSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_assignments_per_uebung);

        final TextView estimatedUebungCountHelp = (TextView) input.findViewById(R.id.subject_manager_dialog_groupsettings_text_estimated_uebung_count);
        final SeekBar estimatedUebungCountSeek = (SeekBar) input.findViewById(R.id.subject_manager_dialog_groupsettings_seek_estimated_uebung_count);

        //offer hint to user
        nameInput.setHint(nameHint);
        //only set the old name as text if it is not "subject name", so the user can correct his value
        if (isOldSubject)
            nameInput.setText(nameHint);

        //minpreshelp
        presInfo.setText("" + presentationPointsHint);

        //minpres seek
        wantedPresentationPointsSeekbar.setProgress(presentationPointsHint);
        wantedPresentationPointsSeekbar.setMax(30);
        wantedPresentationPointsSeekbar.setOnSeekBarChangeListener(new SeekerListener(presInfo));

        //minvotehelp
        //initialize seekbar and seekbar info text
        voteInfo.setText(minimumVotePercentageHint + "%");

        //minvoteseek
        minVoteSeek.setMax(100);
        minVoteSeek.setProgress(minimumVotePercentageHint);
        minVoteSeek.setOnSeekBarChangeListener(new SeekerListener(voteInfo, "%"));

        //assignments per uebung help
        estimatedAssignmentsHelp.setText(scheduledAssignmentsPerLesson + "");

        //assignments per uebung seek
        estimatedAssignmentsSeek.setMax(50);
        estimatedAssignmentsSeek.setProgress(scheduledAssignmentsPerLesson);
        estimatedAssignmentsSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedAssignmentsHelp));

        //uebung instances help
        estimatedUebungCountHelp.setText(scheduledNumberOfLessons + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(50);
        estimatedUebungCountSeek.setProgress(scheduledNumberOfLessons);
        estimatedUebungCountSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedUebungCountHelp));

        String title;
        if (isNewSubject) {
            if (mSubjectDb.getNumberOfSubjects() == 0)
                title = activity.getString(R.string.subject_manage_add_title_first_subject);
            else
                title = activity.getString(R.string.subject_manage_add_title_new_subject);
        } else//if old subject
            title = activity.getString(R.string.subject_manage_add_title_change_subject) + " " + nameHint;

        //build alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(input)
                .setTitle(title)
                .setPositiveButton(activity.getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //create a new subject containing all known values
                        Subject newSubject = new Subject(String.valueOf(databaseId),
                                String.valueOf(nameInput.getText().toString()),
                                String.valueOf(minVoteSeek.getProgress()),
                                "0",
                                String.valueOf(estimatedUebungCountSeek.getProgress()),
                                String.valueOf(estimatedAssignmentsSeek.getProgress()),
                                String.valueOf(wantedPresentationPointsSeekbar.getProgress()));

                        if ("".equals(newSubject.subjectName))
                            newSubject.subjectName = "empty";
                        mSubjectDb.changeSubject(knownSubjectData, newSubject);
                        navDrawer.reloadAdapter();
                    }
                }).setNegativeButton(activity.getString(R.string.dialog_button_abort), null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        //hide the bloody keyboard
                        View view = activity.getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }
                });

        if (isOldSubject) {
            builder.setNeutralButton(R.string.delete_button_text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mSubjectDb.deleteSubject(knownSubjectData);
                    navDrawer.reloadAdapter();
                }
            });
        }

        final AlertDialog dialog = builder.create();

        //check for bad characters
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = String.valueOf(s);
                //deny saving a string breaking the xml backups
                if (value.contains("<") || value.contains(">") || value.contains("/'")) {
                    Toast.makeText(activity, activity.getString(R.string.subject_manage_bad_subject_name), Toast.LENGTH_SHORT).show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                //deny saving an empty name
                else if (value.equals("")) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                //no bad names detected; allow saving
                else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        dialog.show();

        //change delete button text color to red
        if (isOldSubject)
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(activity.getResources().getColor(R.color.warning_red));

        //disable ok button if new subject until a name is entered
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isOldSubject);
    }

    static class SeekerListener implements SeekBar.OnSeekBarChangeListener {
        TextView mAffected;
        String mEndingAddition;

        public SeekerListener(TextView affected, String endingAddition) {
            mAffected = affected;
            mEndingAddition = endingAddition;
        }

        public SeekerListener(TextView affected) {
            this(affected, "");
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
            mAffected.setText(progress + mEndingAddition);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
