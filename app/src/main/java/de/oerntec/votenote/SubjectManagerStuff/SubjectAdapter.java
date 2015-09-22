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
package de.oerntec.votenote.SubjectManagerStuff;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectHolder> {
    public static final int NEW_SUJBECT_CODE = -1;

    private DBSubjects mSubjectDb = DBSubjects.getInstance();
    private Context mContext;
    private Cursor mCursor;

    public SubjectAdapter(Context context) {
        mContext = context;
        requery();
    }

    public int addSubject(Subject subject, int position) {
        //add to database
        int result = mSubjectDb.addGroup(subject);
        requery();
        //the new lesson should be the first, because the cursor is desc sorted by id
        if (result != -1)
            notifyItemInserted(position == NEW_SUJBECT_CODE ? 0 : position);
        return result;
    }

    /**
     * change in db
     * requery
     * notify of single changed item
     */
    public void changeSubject(Subject oldSubject, Subject newSubject, int recyclerViewPosition) {
        if (!oldSubject.id.equals(newSubject.id))
            throw new IllegalArgumentException("Old and new lesson must have the same lesson ID!");
        mSubjectDb.changeSubject(oldSubject, newSubject);
        requery();
        notifyItemChanged(recyclerViewPosition);
    }

    /**
     * save subject, delete, return backup
     *
     * @param subjectId            id in db
     * @param recyclerViewPosition position in recyclerview
     * @return subject backup
     */
    public Subject removeSubject(int subjectId, int recyclerViewPosition) {
        Subject bkp = mSubjectDb.getSubject(subjectId);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("subject adapter", "attempting to remove " + subjectId + " " + bkp.subjectName);

        notifyItemRemoved(recyclerViewPosition);
        mSubjectDb.deleteSubject(bkp);
        requery();
        return bkp;
    }

    private void requery() {
        if (mCursor != null)
            mCursor.close();
        mCursor = null;
        //should be done asynchronously, but i guess that does not matter for 20 entries...
        mCursor = mSubjectDb.getAllGroupsInfos();
    }

    @Override
    public SubjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root = inflater.inflate(R.layout.subject_manager_subject_card, parent, false);
        //create holder for the views
        SubjectHolder holder = new SubjectHolder(root);
        //set view ids
        holder.title = (TextView) root.findViewById(R.id.subject_manager_subject_card_title);
        holder.scheduledPercentage = (TextView) root.findViewById(R.id.subject_manager_subject_card_needed_percentage);
        holder.scheduledPresentationPoints = (TextView) root.findViewById(R.id.subject_manager_subject_card_presentation_points);
        holder.scheduledLessons = (TextView) root.findViewById(R.id.subject_manager_subject_card_scheduled_lesson_count);
        holder.scheduledAssignmentsPerLesson = (TextView) root.findViewById(R.id.subject_manager_subject_card_assignments_per_lesson);
        //return the created holder
        return holder;
    }

    @Override
    public void onBindViewHolder(SubjectHolder holder, int position) {
        mCursor.moveToPosition(position);

        //set view visible to avoid using invisible views
        holder.itemView.setVisibility(View.VISIBLE);

        //load strings
        int subjectId = mCursor.getInt(0);
        String name = mCursor.getString(1);
        String votePercentage = mCursor.getString(2);
        String presPoints = mCursor.getString(3);
        String lessonCount = mCursor.getString(4);
        String assignmentsPerLesson = mCursor.getString(5);

        //set tag for later identification avoiding all
        holder.itemView.setTag(subjectId);

        //set texts
        holder.title.setText(name);
        holder.scheduledPercentage.setText(votePercentage + "%");
        holder.scheduledPresentationPoints.setText(presPoints + " " + mContext.getString(R.string.subjectmanager_set_prespoints));
        holder.scheduledLessons.setText(lessonCount + " " + mContext.getString(R.string.subjectmanager_card_lesson_count));
        holder.scheduledAssignmentsPerLesson.setText(assignmentsPerLesson + " " + mContext.getString(R.string.subjectmanager_assignments_per_lesson));
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    static class SubjectHolder extends RecyclerView.ViewHolder {
        TextView title, scheduledPercentage, scheduledPresentationPoints, scheduledLessons, scheduledAssignmentsPerLesson;

        public SubjectHolder(View itemView) {
            super(itemView);
        }
    }
}