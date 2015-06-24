package de.oerntec.votenote.SubjectFragmentStuff;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import de.oerntec.votenote.R;
import de.oerntec.votenote.SwipeableRecyclerViewTouchListener;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonHolder> {
    //see stackoverflow: http://stackoverflow.com/questions/26517855/using-the-recyclerview-with-a-database
    // PATCH: Because RecyclerView.Adapter in its current form doesn't natively support
    // cursors, we "wrap" a CursorAdapter that will do all teh job
    // for us
    private CursorAdapter mCursorAdapter;
    private SwipeableRecyclerViewTouchListener swipeListener;
    private Context mContext;

    public LessonAdapter(Context context, Cursor c, SwipeableRecyclerViewTouchListener swipeListener) {
        this.swipeListener = swipeListener;
        mContext = context;

        //basically the old lesson adapter
        mCursorAdapter = new CursorAdapter(mContext, c, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                return inflater.inflate(R.layout.subject_fragment_lesson_card, parent, false);
            }

            @Override
            public void bindView(final View view, final Context context, Cursor cursor) {
                //find textviews
                TextView upper = (TextView) view.findViewById(R.id.subject_fragment_card_upper);
                TextView lower = (TextView) view.findViewById(R.id.subject_fragment_card_lower);
                ImageButton deleteButton = (ImageButton) view.findViewById(R.id.subject_fragment_card_delete_button);

                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LessonAdapter.this.swipeListener.deleteItem(view);
                    }
                });

                //load strings
                String lessonIndex = cursor.getString(0);
                String myVote = cursor.getString(1);
                String maxVote = cursor.getString(2);
                String voteString = " " + (Integer.valueOf(myVote) < 2 ? context.getString(R.string.subject_fragment_singular_vote)
                        : context.getString(R.string.main_dialog_lesson_votes));

                //set tag for later identification avoiding all
                view.setTag(Integer.valueOf(lessonIndex));

                //set texts
                upper.setText(myVote + " " + context.getString(R.string.main_dialog_lesson_von) + " " + maxVote + voteString);
                lower.setText(lessonIndex + context.getString(R.string.main_x_th_lesson));
            }
        };
    }

    public CursorAdapter getCursorAdapter() {
        return mCursorAdapter;
    }

    @Override
    public int getItemCount() {
        return mCursorAdapter.getCount();
    }

    @Override
    public void onBindViewHolder(LessonHolder holder, int position) {
        //move to the correct position
        mCursorAdapter.getCursor().moveToPosition(position);
        // Passing the binding operation to cursor loader
        mCursorAdapter.bindView(holder.itemView, mContext, mCursorAdapter.getCursor());
    }

    @Override
    public LessonHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Passing the inflater job to the cursor-adapter
        View v = mCursorAdapter.newView(mContext, mCursorAdapter.getCursor(), parent);
        return new LessonHolder(v);
    }

    class LessonHolder extends RecyclerView.ViewHolder {
        //View v1;

        public LessonHolder(View itemView) {
            super(itemView);
            //v1 = itemView.findViewById(R.id.v1);
        }
    }

}