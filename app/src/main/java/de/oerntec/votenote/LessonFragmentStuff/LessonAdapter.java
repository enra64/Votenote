package de.oerntec.votenote.LessonFragmentStuff;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Lesson;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.Holder> {
    private static final int VIEW_TYPE_INFO = 0;
    private static final int VIEW_TYPE_LESSON = 1;

    private DBLessons mLessonDb = DBLessons.getInstance();

    private Context mContext;
    private int mSubjectId;
    private boolean mLatestLessonFirst;
    private Cursor mCursor;

    public LessonAdapter(Context context, int subjectId) {
        mContext = context;
        mSubjectId = subjectId;
        mLatestLessonFirst = MainActivity.getPreference("reverse_lesson_sort", false);
        mCursor = mLessonDb.getAllLessonsForSubject(mSubjectId);
    }

    /**
     * Does 4 things: add the lesson to the local array;
     * sort the local array
     * add the lesson to the database
     * notify any observers of the change
     */
    public void addLesson(Lesson lesson) {
        //add to database
        int lessonId = lesson.lessonId;
        if (lessonId == -1)
            lessonId = mLessonDb.addLessonAtEnd(lesson);
        else
            mLessonDb.insertLesson(lesson);
        requery();
        //notify
        notifyItemInserted(getRecyclerViewPosition(lessonId));
        if (lessonId != -1)
            notifyChangedLessonRange(getRecyclerViewPosition(lessonId));
    }

    /**
     * change in db
     * requery
     * notify of single changed item
     */
    public void changeLesson(Lesson oldLesson, Lesson newLesson) {
        if (oldLesson.lessonId != newLesson.lessonId)
            throw new IllegalArgumentException("Old and new lesson must have the same lesson ID!");
        mLessonDb.changeLesson(oldLesson, newLesson);
        requery();
        notifyItemChanged(getRecyclerViewPosition(oldLesson.lessonId));
    }

    /**
     * remove from db
     * requery
     * notify
     */
    public Lesson removeLesson(int lessonId) {
        Lesson bkp = mLessonDb.getLesson(mSubjectId, lessonId);
        int listPosition = getRecyclerViewPosition(lessonId);
        notifyItemRemoved(listPosition);
        mLessonDb.removeEntry(mSubjectId, lessonId);
        requery();
        notifyChangedLessonRange(listPosition);
        return bkp;
    }

    private void requery() {
        if (mCursor != null)
            mCursor.close();
        mCursor = null;
        //should be done asynchronously, but i guess that does not matter for 20 entries...
        mCursor = mLessonDb.getAllLessonsForSubject(mSubjectId);
    }

    /**
     * Translates a lesson id to a list position: if not reverse sorted, just add 1 to the lesson id
     * because of the infoview. else, get the item count and subtract the id.
     */
    private int getRecyclerViewPosition(int lessonId) {
        return mLatestLessonFirst ? getItemCount() - lessonId : lessonId;
    }

    /**
     * Notify the recyclerview which items have changed
     *
     * @param changedPosition the position the changed item had in the list
     */
    private void notifyChangedLessonRange(int changedPosition) {
        if (mLatestLessonFirst)
            notifyItemRangeChanged(0, changedPosition);
        else
            notifyItemRangeChanged(changedPosition, getItemCount() - 1);
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_INFO) {
            //inflate layout
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root = inflater.inflate(R.layout.subject_fragment_card_subject_info, parent, false);
            //create lessonholder for the views
            InfoHolder infoHolder = new InfoHolder(root);
            //save view references
            infoHolder.title = (TextView) root.findViewById(R.id.subject_fragment_subject_card_title);
            infoHolder.prespoints = (TextView) root.findViewById(R.id.subject_fragment_subject_card_prespoints);
            infoHolder.avgLeft = (TextView) root.findViewById(R.id.subject_fragment_subject_card_average_assignments_left);
            infoHolder.votedAssignments = (TextView) root.findViewById(R.id.subject_fragment_subject_card_completed_assignments);
            infoHolder.percentage = (TextView) root.findViewById(R.id.subject_fragment_subject_card_title_percentage);
            return infoHolder;
        } else {
            //inflate layout
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View root = inflater.inflate(R.layout.subject_fragment_card_lesson, parent, false);
            //create lesson holder
            LessonHolder lessonHolder = new LessonHolder(root);
            //save references
            lessonHolder.vote = (TextView) root.findViewById(R.id.subject_fragment_card_upper);
            lessonHolder.lessonId = (TextView) root.findViewById(R.id.subject_fragment_card_lower);
            return lessonHolder;
        }
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        if (holder instanceof InfoHolder) {
            InfoHolder infoHolder = (InfoHolder) holder;
            infoHolder.title.setText(DBSubjects.getInstance().getGroupName(mSubjectId));
            SubjectInfoCalculator.setAverageNeededAssignments(mContext, infoHolder.avgLeft, mSubjectId);
            SubjectInfoCalculator.setCurrentPresentationPointStatus(mContext, infoHolder.prespoints, mSubjectId);
            SubjectInfoCalculator.setVoteAverage(mContext, infoHolder.percentage, mSubjectId);
            infoHolder.votedAssignments.setText(DBLessons.getInstance().getCompletedAssignmentCount(mSubjectId) + " " + mContext.getString(R.string.voted_assignments_info_card));
        } else if (holder instanceof LessonHolder) {
            LessonHolder lessonHolder = (LessonHolder) holder;
            //adjust for the info view
            position--;

            mCursor.moveToPosition(position);

            Lesson currentLesson = createLessonFromCursor();

            //load strings
            String lessonIndex = String.valueOf(currentLesson.lessonId);
            String myVote = String.valueOf(currentLesson.myVotes);
            String maxVote = String.valueOf(currentLesson.maxVotes);
            String voteString = " " + (Integer.valueOf(myVote) < 2 ? mContext.getString(R.string.subject_fragment_singular_vote)
                    : mContext.getString(R.string.main_dialog_lesson_votes));

            //tag for identification without mistakes
            lessonHolder.itemView.setTag(currentLesson.lessonId);

            //set texts
            lessonHolder.vote.setText(myVote + " " + mContext.getString(R.string.main_dialog_lesson_von) + " " + maxVote + voteString);
            lessonHolder.lessonId.setText(lessonIndex + mContext.getString(R.string.main_x_th_lesson));
        }
    }

    /**
     * move mCursor to the correct position before!
     */
    private Lesson createLessonFromCursor() {
        return new Lesson(mCursor.getInt(0),
                mCursor.getInt(1),
                mCursor.getInt(2),
                mCursor.getInt(3), mSubjectId);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount() + 1;//+1 for infoview
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_INFO : VIEW_TYPE_LESSON;
    }

    static class Holder extends RecyclerView.ViewHolder {

        public Holder(View itemView) {
            super(itemView);
        }
    }

    static class InfoHolder extends Holder {
        TextView title, prespoints, avgLeft, votedAssignments, percentage;

        public InfoHolder(View itemView) {
            super(itemView);
        }
    }

    static class LessonHolder extends Holder {
        TextView vote, lessonId;

        public LessonHolder(View itemView) {
            super(itemView);
        }
    }
}