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
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.Subject;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBSubjects;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectHolder> {
    public static final int NEW_SUJBECT_CODE = -1;

    private DBSubjects mSubjectDb = DBSubjects.getInstance();
    private Context mContext;
    private List<Subject> mData;

    public SubjectAdapter(Context context) {
        mContext = context;
        requery();
    }

    public int addSubject(Subject subject, int position) {
        //add to database
        int result = mSubjectDb.addItemGetId(subject.name);
        subject.id = result;
        mSubjectDb.changeItem(subject);
        requery();
        //the new lesson should be the first, because the cursor is desc sorted by id
        if (result != -1)
            notifyItemInserted(position == NEW_SUJBECT_CODE ? 0 : position);
        return result;
    }

    /**
     * requery
     * notify of single changed item
     */
    public void notifyOfChangedSubject(int recyclerViewPosition) {
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
        Subject bkp = mSubjectDb.getItem(subjectId);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("subject adapter", "attempting to remove " + subjectId + " " + bkp.name);

        notifyItemRemoved(recyclerViewPosition);
        notifyItemRangeChanged(recyclerViewPosition, getItemCount() - 1);
        mSubjectDb.deleteItem(bkp);
        requery();
        return bkp;
    }

    private void requery() {
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
        holder.counterCount.setText(counterCount + " Admission Counters");
        holder.percentageCounterCount.setText(metaCount + " Percentage Admission Counters");
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