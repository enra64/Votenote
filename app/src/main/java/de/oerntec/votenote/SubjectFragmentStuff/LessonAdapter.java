package de.oerntec.votenote.SubjectFragmentStuff;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import de.oerntec.votenote.CardListHelpers.SwipeableRecyclerViewTouchListener;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.R;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonHolder> {
    private static final int VIEW_TYPE_INFO = 0;
    private static final int VIEW_TYPE_LESSON = 1;

    //see stackoverflow: http://stackoverflow.com/questions/26517855/using-the-recyclerview-with-a-database
    // PATCH: Because RecyclerView.Adapter in its current form doesn't natively support
    // cursors, we "wrap" a CursorAdapter that will do all teh job
    // for us
    private CursorAdapter mCursorAdapter;
    private Context mContext;
    private int mSubjectId;

    public LessonAdapter(Context context, Cursor c, int subjectId, SwipeableRecyclerViewTouchListener SwipeListener) {
        mContext = context;
        mSubjectId = subjectId;

        //basically the old lesson adapter
        mCursorAdapter = new CursorAdapter(mContext, c, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return inflater.inflate(R.layout.subject_fragment_card_lesson, parent, false);
            }

            @Override
            public void bindView(final View view, final Context context, Cursor cursor) {
                //find textviews
                TextView upper = (TextView) view.findViewById(R.id.subject_fragment_card_upper);
                TextView lower = (TextView) view.findViewById(R.id.subject_fragment_card_lower);

                //load strings
                String lessonIndex = cursor.getString(0);
                String myVote = cursor.getString(1);
                String maxVote = cursor.getString(2);
                String voteString = " " + (Integer.valueOf(myVote) < 2 ? context.getString(R.string.subject_fragment_singular_vote)
                        : context.getString(R.string.main_dialog_lesson_votes));

                //set tag for later identification avoiding all
                view.setTag(Integer.valueOf(lessonIndex));

                //set texts
                upper.setText(myVote + " " + context.getString(R.string.main_dialog_lesson_von) + " " + maxVote + voteString);
                lower.setText(lessonIndex + context.getString(R.string.main_x_th_lesson));
            }
        };
    }

    public CursorAdapter getCursorAdapter() {
        return mCursorAdapter;
    }

    @Override
    public int getItemCount() {
        return mCursorAdapter.getCount() + 1;
    }


    @Override
    public void onBindViewHolder(LessonHolder holder, int position) {
        if (position == 0) {
            bindInfoView(holder.itemView);
            return;
        }
        //reduce position, because the infoview is the first position, and everything else is
        //zero-indexed
        position--;
        //move to the correct position
        mCursorAdapter.getCursor().moveToPosition(position);
        // Passing the binding operation to cursor loader
        mCursorAdapter.bindView(holder.itemView, mContext, mCursorAdapter.getCursor());
    }

    private void bindInfoView(View root) {
        TextView title = (TextView) root.findViewById(R.id.subject_fragment_subject_card_title),
                percentage = (TextView) root.findViewById(R.id.subject_fragment_subject_card_vote_percentage),
                prespoints = (TextView) root.findViewById(R.id.subject_fragment_subject_card_prespoints),
                avgLeft = (TextView) root.findViewById(R.id.subject_fragment_subject_card_average_assignments_left),
                percentageTitle = (TextView) root.findViewById(R.id.subject_fragment_subject_card_title_percentage);

        //calc and set values
        title.setText(DBSubjects.getInstance().getGroupName(mSubjectId));
        SubjectInfoCalculator.setAverageNeededAssignments(mContext, avgLeft, mSubjectId);
        SubjectInfoCalculator.setCurrentPresentationPointStatus(mContext, prespoints, mSubjectId);
        SubjectInfoCalculator.setVoteAverage(mContext, percentageTitle, mSubjectId);
    }

    private View createInfoView(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return inflater.inflate(R.layout.subject_fragment_card_subject_info, parent, false);
    }

    @Override
    public LessonHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Passing the inflater job to the cursor-adapter
        View v;
        if (viewType == VIEW_TYPE_LESSON)
            v = mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
        else
            v = createInfoView(parent);
        return new LessonHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_INFO : VIEW_TYPE_LESSON;
    }

    class LessonHolder extends RecyclerView.ViewHolder {
        public LessonHolder(View itemView) {
            super(itemView);
        }
    }

}