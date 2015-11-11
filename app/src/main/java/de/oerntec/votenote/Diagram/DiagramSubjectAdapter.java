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
package de.oerntec.votenote.Diagram;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.oerntec.votenote.Database.DBGroups;
import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.R;

public class DiagramSubjectAdapter extends RecyclerView.Adapter<DiagramSubjectAdapter.SubjectHolder> {
    private DBGroups mSubjectDb = DBGroups.getInstance();

    private AdapterListener mAdapterListener;
    private Cursor mCursor;
    private int[] mColorArray;
    private Context mContext;

    public DiagramSubjectAdapter(Context context, int[] colorArray) {
        mAdapterListener = (AdapterListener) context;
        mColorArray = colorArray;
        mContext = context;
        requery();
    }

    private void requery() {
        if (mCursor != null)
            mCursor.close();
        mCursor = null;
        //should be done asynchronously, but i guess that does not matter for 20 entries...
        mCursor = mSubjectDb.getAllGroupNames();
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
        mCursor.moveToPosition(position);

        //load data
        final int subjectId = mCursor.getInt(0);
        String name = mCursor.getString(1);

        //set tag for later identification avoiding all
        holder.itemView.setTag(subjectId);

        final boolean enoughLessons = DBLessons.getInstance().getLessonCountForSubject(subjectId) > 1;

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enoughLessons) {
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                    mAdapterListener.onClick(subjectId, holder.checkBox.isChecked(), mColorArray[position]);
                } else
                    Toast.makeText(mContext, mContext.getString(R.string.diagram_not_enough_entries), Toast.LENGTH_SHORT).show();
            }
        });

        if (!enoughLessons) {
            holder.checkBox.setEnabled(false);
        }

        holder.checkBox.setChecked(false);

        holder.checkBox.setHighlightColor(mColorArray[position]);

        //set texts
        holder.title.setText(name);
        holder.indicator.clearColorFilter();
        holder.indicator.setColorFilter(mColorArray[position]);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
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