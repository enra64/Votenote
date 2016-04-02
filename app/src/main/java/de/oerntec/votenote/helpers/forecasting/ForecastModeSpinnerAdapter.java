package de.oerntec.votenote.helpers.forecasting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.subject_management.percentage_tracker_creation.PercentageTrackerCreationActivity;

/**
 * Class used to populate the spinner for {@link PercentageTrackerCreationActivity}
 */
public class ForecastModeSpinnerAdapter extends ArrayAdapter<String> implements SpinnerAdapter {
    private final LayoutInflater mInflater;

    public ForecastModeSpinnerAdapter(Context context) {
        super(context, 0);
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

        title.setText(ForecastModeInfo.getTitle(getContext(), position));
        subText.setText(ForecastModeInfo.getDescription(getContext(), position));

        return row;
    }

    @Override
    public int getCount() {
        return PercentageTrackerPojo.EstimationMode.values().length - 1;
    }


}
