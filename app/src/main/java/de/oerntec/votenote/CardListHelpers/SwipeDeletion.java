package de.oerntec.votenote.CardListHelpers;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class SwipeDeletion {
    /**
     * Swipe view to the right to show user fast deletion method. Also calls UndoSnackBar
     *
     * @param context      context for animation
     * @param snackBarHost class having an interface to show a udo snackbar
     * @param animatedView animated view
     */
    public static void executeProgrammaticSwipeDeletion(final Context context, final UndoSnackBarHost snackBarHost, final View animatedView) {
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

                //delete the lesson from db with undobar
                snackBarHost.showUndoSnackBar((Integer) animatedView.getTag());
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
         * @param id lesson/subject id
         */
        void showUndoSnackBar(int id);
    }


}