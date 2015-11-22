/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.AdmissionPercentageFragmentStuff;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBSubjects;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

public class AdmissionPercentageAdapter extends RecyclerView.Adapter<AdmissionPercentageAdapter.Holder> {
    /**
     * Use the infocard
     */
    private static final int VIEW_TYPE_INFO = 0;

    /**
     * use the card for lessons
     */
    private static final int VIEW_TYPE_LESSON = 1;

    /**
     * Admission Percentage Meta Database
     */
    private DBAdmissionPercentageMeta mMetaDb = DBAdmissionPercentageMeta.getInstance();

    /**
     * Admission Percentage Data Database
     */
    private DBAdmissionPercentageData mDataDb = DBAdmissionPercentageData.getInstance();

    /**
     * The meta pojo belonging to the given admission percentage meta id
     */
    private AdmissionPercentageMeta mMetaPojo;

    /**
     * List of current data objects
     */
    private List<AdmissionPercentageData> mData;

    private Context mContext;
    private int mSubjectId, mAdmissionPercentageMetaId;
    private boolean mLatestLessonFirst;

    public AdmissionPercentageAdapter(Context context, int admissionPercentageMetaId) {
        mContext = context;
        //get db
        mMetaDb = DBAdmissionPercentageMeta.getInstance();
        mDataDb = DBAdmissionPercentageData.getInstance();
        //transfer the percentage meta id this adapter is for
        mAdmissionPercentageMetaId = admissionPercentageMetaId;
        //get the pojo object corresponding to this adapter
        mMetaPojo = mMetaDb.getItem(admissionPercentageMetaId);
        //get the subject id
        mSubjectId = mMetaPojo.subjectId;
        //do we want reverse sort?
        mLatestLessonFirst = MainActivity.getPreference("reverse_lesson_sort", false);
        requery();
    }

    /**
     * Does 4 things: add the lesson to the local array;
     * sort the local array
     * add the lesson to the database
     * notify any observers of the change
     */
    public void addLesson(AdmissionPercentageData item) {
        //add to database
        mDataDb.addItem(item);
        requery();
        //notify
        notifyItemInserted(getRecyclerViewPosition(item));
        if (item.lessonId != -1)
            notifyChangedLessonRange(getRecyclerViewPosition(item));
        else
            notifyItemChanged(0);
    }

    /**
     * change in db
     * requery
     * notify of single changed item
     */
    public void changeLesson(AdmissionPercentageData changedLesson) {
        mDataDb.changeItem(changedLesson);
        requery();
        notifyItemChanged(getRecyclerViewPosition(changedLesson));
        //update infoview
        notifyItemChanged(0);
    }

    /**
     * Remove lesson from database
     * @return Savepoint ID of the savepoint created before deleting the object
     */
    public String removeLesson(final AdmissionPercentageData item) {
        String savePointId = "delete_lesson_" + System.currentTimeMillis();
        mDataDb.createSavepoint(savePointId);
        final int listPosition = getRecyclerViewPosition(item);
        mDataDb.deleteItem(item);
        requery();
        notifyItemRemoved(listPosition);
        notifyChangedLessonRange(listPosition);
        return savePointId;
    }

    protected void requery() {
        //mMetaPojo = mMetaDb.getItem(mAdmissionPercentageMetaId);
        mData = mDataDb.getItemsForMetaId(mAdmissionPercentageMetaId, mLatestLessonFirst);
    }

    /**
     * Translates a lesson id to a list position: if not reverse sorted, just add 1 to the lesson id
     * because of the infoview. else, get the item count and subtract the id.
     */
    int getRecyclerViewPosition(AdmissionPercentageData item) {
        int listPosition;
        for (listPosition = 0; listPosition < mData.size(); listPosition++) {
            AdmissionPercentageData apd = mData.get(listPosition);
            if (apd.lessonId == item.lessonId && apd.admissionPercentageMetaId == item.admissionPercentageMetaId)
                break;
        }
        //beware of the infoview, the old lessonIds startes at 1
        listPosition++;
        return mLatestLessonFirst ? getItemCount() - listPosition : listPosition;
    }

    public AdmissionPercentageMeta getCurrentMeta() {
        if (mMetaPojo == null)
            throw new AssertionError("the meta object has not been loaded yet");
        return mMetaPojo;
    }

    /**
     * Notify the recyclerview which items have changed
     *
     * @param changedPosition the position the changed item had in the list
     */
    private void notifyChangedLessonRange(int changedPosition) {
        if (mLatestLessonFirst)
            notifyItemRangeChanged(0, changedPosition);
        else {
            notifyItemRangeChanged(changedPosition, getItemCount() - 1);
            notifyItemChanged(0);
        }
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
            //try to save the holder in the view to enable programatically deleting entries
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
            infoHolder.title.setText(DBSubjects.getInstance().getItem(mSubjectId).name);
            SubjectInfoCalculator.setAverageNeededAssignments(mContext, infoHolder.avgLeft, mSubjectId);
            SubjectInfoCalculator.setCurrentPresentationPointStatus(mContext, infoHolder.prespoints, mSubjectId);
            SubjectInfoCalculator.setVoteAverage(mContext, infoHolder.percentage, mSubjectId);
            //TODO: create list of counters
            //infoHolder.votedAssignments.setText(DBLessons.getInstance().getCompletedAssignmentCount(mSubjectId) + " " + mContext.getString(R.string.voted_assignments_info_card));
        } else if (holder instanceof LessonHolder) {
            LessonHolder lessonHolder = (LessonHolder) holder;
            //adjust for the info view
            position--;

            AdmissionPercentageData data = mData.get(position);

            //load strings
            String lessonIndex = String.valueOf(data.lessonId);
            String myVote = String.valueOf(data.finishedAssignments);
            String maxVote = String.valueOf(data.availableAssignments);
            String voteString = " " + (Integer.valueOf(myVote) < 2 ? mContext.getString(R.string.subject_fragment_singular_vote)
                    : mContext.getString(R.string.main_dialog_lesson_votes));

            //tag for identification without mistakes
            lessonHolder.itemView.setTag(data.lessonId);

            //set visible, because the programmatic swipe sets views invisible to avoid flickering
            lessonHolder.itemView.setVisibility(View.VISIBLE);

            //set texts
            lessonHolder.vote.setText(myVote + " " + mContext.getString(R.string.main_dialog_lesson_von) + " " + maxVote + voteString);
            lessonHolder.lessonId.setText(lessonIndex + mContext.getString(R.string.main_x_th_lesson));
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size() + 1;//+1 for infoview
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