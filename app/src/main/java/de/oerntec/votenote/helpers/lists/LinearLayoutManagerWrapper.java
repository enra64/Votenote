package de.oerntec.votenote.helpers.lists;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import de.oerntec.votenote.MainActivity;

public class LinearLayoutManagerWrapper extends LinearLayoutManager {
    public LinearLayoutManagerWrapper(Context context) {
        super(context);
    }

    //... constructor
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (IndexOutOfBoundsException e) {
            if (MainActivity.ENABLE_DEBUG_LOG_CALLS)
                Log.e("probe", "meet a IOOBE in RecyclerView");
        }
    }
}