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
package de.oerntec.votenote.subject_management;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.Subject;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionCounters;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectHolder> {
    private DBSubjects mSubjectDb;
    private Context mContext;
    private List<Subject> mData;

    public SubjectAdapter(Activity activity) {
        mContext = activity;
        mSubjectDb = DBSubjects.getInstance();
        if (mSubjectDb == null) {
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.w("sub man adapter", "had to recreate dbsubjects instance??");
            mSubjectDb = DBSubjects.setupInstance(activity.getApplicationContext());
        }
        reQuery();
    }

    /**
     * reQuery
     * notify of single changed item
     */
    public void notifyOfChangedSubject(int recyclerViewPosition) {
        reQuery();
        notifyItemChanged(recyclerViewPosition);
    }

    public void notifySubjectAdded() {
        List<Subject> oldList = new ArrayList<>(mData);
        reQuery();
        List<Subject> newList = new ArrayList<>(mData);
        //iterate over the subjects to find the first one that does not match, it must be the new one
        int newIndex;
        for (newIndex = 0; newIndex < oldList.size(); newIndex++)
            if (!oldList.get(newIndex).equals(newList.get(newIndex)))
                break;

        if (newIndex > 0 || newIndex < mData.size()) {
            notifyItemInserted(newIndex);
        } else
            throw new AssertionError("algo fail");
    }

    /**
     * removes subject (with on delete cascade for basically everything else!)
     */
    public void removeSubject(int subjectId, int recyclerViewPosition) {
        notifyItemRemoved(recyclerViewPosition);
        notifyItemRangeChanged(recyclerViewPosition, getItemCount() - 1);
        mSubjectDb.deleteItem(mSubjectDb.getItem(subjectId));
        reQuery();
    }

    private void reQuery() {
        if (mSubjectDb == null)
            mSubjectDb = DBSubjects.getInstance();
        mData = mSubjectDb.getAllSubjects();
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
        holder.counterCount = (TextView) root.findViewById(R.id.subject_manager_subject_card_counter_count);
        holder.percentageCounterCount = (TextView) root.findViewById(R.id.subject_manager_subject_card_percentage_counter_count);
        return holder;
    }

    @Override
    public void onBindViewHolder(SubjectHolder holder, int position) {
        Subject item = mData.get(position);

        //set view visible to avoid using invisible views
        holder.itemView.setVisibility(View.VISIBLE);

        //load strings
        int subjectId = item.id;
        String name = item.name;
        int metaCount = DBAdmissionPercentageMeta.getInstance().getItemsForSubject(item.id).size();
        int counterCount = DBAdmissionCounters.getInstance().getItemsForSubject(item.id).size();

        //set tag for later identification avoiding all
        holder.itemView.setTag(subjectId);

        //set texts
        holder.title.setText(name);
        holder.counterCount.setText(mContext.getString(R.string.subject_adapter_card_counter_count, counterCount));
        holder.percentageCounterCount.setText(mContext.getString(R.string.subject_adapter_percentage_count, metaCount));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class SubjectHolder extends RecyclerView.ViewHolder {
        TextView title, percentageCounterCount, counterCount;

        public SubjectHolder(View itemView) {
            super(itemView);
        }
    }
}