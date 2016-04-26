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
package de.oerntec.votenote.percentage_tracker_fragment;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.AdmissionCounter;
import de.oerntec.votenote.database.pojo.Lesson;
import de.oerntec.votenote.database.pojo.percentagetracker.EstimationModeDependentResults;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerCalculationResult;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerCalculator;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionCounters;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.helpers.General;
import de.oerntec.votenote.preferences.Preferences;

public class PercentageTrackerAdapter extends RecyclerView.Adapter<PercentageTrackerAdapter.Holder> {
    /**
     * Use the infocard
     */
    static final int VIEW_TYPE_INFO = 0;

    /**
     * use the card for lessons
     */
    static final int VIEW_TYPE_LESSON = 1;

    /**
     * cards used for tutorial
     */
    static final int VIEW_TYPE_TUTORIAL = 2;
    /**
     * Convenience constant to ease reading of various previous "+ 1"'s
     */
    private static final int NUMBER_OF_INFO_VIEWS = 1;
    /**
     * The meta pojo belonging to the given admission percentage meta id
     */
    private final PercentageTrackerPojo mMetaPojo;
    private final Context mContext;
    private final int mAdmissionPercentageMetaId;
    private final boolean mLatestLessonFirst;
    /**
     * Admission Percentage Meta Database
     */
    @SuppressWarnings("FieldCanBeLocal")
    private DBPercentageTracker mMetaDb = DBPercentageTracker.getInstance();
    /**
     * Admission Percentage Data Database
     */
    private DBLessons mDataDb = DBLessons.getInstance();
    /**
     * Holds the position of the item that was last deleted, so we know what to do for an undo
     */
    private Integer mLastDeletedPosition;
    /**
     * List of current data objects
     */
    private List<Lesson> mData;
    /**
     * Should we display the tutorial?
     */
    private boolean mDisplayTutorial = false;
    /**
     * If we show a tutorial, this is 1, so we can add it to things like getCount etc
     */
    private int mNumberOfTutorialViews = 0;

    public PercentageTrackerAdapter(
            Context context,
            int admissionPercentageMetaId) {
        mContext = context;
        //get db
        mMetaDb = DBPercentageTracker.getInstance();
        mDataDb = DBLessons.getInstance();
        //transfer the percentage meta id this adapter is for
        mAdmissionPercentageMetaId = admissionPercentageMetaId;
        //get the pojo object corresponding to this adapter
        mMetaPojo = mMetaDb.getItem(admissionPercentageMetaId);
        //do we want reverse sort?
        mLatestLessonFirst = Preferences.getPreference(mContext, "reverse_lesson_sort", false);
        reQuery();
    }

    /**
     * Add item to the database, notify recyclerview of change
     *
     * @param item item to add to the database
     */
    public void addLesson(Lesson item) {
        //add to database
        item.lessonId = mDataDb.addItemGetId(item);
        reQuery();

        int recyclerViewPosition = getRecyclerViewPosition(item);

        //notify of changes
        notifyItemInserted(recyclerViewPosition);
        notifyChangedLessonRange(recyclerViewPosition);
    }

    @SuppressWarnings("unused")
    public void refreshItem(int swipedLessonId, int percentageMetaId) {
        notifyItemChanged(getRecyclerViewPosition(new Lesson(percentageMetaId, swipedLessonId, -1, -1)));
    }

    public void reinstateLesson() {
        //reload database
        reQuery();
        //if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
        //    Log.i("ap adapter", "reinstate at " + mLastDeletedPosition);
        //notify of changes
        notifyItemInserted(mLastDeletedPosition);
        notifyChangedLessonRange(mLastDeletedPosition + NUMBER_OF_INFO_VIEWS);
    }

    /**
     * change in db
     * requery
     * notify of single changed item
     */
    public void changeLesson(Lesson changedLesson) {
        mDataDb.changeItem(changedLesson);
        reQuery();
        notifyItemChanged(getRecyclerViewPosition(changedLesson));
        //update infoview
        notifyItemChanged(0);
    }

