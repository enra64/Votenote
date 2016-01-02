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
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
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
     * Holds the position of the item that was last deleted, so we know what to do for an undo
     */
    private Integer mLastDeletedPosition;

    /**
     * List of current data objects
     */
    private List<AdmissionPercentageData> mData;

    private Context mContext;
    private int mAdmissionPercentageMetaId;
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
        //do we want reverse sort?
        mLatestLessonFirst = MainActivity.getPreference("reverse_lesson_sort", false);
        requery();
    }

    /**
     * Add item to the database, notify recyclerview of change
     * @param item item to add to the database
     */
    public void addLesson(AdmissionPercentageData item) {
        //add to database
        item.lessonId = mDataDb.addItemGetId(item);
        requery();

        int recyclerViewPosition = getRecyclerViewPosition(item);

        //notify of changes
        notifyItemInserted(recyclerViewPosition);
        notifyChangedLessonRange(recyclerViewPosition);
    }

    public void reinstateLesson() {
        //reload database
        requery();
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("ap adapter", "reinstate at " + mLastDeletedPosition);
        //notify of changes
        notifyItemInserted(mLastDeletedPosition);
        notifyChangedLessonRange(mLastDeletedPosition + 1);
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
        final int recyclerViewPosition = getRecyclerViewPosition(item);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("ap adapter", "delete pos " + recyclerViewPosition);
        mLastDeletedPosition = recyclerViewPosition;
        mDataDb.deleteItem(item);
        requery();
        notifyItemRemoved(recyclerViewPosition);
        notifyChangedLessonRange(recyclerViewPosition);
        return savePointId;
    }

    protected void requery() {
        mData = mDataDb.getItemsForMetaId(mAdmissionPercentageMetaId, mLatestLessonFirst);
    }

    int getRecyclerViewPosition(AdmissionPercentageData item) {
        int listPosition;
        for (listPosition = 0; listPosition < mData.size(); listPosition++) {
            AdmissionPercentageData apd = mData.get(listPosition);
            if (apd.lessonId == item.lessonId && apd.admissionPercentageMetaId == item.admissionPercentageMetaId)
                break;
        }
        return listPosition + 1;
    }

    public AdmissionPercentageMeta getCurrentMeta() {
        if (mMetaPojo == null)
            throw new AssertionError("the meta object has not been loaded yet");
        return mMetaPojo;
    }

    /**
     * Notify the recyclerview which items have changed
     * @param changedPosition the position the changed item had in the list
     */
    private void notifyChangedLessonRange(int changedPosition) {
        if (mLatestLessonFirst)
            notifyItemRangeChanged(0, changedPosition);
        else {
            notifyItemChanged(0);
            notifyItemRangeChanged(changedPosition, getItemCount() - changedPosition);
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
        } else if (viewType == VIEW_TYPE_LESSON) {
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
        } else
            throw new AssertionError("impossible view type requested!");
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        if (position == 0) {
            InfoHolder infoHolder = (InfoHolder) holder;
            mMetaPojo.loadData(DBAdmissionPercentageData.getInstance(), mLatestLessonFirst);
            mMetaPojo.calculateData();
            infoHolder.title.setText(mMetaPojo.name);
            setAverageNeededAssignments(infoHolder.avgLeft, mMetaPojo);
            setVoteAverage(infoHolder.percentage, mMetaPojo);
            List<AdmissionCounter> counterList = DBAdmissionCounters.getInstance().getItemsForSubject(mMetaPojo.subjectId);
            //only show this
            if(counterList.size() != 1)
                infoHolder.prespoints.setVisibility(View.GONE);
            else if (counterList.size() == 1)
                setCurrentPresentationPointStatus(infoHolder.prespoints, counterList.get(0).id);
            infoHolder.votedAssignments.setText(mContext.getString(R.string.voted_assignments_w_placeholder_info_card, mMetaPojo.getFinishedAssignments()));
        } else if (holder instanceof LessonHolder) {
            LessonHolder lessonHolder = (LessonHolder) holder;
            //adjust for the info view
            position--;

            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.i("ap adapter", "bind view at " + position);
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
            lessonHolder.vote.setText(mContext.getString(R.string.number_of_done_assignments_lesson_card, data.finishedAssignments, data.availableAssignments));
            //lessonIndex + mContext.getString(R.string.main_x_th_lesson)
            lessonHolder.lessonId.setText(mContext.getString(R.string.x_th_lesson_lesson_card, lessonIndex));
        } else
            throw new AssertionError("the view type is not lesson, but position is not 0 either");
    }

    private void setCurrentPresentationPointStatus(TextView presentationPointsView, int counterId) {
        AdmissionCounter counter = DBAdmissionCounters.getInstance().getItem(counterId);
        int currentPoints = counter.currentValue;
        int targetPoints = counter.targetValue;

        //set text informing of current presentation point level, handling plural by the way.
        presentationPointsView.setText(counter.counterName + ": " + currentPoints + " " + mContext.getString(R.string.main_dialog_lesson_von) + " " + targetPoints);
    }

    private void setVoteAverage(TextView averageVoteView, AdmissionPercentageMeta meta) {
        float average = meta.getAverageFinished();

        //no votes have been given
        if (!meta.getHasLessons())
            averageVoteView.setText(mContext.getString(R.string.infoview_vote_average_no_data));

        //get minvote for section
        int minVote = meta.targetPercentage;

        //write percentage and color coding to summaryview
        if (Float.isNaN(average))
            averageVoteView.setText(mContext.getString(R.string.infoview_vote_average_no_data));
        else
            averageVoteView.setText(String.format("%.1f", average) + "%");

        //color text in
        averageVoteView.setTextColor(average >= minVote ? Color.argb(255, 153, 204, 0) : Color.argb(255, 204, 0, 0));//red
    }

    private void setAverageNeededAssignments(TextView averageNeededVotesView, AdmissionPercentageMeta meta) {
        if (meta.getNumberOfLessonsLeft() == 0)
            averageNeededVotesView.setText(mContext.getString(R.string.subject_fragment_reached_scheduled_lesson_count));
        else if (meta.getNumberOfLessonsLeft() < 0)
            averageNeededVotesView.setText(mContext.getString(R.string.subject_fragment_overshot_lesson_count));
        else
            averageNeededVotesView.setText(String.format("%s %s %s", mContext.getString(R.string.subject_fragment_on_average), String.format("%.2f", meta.getNeededAssignmentsPerUebung()), mContext.getString(R.string.lesson_fragment_info_card_assignments_per_lesson_description)));

        if (meta.getEstimatedNumberOfAssignments() < 0)
            averageNeededVotesView.setText(mContext.getString(R.string.subject_fragment_error_detected));

        //set color
        if (meta.getNeededAssignmentsPerUebung() > meta.estimatedAssignmentsPerLesson)
            averageNeededVotesView.setTextColor(Color.argb(255, 204, 0, 0));//red
        else
            averageNeededVotesView.setTextColor(mContext.getColor(R.color.abc_primary_text_material_light));
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