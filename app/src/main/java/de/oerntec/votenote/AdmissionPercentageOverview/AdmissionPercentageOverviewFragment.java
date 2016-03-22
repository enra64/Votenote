package de.oerntec.votenote.AdmissionPercentageOverview;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageCalculationResult;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageCalculator;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.EstimationModeDependentResults;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.MainActivity;
import de.oerntec.votenote.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AdmissionPercentageOverviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdmissionPercentageOverviewFragment extends Fragment {
    public static final String PARAMETER_NAME_ADMISSION_PERCENTAGE_COUNTER_ID = "admission_counter_id";

    private AdmissionPercentageMetaPojo mAdmissionCounterMetaPojo;

    private ListView mDataListView;
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
        boolean lastestLessonFirst = MainActivity.getPreference("reverse_lesson_sort", false);

        //load all data
        mAdmissionCounterMetaPojo.loadData(DBAdmissionPercentageData.getInstance(), lastestLessonFirst);

        //calculate all data
        AdmissionPercentageCalculationResult data =
                AdmissionPercentageCalculator.calculateAll(mAdmissionCounterMetaPojo);

        mContentList = new ArrayList<>();

        setHasOptionsMenu(true);

        fillContentList(mAdmissionCounterMetaPojo, data);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_subject_overview, menu);
        MenuItem spinnerMenuItem = menu.findItem(R.id.overview_spinner);
        View view1 = spinnerMenuItem.setActionView(new Spinner(getContext())).getActionView();
        if (view1 instanceof Spinner) {
            mEstimationModeSpinner = (Spinner) view1;

            String user = getString(R.string.estimation_name_user);
            String mean = getString(R.string.estimation_name_mean);
            String best = getString(R.string.estimation_name_best);
            String worst = getString(R.string.estimation_name_worst);

            List<String> dataList = Arrays.asList(user, mean, best, worst);

            mEstimationModeSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, dataList));

            mEstimationModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    onEstimationModeChange(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void fillContentList(AdmissionPercentageMetaPojo metaPojo, AdmissionPercentageCalculationResult data) {
        //this is the order as in the ordinals of the enum
        mContentList.add(createSingleList(metaPojo, data, AdmissionPercentageMetaPojo.EstimationMode.user));
        mContentList.add(createSingleList(metaPojo, data, AdmissionPercentageMetaPojo.EstimationMode.mean));
        mContentList.add(createSingleList(metaPojo, data, AdmissionPercentageMetaPojo.EstimationMode.best));
        mContentList.add(createSingleList(metaPojo, data, AdmissionPercentageMetaPojo.EstimationMode.worst));
    }

    private void setAdapterDataset(int enumerationOrdinal) {
        mAdapter.clear();
        mAdapter.addAll(mContentList.get(enumerationOrdinal));
        mAdapter.notifyDataSetChanged();
    }

    private void onEstimationModeChange(int seekbarProgress) {
        setAdapterDataset(seekbarProgress);
    }

    private ArrayList<String> createSingleList(AdmissionPercentageMetaPojo metaPojo, AdmissionPercentageCalculationResult data, AdmissionPercentageMetaPojo.EstimationMode mode) {
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
        mDataListView = (ListView) rootView.findViewById(R.id.fragment_admission_percentage_overview_list);

        //listView init
        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
        mDataListView.setAdapter(mAdapter);

        if (mEstimationModeSpinner != null)
            mEstimationModeSpinner.setSelection(mAdmissionCounterMetaPojo.estimationMode.ordinal());

        return rootView;
    }

}
