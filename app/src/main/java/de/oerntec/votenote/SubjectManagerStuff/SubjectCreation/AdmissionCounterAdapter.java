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
package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Database.DBAdmissionCounters;
import de.oerntec.votenote.MainActivity;

public class AdmissionCounterAdapter extends RecyclerView.Adapter<AdmissionCounterAdapter.AdmissionCounterHolder> {
    private Context mContext;
    private DBAdmissionCounters mDb;
    private List<AdmissionCounter> mData;
    private int mSubjectId;


    public AdmissionCounterAdapter(Context context, int subjectId) {
        mContext = context;
        mSubjectId = subjectId;
        mDb = DBAdmissionCounters.getInstance();
        requery();
    }

    public AdmissionCounter getItemAtPosition(int position){
        return mData.get(position);
    }

    /**
     * Add the item to the database and the recyclerview
     * @param item
     * @return True if the item has been successfully inserted
     */
    public boolean addItem(AdmissionCounter item) {
        //add to database
        boolean success = mDb.addItem(item);
        requery();
        //the counters are ordered by their id ascending, so the latest counter should be the lowest
        if (success)
            notifyItemInserted(mData.size() - 1);
        return success;
    }

    /**
     * requery
     * notify of single changed item
     */
    public void notifyOfChangedPosition(int recyclerViewPosition) {
        requery();
        notifyItemChanged(recyclerViewPosition);
    }

    /**
     * save admission counter, delete, return backup
     *
     * @param admissionCounterId            id in db
     * @param recyclerViewPosition position in recyclerview
     * @return admission counter backup
     */
    public AdmissionCounter removeCounter(int admissionCounterId, int recyclerViewPosition) {
        AdmissionCounter bkp = mDb.getItem(admissionCounterId);
        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("subject adapter", "attempting to remove counter: " + bkp.counterName);

        notifyItemRemoved(recyclerViewPosition);
        notifyItemRangeChanged(recyclerViewPosition, getItemCount() - 1);
        mDb.deleteItem(admissionCounterId);
        requery();
        return bkp;
    }

    private void requery() {
        mData = null;
        mData = mDb.getItemsForSubject(mSubjectId);
    }

    @Override
    public AdmissionCounterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        //create holder for the views
        AdmissionCounterHolder holder = new AdmissionCounterHolder(root);
        //set view ids
        holder.name = (TextView) root.findViewById(android.R.id.text1);
        //return the created holder
        return holder;
    }

    @Override
    public void onBindViewHolder(AdmissionCounterHolder holder, int position) {
        AdmissionCounter data = mData.get(position);

        //set view visible to avoid using invisible views
        holder.itemView.setVisibility(View.VISIBLE);

        //set tag for later identification avoiding all confusion
        holder.itemView.setTag(data.id);

        //set texts
        holder.name.setText(data.counterName + " - " + data.targetValue);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class AdmissionCounterHolder extends RecyclerView.ViewHolder {
        TextView name;

        public AdmissionCounterHolder(View itemView) {
            super(itemView);
        }
    }
}