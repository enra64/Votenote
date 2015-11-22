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

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.PojoDatabase;
import de.oerntec.votenote.MainActivity;

public class SubjectCreatorAdapter<T extends NameAndIdPojo> extends RecyclerView.Adapter<SubjectCreatorAdapter.NameViewHolder> {
    private PojoDatabase<T> mDb;
    private List<T> mData;
    private int mSubjectId;

    public SubjectCreatorAdapter(PojoDatabase<T> db, int subjectId, T newItemButton) {
        mSubjectId = subjectId;
        mDb = db;
        requery();
    }

    public T getItemAtPosition(int position) {
        return mData.get(position);
    }

    /**
     * Add the item to the database and the recyclerview
     * @param item
     * @return True if the item has been successfully inserted
     */
    public boolean addItem(T item) {
        //add to database
        int check = mDb.addItemGetId(item);
        requery();
        //the counters are ordered by their id ascending, so the latest counter should be the lowest
        if (check >= 0)
            notifyItemInserted(mData.size() - 1);
        return check >= 0;
    }

    /**
     * requery
     * notify of single changed item
     */
    public void notifyOfChangeAtId(int id) {
        requery();
        notifyItemChanged(convertIdToPosition(id));
    }

    /**
     * save admission counter, delete, return backup
     *
     * @param admissionCounterId            id in db
     * @return admission counter backup
     */
    public T removeItem(int admissionCounterId) {
        T bkp = mDb.getItem(admissionCounterId);
        if (bkp == null)
            throw new AssertionError("could not find item i am supposed to delete");

        int recyclerViewPosition = convertIdToPosition(admissionCounterId);

        if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
            Log.i("subject adapter", "attempting to remove counter: " + bkp.getDisplayName());

        notifyItemRemoved(recyclerViewPosition);
        notifyItemRangeChanged(recyclerViewPosition, getItemCount() - 1);
        mDb.deleteItem(admissionCounterId);
        requery();
        return bkp;
    }

    /**
     * goes through the datalist, returns the position our id can be found at
     */
    private int convertIdToPosition(int id) {
        for (int pos = 0; pos < mData.size(); pos++)
            if (mData.get(pos).getId() == id)
                return pos;
        throw new AssertionError("could not find that id in mData - someone fucked up");
    }

    private void requery() {
        mData = null;
        mData = mDb.getItemsForSubject(mSubjectId);
        //mData.add(0, mNewItemButton);
    }

    @Override
    public NameViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        //create holder for the views
        NameViewHolder holder = new NameViewHolder(root);
        //set view ids
        holder.name = (TextView) root.findViewById(android.R.id.text1);
        //return the created holder
        return holder;
    }

    @Override
    public void onBindViewHolder(NameViewHolder holder, int position) {
        T data = mData.get(position);

        //set view visible to avoid using invisible views
        holder.itemView.setVisibility(View.VISIBLE);

        //set tag for later identification avoiding all confusion
        holder.itemView.setTag(data.getId());

        //set texts
        holder.name.setText(data.getDisplayName());
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class NameViewHolder extends RecyclerView.ViewHolder {
        TextView name;

        public NameViewHolder(View itemView) {
            super(itemView);
        }
    }
}