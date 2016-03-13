package de.oerntec.votenote.AdmissionPercentageOverview;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.AdmissionPercentageCalculationResult;
import de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.AdmissionPercentageCalculator;
import de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.EstimationModeDependentResults;
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

    private AdmissionPercentageMeta mAdmissionCounterMetaPojo;

    private ListView mDataListView;
    private SeekBar mEstimationModeSeekBar;
    private TextView mEstimationModeCurrentValueTextView;

    private String mCurrentEstimationMode;

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

        fillContentList(data);
    }

    private void fillContentList(AdmissionPercentageCalculationResult data) {
        //this is the order as in the ordinals of the enum
        mContentList.add(createSingleList(data, AdmissionPercentageMeta.EstimationMode.user));
        mContentList.add(createSingleList(data, AdmissionPercentageMeta.EstimationMode.mean));
        mContentList.add(createSingleList(data, AdmissionPercentageMeta.EstimationMode.best));
        mContentList.add(createSingleList(data, AdmissionPercentageMeta.EstimationMode.worst));
    }

    private void setAdapterDataset(int enumerationOrdinal){
        mAdapter.clear();
        mAdapter.addAll(mContentList.get(enumerationOrdinal));
        mAdapter.notifyDataSetChanged();
    }

    private void onEstimationModeChange(int seekbarProgress) {
        switch (AdmissionPercentageMeta.EstimationMode.values()[seekbarProgress]) {
            case user://user
                mCurrentEstimationMode = "user";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_user);
                break;
            case mean://mean
                mCurrentEstimationMode = "mean";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_mean);
                break;
            case best://best
                mCurrentEstimationMode = "best";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_best);
                break;
            case worst://worst
                mCurrentEstimationMode = "worst";
                mEstimationModeCurrentValueTextView.setText(R.string.estimation_name_worst);
                break;
            default:
                throw new AssertionError("unknown progress value!");
        }
        setAdapterDataset(seekbarProgress);
    }

    private ArrayList<String> createSingleList(AdmissionPercentageCalculationResult data, AdmissionPercentageMeta.EstimationMode mode) {
        ArrayList<String> list = new ArrayList<>(11);

        //independent results
        list.add("Geschätzte Gesamtanzahl an Übungen: " + mAdmissionCounterMetaPojo.userLessonCountEstimation);
        list.add("Eingetragene Übungen: " + data.numberOfPastLessons);
        list.add("Zukünftige Übungen: " + data.numberOfFutureLessons);
        list.add("Anzahl vergangener Aufgaben:" + data.numberOfPastAvailableAssignments);
        list.add("Anzahl votierter Aufgaben:" + data.numberOfFinishedAssignments);

        EstimationModeDependentResults results =
                data.getEstimationDependentResults(mode);

        //estimation mode dependent results
        if(mAdmissionCounterMetaPojo.bonusTargetPercentageEnabled)
            list.add("Bonus erreichbar: " + (results.bonusReachable ? "Ja" : "Nein"));
        else
            list.add("Bonus nicht aktiviert.");

        list.add("Geschätzte Aufgaben pro Übung (in der Zukunft): " + results.numberOfAssignmentsEstimatedPerLesson);
        list.add("Geschätzte Gesamtanzahl an Aufgaben: " + results.numberOfEstimatedOverallAssignments);
        list.add("Geschätzte Anzahl an gesamt benötigten Aufgaben: " + results.numberOfNeededAssignments);
        list.add("Geschätzte Anzahl an noch benötigten Aufgaben: " + results.numberOfRemainingNeededAssignments);
        list.add("Geschätzte Anzahl an pro Übung benötigten Aufgaben: " + results.numberOfAssignmentsNeededPerLesson);
        return list;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_admission_percentage_overview, container, false);

        //find views
        mEstimationModeSeekBar = (SeekBar) rootView.findViewById(R.id.fragment_admission_percentage_overview_seekBar);
        mDataListView = (ListView) rootView.findViewById(R.id.fragment_admission_percentage_overview_list);
        mEstimationModeCurrentValueTextView = (TextView) rootView.findViewById(R.id.fragment_admission_percentage_overview_estimation_mode_current_text_view);

        //listView init
        mAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>());
        mDataListView.setAdapter(mAdapter);

        //seekbar initialisation
        mEstimationModeSeekBar.setMax(AdmissionPercentageMeta.EstimationMode.values().length - 2);
        mEstimationModeSeekBar.setProgress(mAdmissionCounterMetaPojo.estimationMode.ordinal());
        mEstimationModeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onEstimationModeChange(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        onEstimationModeChange(mEstimationModeSeekBar.getProgress());

        return rootView;
    }

}
