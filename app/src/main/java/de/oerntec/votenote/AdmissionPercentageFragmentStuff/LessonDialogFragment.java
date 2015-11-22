package de.oerntec.votenote.AdmissionPercentageFragmentStuff;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

public class LessonDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String ARG_AP_META_ID = "apmetaid";
    private static final String ARG_LESSON_ID = "lessonid";

    private static final int ADD_LESSON_CODE = -2;

    /**
     * Admission percentage counter meta id for this dialog
     */
    private int mMetaId;

    /**
     * Lesson id for this dialog
     */
    private int mLessonId;

    /**
     * Whether the lesson is old or new
     */
    private boolean mIsNewLesson, mIsOldLesson;

    /**
     * Whether the lessons are displayed in reverse  or not
     */
    private boolean mReverseLessonSort;

    /**
     * Whether to try and fix invalid number picker values or let the user do it
     */
    private boolean mEnableDataAutoFix;

    /**
     * database instance to use
     */
    private DBAdmissionPercentageData mDataDb;

    /**
     * text view used to display information about the current picker status
     */
    private TextView mInfoView;

    /**
     * picker used to choose how many assignments have been done
     */
    private NumberPicker mFinishedAssignmentsPicker;

    /**
     * picker used to choose how many assignments are available to be done
     */
    private NumberPicker mAvailableAssignmentsPicker;

    /**
     * default constructor
     */
    public LessonDialogFragment() {
    }

    /**
     * get a new instance of the fragment
     *
     * @param apMetaId the ap meta id this lesson is for
     * @param lessonID lesson id to identify the lesson
     * @return new instance
     */
    public static LessonDialogFragment newInstance(final int apMetaId, final int lessonID) {
        LessonDialogFragment fragment = new LessonDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_AP_META_ID, apMetaId);
        args.putInt(ARG_LESSON_ID, lessonID);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMetaId = getArguments().getInt(ARG_AP_META_ID);
            mLessonId = getArguments().getInt(ARG_LESSON_ID);
            mIsNewLesson = mLessonId == ADD_LESSON_CODE;
            mIsOldLesson = !mIsNewLesson;

            mReverseLessonSort = MainActivity.getPreference("reverse_lesson_sort", false);
            mEnableDataAutoFix = MainActivity.getPreference("move_max_assignments_picker", true);
        } else
            throw new AssertionError("Why are there no arguments here?");
    }

    private View createView(LayoutInflater inflater) {
        //inflate rootView
        final View rootView = inflater.inflate(R.layout.subject_fragment_dialog_newentry, null);

        //find all necessary views
        mInfoView = (TextView) rootView.findViewById(R.id.infoTextView);
        mFinishedAssignmentsPicker = (NumberPicker) rootView.findViewById(R.id.pickerMyVote);
        mAvailableAssignmentsPicker = (NumberPicker) rootView.findViewById(R.id.pickerMaxVote);

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity())
                .setView(createView(getActivity().getLayoutInflater()))
                .setTitle(getTitle())
                .setPositiveButton("OK", this)
                .setNegativeButton("Cancel", this)
                .setNeutralButton("Delete", this);
        return b.create();
    }

    private String getTitle() {
        return null;//todo return useful title here
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                break;
            case AlertDialog.BUTTON_NEGATIVE:
                break;
            case AlertDialog.BUTTON_NEUTRAL:
                break;
        }
    }
}
