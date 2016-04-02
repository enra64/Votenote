package de.oerntec.votenote.percentage_tracker_overview;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import de.oerntec.votenote.R;
import de.oerntec.votenote.database.pojo.percentagetracker.EstimationModeDependentResults;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerCalculationResult;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerCalculator;
import de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo;
import de.oerntec.votenote.database.tablehelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.database.tablehelpers.DBLessons;
import de.oerntec.votenote.helpers.Preferences;
import de.oerntec.votenote.helpers.forecasting.ForecastModeSpinnerAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AdmissionPercentageOverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdmissionPercentageOverviewFragment extends Fragment {
    public static final String PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID = "admission_counter_id";

    private PercentageTrackerPojo mAdmissionCounterMetaPojo;

    private Spinner mEstimationModeSpinner;

    private List<List<String>> mContentList;
    private ArrayAdapter<String> mAdapter;

    /**
     * Required empty public constructor
     */
    public AdmissionPercentageOverviewFragment() {
    }

    public static AdmissionPercentageOverviewFragment newInstance(int admissionCounterId) {
        AdmissionPercentageOverviewFragment fragment = new AdmissionPercentageOverviewFragment();
        Bundle args = new Bundle();
        args.putInt(PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID, admissionCounterId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get pojo
        int admissionCounterId = getArguments().getInt(PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID);
        mAdmissionCounterMetaPojo = DBAdmissionPercentageMeta.getInstance().getItem(admissionCounterId);

        //do we want reverse sort?
        boolean lastestLessonFirst = Preferences.getPreference(getActivity(), "reverse_lesson_sort", false);

        //load all data
        mAdmissionCounterMetaPojo.loadData(DBLessons.getInstance(), lastestLessonFirst);

        //calculate all data
        PercentageTrackerCalculationResult data =
                PercentageTrackerCalculator.calculateAll(mAdmissionCounterMetaPojo);

        mContentList = new ArrayList<>();

        fillContentList(mAdmissionCounterMetaPojo, data);
    }

    private void fillContentList(PercentageTrackerPojo metaPojo, PercentageTrackerCalculationResult data) {
        //this is the order as in the ordinals of the enum
        mContentList.add(createSingleList(metaPojo, data, PercentageTrackerPojo.EstimationMode.user));
        mContentList.add(createSingleList(metaPojo, data, PercentageTrackerPojo.EstimationMode.mean));
        mContentList.add(createSingleList(metaPojo, data, PercentageTrackerPojo.EstimationMode.best));
        mContentList.add(createSingleList(metaPojo, data, PercentageTrackerPojo.EstimationMode.worst));
    }

    private void setAdapterDataset(int enumerationOrdinal) {
        mAdapter.clear();
        mAdapter.addAll(mContentList.get(enumerationOrdinal));
        mAdapter.notifyDataSetChanged();
    }

    private void onForecastModeChange(int ordinal) {
        setAdapterDataset(ordinal);
    }

    private ArrayList<String> createSingleList(PercentageTrackerPojo metaPojo, PercentageTrackerCalculationResult data, PercentageTrackerPojo.EstimationMode mode) {
        ArrayList<String> list = new ArrayList<>(11);

        //independent results
        list.add(getActivity().getString(R.string.overview_overall_lessons) + mAdmissionCounterMetaPojo.userLessonCountEstimation);
        list.add(getActivity().getString(R.string.overview_entered_lesson_count) + data.numberOfPastLessons);
        list.add(getActivity().getString(R.string.overview_future_lesson_count) + data.numberOfFutureLessons);
        list.add(getActivity().getString(R.string.overview_past_available_count) + data.numberOfPastAvailableAssignments);
        list.add(getActivity().getString(R.string.overview_past_finished_count) + data.numberOfFinishedAssignments);
        list.add(getActivity().getString(R.string.overview_percentage) + metaPojo.getAverageFinished());

        EstimationModeDependentResults results =
                data.getEstimationDependentResults(mode);

        //estimation mode dependent results
        if (mAdmissionCounterMetaPojo.bonusTargetPercentageEnabled)
            list.add("Bonus erreichbar: " + (results.bonusReachable ? "Ja" : "Nein"));
        else
            list.add("Bonus nicht aktiviert.");

        list.add(getActivity().getString(R.string.overview_available_assignments_per_lesson) + results.numberOfAssignmentsEstimatedPerLesson);
        list.add(getActivity().getString(R.string.overview_overall_available_assignments) + results.numberOfEstimatedOverallAssignments);
        list.add(getActivity().getString(R.string.overview_overall_required_assignments) + results.numberOfNeededAssignments);
        list.add(getActivity().getString(R.string.overview_remaining_required_finished_assignments) + results.numberOfRemainingNeededAssignments);
        list.add(getActivity().getString(R.string.overview_assignments_per_lesson) + results.numberOfAssignmentsNeededPerLesson);
        return list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_admission_percentage_overview, container, false);

        setHasOptionsMenu(true);

        //find views
        ListView mDataListView = (ListView) rootView.findViewById(R.id.fragment_admission_percentage_overview_list);
        mEstimationModeSpinner = (Spinner) rootView.findViewById(R.id.fragment_admission_percentage_overview_spinner);

        //listView init
        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
        mDataListView.setAdapter(mAdapter);

        spinnerInitialisation();

        return rootView;
    }

    private void spinnerInitialisation() {
        mEstimationModeSpinner.setAdapter(new ForecastModeSpinnerAdapter(getActivity()));

        mEstimationModeSpinner.setSelection(mAdmissionCounterMetaPojo.estimationMode.ordinal());

        mEstimationModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onForecastModeChange(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

}