    /**
     * Remove lesson from database
     *
     * @return Savepoint ID of the savepoint created before deleting the object
     */
    public String removeLesson(final Lesson item) {
        String savePointId = "delete_lesson_" + System.currentTimeMillis();
        mDataDb.createSavepoint(savePointId);
        final int recyclerViewPosition = getRecyclerViewPosition(item);
        //if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
        //    Log.i("ap adapter", "delete pos " + recyclerViewPosition);
        mLastDeletedPosition = recyclerViewPosition;
        mDataDb.deleteItem(item);
        reQuery();
        notifyItemRemoved(recyclerViewPosition);
        notifyChangedLessonRange(recyclerViewPosition);
        return savePointId;
    }

    private void reQuery() {
        mData = mDataDb.getItemsForMetaId(mAdmissionPercentageMetaId, mLatestLessonFirst);
        checkShowTutorial();
    }

    /**
     * Enable or disable the tutorial. notifyDataSetChanged if display state changed
     */
    private void checkShowTutorial() {
        boolean tutorialDisplayedOnLastCheck = mDisplayTutorial;
        mDisplayTutorial = mData.isEmpty();
        mNumberOfTutorialViews = mDisplayTutorial ? 1 : 0;

        // only notify of data set refresh if the state changed
        if (tutorialDisplayedOnLastCheck != mDisplayTutorial)
            notifyDataSetChanged();
    }

    int getRecyclerViewPosition(Lesson item) {
        int listPosition;
        for (listPosition = 0; listPosition < mData.size(); listPosition++) {
            Lesson apd = mData.get(listPosition);
            if (apd.lessonId == item.lessonId && apd.admissionPercentageMetaId == item.admissionPercentageMetaId)
                break;
        }
        return listPosition + NUMBER_OF_INFO_VIEWS + mNumberOfTutorialViews;
    }

