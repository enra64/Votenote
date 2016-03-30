package de.oerntec.votenote.subject_management.percentage_tracker_creation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;

/**
 * Class used to populate the spinner for {@link PercentageTrackerCreationActivity}
 */
public class EstimationModeSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    LayoutInflater mInflater;

    public EstimationModeSpinnerAdapter(Context context, int resource) {
        super(context, resource);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, @SuppressWarnings("UnusedParameters") View convertView, ViewGroup parent) {
        //noinspection deprecation
        TwoLineListItem row = (TwoLineListItem) mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);

        TextView title = (TextView) row.findViewById(android.R.id.text1);
        TextView subText = (TextView) row.findViewById(android.R.id.text2);

        title.setText(getTitle(position));
        subText.setText(getDescription(position));

        return row;
    }

    @Override
    public int getCount() {
        return PercentageTrackerPojo.EstimationMode.values().length - 1;
    }

    private String getTitle(int ordinal) {
        PercentageTrackerPojo.EstimationMode tmp = PercentageTrackerPojo.EstimationMode.values()[ordinal];
        switch (tmp) {
            case user:
                return getContext().getString(R.string.estimation_name_user);
            case mean:
                return getContext().getString(R.string.estimation_name_mean);
            case best:
                return getContext().getString(R.string.estimation_name_best);
            case worst:
                return getContext().getString(R.string.estimation_name_worst);
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode");
        }
    }

    private String getDescription(int ordinal) {
        PercentageTrackerPojo.EstimationMode tmp = PercentageTrackerPojo.EstimationMode.values()[ordinal];
        switch (tmp) {
            case user:
                return getContext().getString(R.string.estimation_description_user);
            case mean:
                return getContext().getString(R.string.estimation_description_mean);
            case best:
                return getContext().getString(R.string.estimation_description_best);
            case worst:
                return getContext().getString(R.string.estimation_description_worst);
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode");
        }
    }
}
