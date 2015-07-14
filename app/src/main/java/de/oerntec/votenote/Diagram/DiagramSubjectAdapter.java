package de.oerntec.votenote.Diagram;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import de.oerntec.votenote.Database.DBLessons;
import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.R;

public class DiagramSubjectAdapter extends RecyclerView.Adapter<DiagramSubjectAdapter.SubjectHolder> {
    private DBSubjects mSubjectDb = DBSubjects.getInstance();

    private AdapterListener mAdapterListener;
    private Cursor mCursor;
    private int[] mColorArray;

    public DiagramSubjectAdapter(AdapterListener listener, int[] colorArray) {
        mAdapterListener = listener;
        mColorArray = colorArray;
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

        if (DBLessons.getInstance().getLessonCountForSubject(subjectId) <= 0) {
            holder.itemView.setEnabled(false);
            holder.checkBox.setEnabled(false);
        } else {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.checkBox.setChecked(!holder.checkBox.isChecked());
                    mAdapterListener.onClick(subjectId, holder.checkBox.isChecked(), mColorArray[position]);
                }
            });
        }

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