    /**
     * Get the admission percentage meta pojo containing the info corresponding to the admission
     * percentage counter this adapter shows lessons for
     *
     * @return meta pojo
     */
    public PercentageTrackerPojo getCurrentMeta() {
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
            notifyItemChanged(0);
            notifyItemRangeChanged(changedPosition, getItemCount() - changedPosition);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root;
        switch (viewType) {
            case VIEW_TYPE_INFO:
                root = inflater.inflate(R.layout.percentage_tracker_info_card, parent, false);
                //create lessonholder for the views
                InfoHolder infoHolder = new InfoHolder(root, VIEW_TYPE_INFO);
                //save view references
                infoHolder.title = (TextView) root.findViewById(R.id.subject_fragment_subject_card_title);
                infoHolder.prespoints = (TextView) root.findViewById(R.id.subject_fragment_subject_card_prespoints);
                infoHolder.avgLeft = (TextView) root.findViewById(R.id.subject_fragment_subject_card_average_assignments_left);
                infoHolder.votedAssignments = (TextView) root.findViewById(R.id.subject_fragment_subject_card_completed_assignments);
                infoHolder.percentage = (TextView) root.findViewById(R.id.subject_fragment_subject_card_title_percentage);
                return infoHolder;
            case VIEW_TYPE_LESSON:
                root = inflater.inflate(R.layout.percentage_tracker_lesson_card, parent, false);
                //create lesson holder
                LessonHolder lessonHolder = new LessonHolder(root, VIEW_TYPE_LESSON);
                //save references
                lessonHolder.vote = (TextView) root.findViewById(R.id.subject_fragment_card_upper);
                lessonHolder.lessonId = (TextView) root.findViewById(R.id.subject_fragment_card_lower);
                return lessonHolder;
            case VIEW_TYPE_TUTORIAL:
                root = inflater.inflate(R.layout.tutorial_lessons, parent, false);
                return new TutorialHolder(root, VIEW_TYPE_TUTORIAL);
            default:
                throw new AssertionError("unknown viewType");
        }
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        if (holder instanceof InfoHolder) {
            InfoHolder infoHolder = (InfoHolder) holder;
            mMetaPojo.loadData(DBLessons.getInstance(), mLatestLessonFirst);
            PercentageTrackerCalculationResult result = PercentageTrackerCalculator.calculateAll(mMetaPojo);
            infoHolder.title.setText(mMetaPojo.name);
            setAverageNeededAssignments(infoHolder.avgLeft, mMetaPojo, result);
            setVoteAverage(infoHolder.percentage, mMetaPojo);
            List<AdmissionCounter> counterList = DBAdmissionCounters.getInstance().getItemsForSubject(mMetaPojo.subjectId);
            //only show this
            if (counterList.size() != 1)
                infoHolder.prespoints.setVisibility(View.GONE);
            else if (counterList.size() == 1)
                setCurrentPresentationPointStatus(infoHolder.prespoints, counterList.get(0).id);
            infoHolder.votedAssignments.setText(mContext.getString(R.string.voted_assignments_w_placeholder_info_card, (int) result.numberOfFinishedAssignments));
        } else if (holder instanceof LessonHolder) {
            LessonHolder lessonHolder = (LessonHolder) holder;
            //adjust for the info view
            position--;

            //if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            //    Log.i("ap adapter", "bind view at " + position);
            Lesson data = mData.get(position);

            //load strings
            String lessonIndex = String.valueOf(data.lessonId);

            //tag for identification without mistakes
            lessonHolder.itemView.setTag(data.lessonId);

            //set visible, because the programmatic swipe sets views invisible to avoid flickering
            lessonHolder.itemView.setVisibility(View.VISIBLE);

            //set texts
            lessonHolder.vote.setText(mContext.getString(R.string.number_of_done_assignments_lesson_card, data.finishedAssignments, data.availableAssignments));
            //lessonIndex + mContext.getString(R.string.main_x_th_lesson)
            lessonHolder.lessonId.setText(mContext.getString(R.string.x_th_lesson_lesson_card, lessonIndex));
        } else //noinspection StatementWithEmptyBody
            if (holder instanceof TutorialHolder) {

            } else
                throw new AssertionError("the view type is not lesson, but position is not 0 either");
    }

    private void setCurrentPresentationPointStatus(TextView presentationPointsView, int counterId) {
        AdmissionCounter counter = DBAdmissionCounters.getInstance().getItem(counterId);
        int currentPoints = counter.currentValue;
        int targetPoints = counter.targetValue;

        //set text informing of current presentation point level, handling plural by the way.
        presentationPointsView.setText(String.format(General.getCurrentLocale(mContext),
                "%s: %d %s %d",
                counter.name,
                currentPoints,
                mContext.getString(R.string.main_dialog_lesson_von),
                targetPoints));
    }

    private void setVoteAverage(TextView averageVoteView, PercentageTrackerPojo meta) {
        float average = meta.getAverageFinished();

        //no votes have been given
        if (meta.hasNoLessons())
            averageVoteView.setText(mContext.getString(R.string.infoview_vote_average_no_data));

        //write percentage and color coding to summaryview
        if (Float.isNaN(average) || average < 0)
            averageVoteView.setText(mContext.getString(R.string.infoview_vote_average_no_data));
        else
            averageVoteView.setText(String.format(General.getCurrentLocale(mContext), "%.1f%%", average));

        General.triStateClueColors(averageVoteView, mContext, meta, 0, 0);
    }

