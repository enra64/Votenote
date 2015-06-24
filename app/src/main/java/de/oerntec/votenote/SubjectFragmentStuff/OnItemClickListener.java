package de.oerntec.votenote.SubjectFragmentStuff;

import android.view.View;

interface OnItemClickListener {
    void onItemClick(View view, int position);

    void onItemLongClick(View view, int position);
}