package de.oerntec.votenote.SubjectManagerStuff;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import de.oerntec.votenote.R;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.LessonHolder> {
    //see stackoverflow: http://stackoverflow.com/questions/26517855/using-the-recyclerview-with-a-database
    // PATCH: Because RecyclerView.Adapter in its current form doesn't natively support
    // cursors, we "wrap" a CursorAdapter that will do all teh job
    // for us
    private CursorAdapter mCursorAdapter;
    private Context mContext;

    public SubjectAdapter(Context context, Cursor c) {
        mContext = context;

        //basically the old lesson adapter
        mCursorAdapter = new CursorAdapter(mContext, c, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return inflater.inflate(R.layout.subject_manager_subject_card, parent, false);
            }

            @Override
            public void bindView(final View view, final Context context, Cursor cursor) {
                //find textviews
                TextView title = (TextView) view.findViewById(R.id.subject_manager_subject_card_title),
                        scheduledPercentage = (TextView) view.findViewById(R.id.subject_manager_subject_card_needed_percentage),
                        scheduledPresentationPoints = (TextView) view.findViewById(R.id.subject_manager_subject_card_presentation_points),
                        scheduledLessons = (TextView) view.findViewById(R.id.subject_manager_subject_card_scheduled_lesson_count),
                        scheduledAssignmentsPerLesson = (TextView) view.findViewById(R.id.subject_manager_subject_card_assignments_per_lesson);

                //load strings
                int subjectId = cursor.getInt(0);
                String name = cursor.getString(1);
                String votePercentage = cursor.getString(2);
                String presPoints = cursor.getString(3);
                String lessonCount = cursor.getString(4);
                String assignmentsPerLesson = cursor.getString(5);

                //set tag for later identification avoiding all
                view.setTag(subjectId);

                //set texts
                title.setText(name);
                scheduledPercentage.setText(votePercentage + "%");
                scheduledPresentationPoints.setText(presPoints + " " + context.getString(R.string.subjectmanager_set_prespoints));
                scheduledLessons.setText(lessonCount + " " + context.getString(R.string.subjectmanager_card_lesson_count));
                scheduledAssignmentsPerLesson.setText(assignmentsPerLesson + " " + context.getString(R.string.subjectmanager_assignments_per_lesson));
            }
        };
    }

    public CursorAdapter getCursorAdapter() {
        return mCursorAdapter;
    }

    @Override
    public int getItemCount() {
        return mCursorAdapter.getCount();
    }


    @Override
    public void onBindViewHolder(LessonHolder holder, int position) {
        //move to the correct position
        mCursorAdapter.getCursor().moveToPosition(position);
        // Passing the binding operation to cursor loader
        mCursorAdapter.bindView(holder.itemView, mContext, mCursorAdapter.getCursor());
    }

    @Override
    public LessonHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Passing the inflater job to the cursor-adapter
        View v = mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
        return new LessonHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    class LessonHolder extends RecyclerView.ViewHolder {
        public LessonHolder(View itemView) {
            super(itemView);
        }
    }

}