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
package de.oerntec.votenote.NavigationDrawer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.Database.Subject;
import de.oerntec.votenote.R;

public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.SubjectHolder> {
    private DBSubjects mSubjectDb = DBSubjects.getInstance();
    private List<Subject> mData;
    private SelectionCallback mOnClickCallback;
    private int mCurrentSelection;
    private Context mContext;

    public NavigationDrawerAdapter(Context context, SelectionCallback callbacks) {
        mOnClickCallback = callbacks;
        mContext = context;
        requeryAndReload();
    }

    /**
     * Requery cursor and reload everything
     */
    public void requeryAndReload() {
        mData = mSubjectDb.getAllSubjects();
        notifyItemRangeChanged(0, mData.size());
    }

    @Override
    public SubjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root = inflater.inflate(R.layout.navigation_fragment_list_item, parent, false);
        //create holder for the views
        SubjectHolder holder = new SubjectHolder(root);
        //set view ids
        holder.selectionIndicator = (ImageView) root.findViewById(R.id.navigation_fragment_list_item_selection_indicator);
        holder.title = (TextView) root.findViewById(R.id.navigation_fragment_list_item_text);
        //return the created holder
        return holder;
    }

    @Override
    public void onBindViewHolder(final SubjectHolder holder, int position) {
        Subject currentData = mData.get(position);

        //load strings
        int subjectId = currentData.id;
        String name = currentData.name;
        holder.subjectId = subjectId;

        //set tag for later identification avoiding all confusion
        holder.itemView.setTag(subjectId);

        if (position == mCurrentSelection) {
            holder.selectionIndicator.setImageResource(R.drawable.selection_indicator_activated);
            holder.title.setTextColor(mContext.getResources().getColor(R.color.colorControlHighlight));
        } else {
            holder.selectionIndicator.setImageResource(R.drawable.selection_indicator);
            holder.title.setTextColor(mContext.getResources().getColor(R.color.textColorPrimary));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mOnClickCallback.onItemClick(holder.getAdapterPosition());
            }
        });

        //set texts
        holder.title.setText(name);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * Set the current selection and notify the recycler view
     */
    public void setCurrentSelection(int position) {
        notifyItemChanged(mCurrentSelection);
        notifyItemChanged(position);
        mCurrentSelection = position;
    }
    static class SubjectHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView selectionIndicator;
        int subjectId;

        public SubjectHolder(View itemView) {
            super(itemView);
        }
    }
}