    private void setAverageNeededAssignments(TextView averageNeededVotesView,
                                             PercentageTrackerPojo meta,
                                             PercentageTrackerCalculationResult result) {
        EstimationModeDependentResults dependentResults = result.getEstimationDependentResults(meta.estimationMode);

        if (result.numberOfFutureLessons == 0)
            averageNeededVotesView.setText(mContext.getString(R.string.subject_fragment_reached_scheduled_lesson_count));
        else if (result.numberOfFutureLessons < 0)
            averageNeededVotesView.setText(mContext.getString(R.string.subject_fragment_overshot_lesson_count));
        else
            averageNeededVotesView.setText(String.format("%s %s %s", mContext.getString(R.string.subject_fragment_on_average),
                    String.format(
                            General.getCurrentLocale(mContext),
                            "%.2f",
                            dependentResults.numberOfAssignmentsNeededPerLesson),
                    mContext.getString(R.string.lesson_fragment_info_card_assignments_per_lesson_description)));

        if (dependentResults.numberOfEstimatedOverallAvailableAssignments < 0)
            averageNeededVotesView.setText(mContext.getString(R.string.subject_fragment_error_detected));

        if (meta.bonusTargetPercentageEnabled) {
            if (dependentResults.bonusReachable &&
                    dependentResults.numberOfAssignmentsNeededPerLesson < dependentResults.numberOfAssignmentsEstimatedPerLesson) {
                averageNeededVotesView.setTextColor(ContextCompat.getColor(mContext, android.R.color.primary_text_light));
            } else if (!dependentResults.bonusReachable &&
                    dependentResults.numberOfAssignmentsNeededPerLesson <= dependentResults.numberOfAssignmentsEstimatedPerLesson) {
                averageNeededVotesView.setTextColor(ContextCompat.getColor(mContext, R.color.warning_orange));
            } else if (!dependentResults.bonusReachable &&
                    dependentResults.numberOfAssignmentsNeededPerLesson > dependentResults.numberOfAssignmentsEstimatedPerLesson) {
                averageNeededVotesView.setTextColor(ContextCompat.getColor(mContext, R.color.warning_red));
            }
        } else {
            if (dependentResults.numberOfAssignmentsNeededPerLesson < dependentResults.numberOfAssignmentsEstimatedPerLesson) {
                averageNeededVotesView.setTextColor(ContextCompat.getColor(mContext, android.R.color.primary_text_light));
            } else if (dependentResults.numberOfAssignmentsNeededPerLesson > dependentResults.numberOfAssignmentsEstimatedPerLesson) {
                averageNeededVotesView.setTextColor(ContextCompat.getColor(mContext, R.color.warning_red));
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mData == null)
            return 0;
        else
            return mData.size() + NUMBER_OF_INFO_VIEWS + mNumberOfTutorialViews;
    }

    @Override
    public int getItemViewType(int position) {
        int viewType;
        if (mDisplayTutorial) {
            switch (position) {
                case 0:
                    viewType = VIEW_TYPE_INFO;
                    break;
                case 1:
                    viewType = VIEW_TYPE_TUTORIAL;
                    break;
                default:
                    viewType = VIEW_TYPE_LESSON;
                    break;
            }
        } else {
            switch (position) {
                case 0:
                    viewType = VIEW_TYPE_INFO;
                    break;
                default:
                    viewType = VIEW_TYPE_LESSON;
                    break;
            }
        }
        return viewType;
    }

    static class Holder extends RecyclerView.ViewHolder {
        public Holder(View itemView) {
            super(itemView);
        }
    }

    static class InfoHolder extends Holder {
        TextView title, prespoints, avgLeft, votedAssignments, percentage;

        public InfoHolder(View itemView, int viewType) {
            super(itemView);
            itemView.setTag(viewType);
        }
    }

    static class LessonHolder extends Holder {
        TextView vote, lessonId;

        public LessonHolder(View itemView, int viewType) {
            super(itemView);
            itemView.setTag(viewType);
        }
    }

    static class TutorialHolder extends Holder {
        public TutorialHolder(View itemView, int viewType) {
            super(itemView);
            itemView.setTag(viewType);
        }
    }
}