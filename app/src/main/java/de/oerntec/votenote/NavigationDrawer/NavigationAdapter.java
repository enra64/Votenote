package de.oerntec.votenote.NavigationDrawer;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import de.oerntec.votenote.Database.DBSubjects;
import de.oerntec.votenote.R;

public class NavigationAdapter extends RecyclerView.Adapter<NavigationAdapter.SubjectHolder> {
    private DBSubjects mSubjectDb = DBSubjects.getInstance();

    private SelectionCallback mOnClickCallback;
    private Cursor mCursor;
    private int mCurrentSelection, mOldSelection;
    private boolean mSelectionApplied;
    private Context mContext;

    public NavigationAdapter(Context context, SelectionCallback callbacks) {
        mOnClickCallback = callbacks;
        mContext = context;
        reQuery();
    }

    public void notifyForReload() {
        reQuery();
        notifyItemRangeChanged(0, mCursor.getCount());
    }

    private void reQuery() {
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
        mCursor.moveToPosition(position);

        //load strings
        int subjectId = mCursor.getInt(0);
        String name = mCursor.getString(1);
        holder.subjectId = mCursor.getInt(0);

        //set tag for later identification avoiding all
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
        return mCursor.getCount();
    }

    public void setCurrentSelection(int position) {
        mSelectionApplied = false;
        mOldSelection = mCurrentSelection;
        mCurrentSelection = position;
    }

    public void drawerClosed() {
        if (mSelectionApplied)
            return;
        mSelectionApplied = true;
        notifyItemChanged(mOldSelection);
        notifyItemChanged(mCurrentSelection);
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