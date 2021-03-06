/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.helpers.lists;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import de.oerntec.votenote.percentage_tracker_fragment.PercentageTrackerAdapter;

public class SwipeAnimationUtils {
    /**
     * Swipe view to the right to show user fast deletion method. Also calls UndoSnackBar
     *
     * @param context      context for animation
     * @param snackBarHost class having an interface to show a udo snackbar
     * @param animatedView animated view
     */
    public static void executeProgrammaticSwipeDeletion(
            final Context context,
            final UndoSnackBarHost snackBarHost,
            final View animatedView,
            final int position) {
        //delay animation to start after dialog closed
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //show animation
                Animation anim = AnimationUtils.loadAnimation(context, android.R.anim.slide_out_right);
                anim.setDuration(200);
                animatedView.startAnimation(anim);

                //hide view after animation finish
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        //avoid flickering
                        animatedView.setVisibility(View.INVISIBLE);
                    }
                }, anim.getDuration());

                // get the id. it may be only accessible under
                Integer id = (Integer) animatedView.getTag(PercentageTrackerAdapter.ID_TAG);
                if(id == null)
                    id = (Integer) animatedView.getTag();

                if(id == null)
                    throw new AssertionError("unknown lesson id format");

                //delete the lesson from db with undobar
                snackBarHost.showUndoSnackBar(id, position);
            }
        }, 300);
    }

    /**
     * Interface to generify use of the swipe deletion code
     */
    public interface UndoSnackBarHost {
        /**
         * called to show the undo snack bar
         *
         * @param id database id
         * @param position position of the view in the recyclerview
         */
        void showUndoSnackBar(int id, int position);
    }


}