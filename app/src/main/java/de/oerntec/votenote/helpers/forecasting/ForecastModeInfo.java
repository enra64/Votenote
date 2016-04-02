package de.oerntec.votenote.helpers.forecasting;

import android.content.Context;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;

public class ForecastModeInfo {
    public static String getTitle(Context context, int ordinal) {
        PercentageTrackerPojo.EstimationMode tmp = PercentageTrackerPojo.EstimationMode.values()[ordinal];
        switch (tmp) {
            case user:
                return context.getString(R.string.estimation_name_user);
            case mean:
                return context.getString(R.string.estimation_name_mean);
            case best:
                return context.getString(R.string.estimation_name_best);
            case worst:
                return context.getString(R.string.estimation_name_worst);
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode");
        }
    }

    public static String getDescription(Context context, int ordinal) {
        PercentageTrackerPojo.EstimationMode tmp = PercentageTrackerPojo.EstimationMode.values()[ordinal];
        switch (tmp) {
            case user:
                return context.getString(R.string.estimation_description_user);
            case mean:
                return context.getString(R.string.estimation_description_mean);
            case best:
                return context.getString(R.string.estimation_description_best);
            case worst:
                return context.getString(R.string.estimation_description_worst);
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode");
        }
    }
}
