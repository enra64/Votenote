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
package de.oerntec.votenote.chart;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.database.tablehelpers.DBPercentageTracker;
import de.oerntec.votenote.database.tablehelpers.DBSubjects;

public class ChartSubjectAdapter extends RecyclerView.Adapter<ChartSubjectAdapter.SubjectHolder> {
    //private DBGroups mSubjectDb = DBGroups.getInstance();

    private final AdapterListener mAdapterListener;
    private final int[] mColorArray;
    private final Context mContext;
    private List<PercentageTrackerPojo> mData;

    public ChartSubjectAdapter(Context context, int[] colorArray) {
        mAdapterListener = (AdapterListener) context;
        mColorArray = colorArray;
        mContext = context;
        requery();
    }

    private void requery() {
        mData = DBPercentageTracker.getInstance().getAllItems();
    }

    @Override
    public SubjectHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //inflate layout
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root = inflater.inflate(R.layout.diagramactivity_listitem, parent, false);
        //create holder for the views
        SubjectHolder holder = new SubjectHolder(root);
        //set view ids
        holder.title = (TextView) root.findViewById(R.id.diagramactivity_listview_listitem_text_name);
        holder.indicator = (ImageView) root.findViewById(R.id.diagramactivity_listview_listitem_selection_indicator);
        holder.checkBox = (CheckBox) root.findViewById(R.id.diagramactivity_listview_listitem_checkbox);
        //return the created holder
        return holder;
    }

    @Override
    public void onBindViewHolder(final SubjectHolder holder, final int position) {
        final PercentageTrackerPojo data = mData.get(holder.getAdapterPosition());

        //load data
        String trackerName = data.name;
        final int metaId = data.id;

        //set tag for later identification avoiding all
        holder.itemView.setTag(data.id);

        final boolean enoughLessons = DBLessons.getInstance().getItemsForMetaId(metaId, false/*sorting does not matter here*/).size() > 1;

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enoughLessons) {
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                    mAdapterListener.onClick(metaId, holder.checkBox.isChecked(), mColorArray[holder.getAdapterPosition()]);
                } else
                    Toast.makeText(mContext, mContext.getString(R.string.diagram_not_enough_entries), Toast.LENGTH_SHORT).show();
            }
        });

        holder.checkBox.setEnabled(enoughLessons);

        holder.checkBox.setChecked(false);

        holder.checkBox.setHighlightColor(mColorArray[position]);

        //set texts
        holder.title.setText(String.format("%s: %s", DBSubjects.getInstance().getItem(data.subjectId).name, trackerName));
        holder.indicator.clearColorFilter();
        holder.indicator.setColorFilter(mColorArray[position]);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    public interface AdapterListener {
        void onClick(int subjectId, boolean enabled, int color);
    }

    static class SubjectHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView indicator;
        CheckBox checkBox;

        public SubjectHolder(View itemView) {
            super(itemView);
        }
    }
}