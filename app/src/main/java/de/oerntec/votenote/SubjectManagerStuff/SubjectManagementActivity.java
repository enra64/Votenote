package de.oerntec.votenote.SubjectManagerStuff;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.CardListHelpers.OnItemClickListener;
import de.oerntec.votenote.CardListHelpers.RecyclerItemClickListener;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.ImportExport.XmlImporter;
import de.oerntec.votenote.R;

@SuppressLint("InflateParams")
public class SubjectManagementActivity extends AppCompatActivity {
    /**
     * Lesson should be added, not changed
     */
    private static final int ADD_SUBJECT_CODE = -1;

    /**
     * default db access
     */
    private static DBSubjects mSubjectDb;

    /**
     * the subject adapter used to fill the list
     */
    private static SubjectAdapter mSubjectAdapter;

    /**
     * saves the subject on delete to be able to add it again should the user demand it
     */
    private Subject subjectToBeDeleted;
    private int positionOfSubjectToBeDeleted;

    private RecyclerView mSubjectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subject_manager_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.subject_manager_toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        //tutorial?
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("firstGroup", false)) {
            Builder b = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            b.setTitle("Tutorial");
            b.setView(this.getLayoutInflater().inflate(R.layout.tutorial_subjects, null));
            b.setPositiveButton(getString(R.string.dialog_button_ok), null);
            b.create().show();
        }

        //get db
        mSubjectDb = DBSubjects.getInstance();

        //get listview
        mSubjectList = (RecyclerView) findViewById(R.id.subject_manager_recycler_view);

        //config the recyclerview
        mSubjectList.setHasFixedSize(true);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //info view has no tag
                Object viewTag = viewHolder.itemView.getTag();
                if (viewTag != null) {
                    int subjectId = (Integer) viewTag;
                    if (subjectId != 0)
                        showUndoSnackBar(subjectId, mSubjectList.getChildAdapterPosition(viewHolder.itemView));
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(mSubjectList);

        //give it a layoutmanager (whatever that is)
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        mSubjectList.setLayoutManager(manager);

        mSubjectAdapter = new SubjectAdapter(this);

        mSubjectList.setAdapter(mSubjectAdapter);
        mSubjectList.addOnItemTouchListener(new RecyclerItemClickListener(this, mSubjectList, new OnItemClickListener() {
            public void onItemClick(View view, int position) {
                showSubjectDialog((Integer) view.getTag(), position);
            }

            public void onItemLongClick(final View view, int position) {
            }
        }));

        //add listener to fab
        FloatingActionButton addFab = (FloatingActionButton) findViewById(R.id.subject_manager_add_fab);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubjectDialog(ADD_SUBJECT_CODE, -1);
            }
        });
    }

    private void showUndoSnackBar(final int subjectId, int recyclerViewPosition) {
        positionOfSubjectToBeDeleted = recyclerViewPosition;
        subjectToBeDeleted = mSubjectAdapter.removeSubject(subjectId, recyclerViewPosition);
        Snackbar
                .make(findViewById(R.id.subject_manager_coordinator_layout), "Gelöscht!", Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUndo();
                    }
                }).show();
    }

    //dont do anything, as we only delete the lesson when the undo bar gets hidden
    private void onUndo() {
        if (subjectToBeDeleted != null) {
            mSubjectAdapter.addSubject(subjectToBeDeleted, positionOfSubjectToBeDeleted);
        }
        subjectToBeDeleted = null;
        positionOfSubjectToBeDeleted = -1;
    }

    private void showSubjectDialog(final int databaseId, final int recyclerViewPosition) {
        String nameHint = getString(R.string.subject_add_hint);
        int presentationPointsHint = 0;
        int minimumVotePercentageHint = 50;
        int scheduledAssignmentsPerLesson = 5;
        int scheduledNumberOfLessons = 10;

        Subject oldSubject = null;
        //if we only change the entry, get the previously set values.
        if (databaseId != ADD_SUBJECT_CODE) {
            oldSubject = mSubjectDb.getSubject(databaseId);
            nameHint = oldSubject.subjectName;
            presentationPointsHint = Integer.parseInt(oldSubject.subjectWantedPresentationPoints);
            minimumVotePercentageHint = Integer.parseInt(oldSubject.subjectMinimumVotePercentage);
            scheduledAssignmentsPerLesson = Integer.parseInt(oldSubject.subjectScheduledAssignmentsPerLesson);
            scheduledNumberOfLessons = Integer.parseInt(oldSubject.subjectScheduledLessonCount);
        }

        //inflate view with seekbar and name
        final View input = this.getLayoutInflater().inflate(R.layout.subject_manager_dialog_groupsettings, null);

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
        nameInput.setText(nameHint);

        //avoid keyboard popup
        voteInfo.requestFocus();

        //minpreshelp
        presInfo.setText("" + presentationPointsHint);

        //minpres seek
        wantedPresentationPointsSeekbar.setProgress(presentationPointsHint);
        wantedPresentationPointsSeekbar.setMax(5);
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
        estimatedAssignmentsSeek.setMax(20);
        estimatedAssignmentsSeek.setProgress(scheduledAssignmentsPerLesson);
        estimatedAssignmentsSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedAssignmentsHelp));

        //uebung instances help
        estimatedUebungCountHelp.setText(scheduledNumberOfLessons + "");

        //ubeung instances seek
        estimatedUebungCountSeek.setMax(20);
        estimatedUebungCountSeek.setProgress(scheduledNumberOfLessons);
        estimatedUebungCountSeek.setOnSeekBarChangeListener(new SeekerListener(estimatedUebungCountHelp));

        String title;
        if (databaseId == ADD_SUBJECT_CODE) {
            if (mSubjectDb.getNumberOfSubjects() == 0)
                title = getString(R.string.subject_manage_add_title_first_subject);
            else
                title = getString(R.string.subject_manage_add_title_new_subject);
        } else
            title = getString(R.string.subject_manage_add_title_change_subject) + " " + nameHint;

        //build alertdialog
        final Subject finalOldSubject = oldSubject;
        Builder b = new Builder(this)
                .setView(input)
                .setTitle(title)
                .setPositiveButton(getString(R.string.dialog_button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = nameInput.getText().toString();
                        Subject newSubject = new Subject(String.valueOf(databaseId),
                                String.valueOf("".equals(name) ? finalOldSubject.subjectName : name),
                                String.valueOf(minVoteSeek.getProgress()),
                                "0",
                                String.valueOf(estimatedUebungCountSeek.getProgress()),
                                String.valueOf(estimatedAssignmentsSeek.getProgress()),
                                String.valueOf(wantedPresentationPointsSeekbar.getProgress()));

                        if (databaseId == ADD_SUBJECT_CODE) {
                            if (mSubjectAdapter.addSubject(newSubject, SubjectAdapter.NEW_SUJBECT_CODE) == -1)
                                Toast.makeText(getApplicationContext(), getString(R.string.subject_manage_subject_exists_already), Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                        } else {
                            if ("".equals(newSubject.subjectName))
                                newSubject.subjectName = "empty";
                            mSubjectAdapter.changeSubject(finalOldSubject, newSubject, recyclerViewPosition);
                        }
                    }
                }).setNegativeButton(getString(R.string.dialog_button_abort), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        final AlertDialog dialog = b.create();

        //check for bad characters
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = String.valueOf(s);
                if (value.contains("<") || value.contains(">") || value.contains("/'")) {
                    Toast.makeText(getApplication(), getString(R.string.subject_manage_bad_subject_name), Toast.LENGTH_SHORT).show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
            case R.id.action_read_from_storage:
                XmlImporter.importDialog(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_subject_management, menu);
        return true;
    }

    /**
     * fuck it recreate dat adapter
     */
    public void onImport() {
        mSubjectList.swapAdapter(new SubjectAdapter(this), false);
    }

    class SeekerListener implements OnSeekBarChangeListener {
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