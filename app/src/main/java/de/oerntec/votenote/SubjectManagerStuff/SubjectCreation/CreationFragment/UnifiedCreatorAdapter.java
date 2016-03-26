package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.Helpers.TextWatchers.FakeDisabledNotEmptyWatcher;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.SubjectOverview.Dialogs;

public class UnifiedCreatorAdapter extends RecyclerView.Adapter<UnifiedCreatorAdapter.ViewHolder> implements View.OnClickListener {
    static final int VIEW_INFO = 0;
    static final int VIEW_PERCENTAGE = 1;
    static final int VIEW_COUNTER = 2;

    private List<AdmissionCounter> mCounterData;
    private List<AdmissionPercentageMetaPojo> mPercentageData;

    private DBAdmissionCounters mCounterDb;
    private DBAdmissionPercentageMeta mPercentageDb;

    private EditText mNameInput;

    private String mSubjectName = null;
    private int mSubjectId;

    private Context mContext;
    private FragmentManager mFragmentManager;

    private Button mParentOkButton;

    private View.OnClickListener mEnabledOnClickListener;

    /**
     * Instance of the calling subject creation fragment, because we want to create the intents for
     * admission percentage counters there
     */
    private SubjectCreationFragment mSubjectCreationFragment;

    //this got out of hand
    public UnifiedCreatorAdapter(
            Context context,
            SubjectCreationFragment subjectCreationFragment,
            FragmentManager fragmentManager,
            String subjectName,
            int subjectId,
            Button okButton,
            View.OnClickListener enabledOnClickListener) {
        mContext = context;
        mCounterDb = DBAdmissionCounters.getInstance();
        mPercentageDb = DBAdmissionPercentageMeta.getInstance();
        mSubjectName = subjectName;
        mSubjectId = subjectId;
        mFragmentManager = fragmentManager;
        mSubjectCreationFragment = subjectCreationFragment;
        mParentOkButton = okButton;
        mEnabledOnClickListener = enabledOnClickListener;
        requery();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View root;
        switch (viewType){
            case VIEW_INFO:
                root = inflater.inflate(R.layout.subject_manager_fragment_main_card, parent, false);
                InfoViewHolder infoHolder = new InfoViewHolder(root);
                infoHolder.percentageAddButton = (Button) root.findViewById(R.id.subject_manager_fragment_main_card_add_percentage_counter);
                infoHolder.counterAddButton = (Button) root.findViewById(R.id.subject_manager_fragment_main_card_add_counter);
                mNameInput = (EditText) root.findViewById(R.id.subject_manager_fragment_main_card_name);
                infoHolder.name = mNameInput;
                return infoHolder;
            case VIEW_COUNTER:
                root = inflater.inflate(R.layout.subject_manager_fragment_counter_card, parent, false);
                CounterViewHolder counterHolder = new CounterViewHolder(root);
                counterHolder.targetPoints = (TextView) root.findViewById(R.id.subject_manager_fragment_counter_card_target_count);
                counterHolder.name = (TextView) root.findViewById(R.id.subject_manager_fragment_counter_card_name);
                return counterHolder;
            case VIEW_PERCENTAGE:
                root = inflater.inflate(R.layout.subject_manager_fragment_percentage_card, parent, false);
                PercentageViewHolder percentageHolder = new PercentageViewHolder(root);
                percentageHolder.targetPercentage = (TextView) root.findViewById(R.id.subject_manager_fragment_percentage_card_target_percentage);
                percentageHolder.targetLessonCount = (TextView) root.findViewById(R.id.subject_manager_fragment_percentage_card_target_lesson_count);
                percentageHolder.assignmentsPerLesson= (TextView) root.findViewById(R.id.subject_manager_fragment_percentage_card_assignments_per_lesson);
                percentageHolder.name = (TextView) root.findViewById(R.id.subject_manager_fragment_percentage_card_name);
                return percentageHolder;
            default:
                throw new AssertionError("unknown view type requested");
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.type){
            case VIEW_INFO:
                final InfoViewHolder infoHolder = (InfoViewHolder) holder;
                if(mSubjectName != null)
                    infoHolder.name.setText(mSubjectName);
                infoHolder.name.addTextChangedListener(
                        new FakeDisabledNotEmptyWatcher(
                                infoHolder.name,
                                mParentOkButton.getContext().getString(R.string.edit_text_empty_error_text),
                                mSubjectName == null || mSubjectName.isEmpty(),
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        infoHolder.name.setError(v.getContext().getString(R.string.edit_text_empty_error_text));
                                    }
                                },
                                mEnabledOnClickListener,
                                mParentOkButton));
                infoHolder.itemView.setTag(-1);
                infoHolder.counterAddButton.setOnClickListener(this);
                infoHolder.percentageAddButton.setOnClickListener(this);
                break;
            case VIEW_COUNTER:
                CounterViewHolder counterHolder = (CounterViewHolder) holder;
                AdmissionCounter counterData = getCounterPojoAtPosition(position);
                counterHolder.itemView.setTag(counterData.id);
                counterHolder.name.setText(counterData.counterName);
                counterHolder.targetPoints.setText(mContext.getString(R.string.minimum_points_text, counterData.targetValue));
                break;
            case VIEW_PERCENTAGE:
                PercentageViewHolder percentageHolder = (PercentageViewHolder) holder;
                AdmissionPercentageMetaPojo percentageData = getPercentagePojoAtPosition(position);
                percentageHolder.itemView.setTag(percentageData.id);
                percentageHolder.assignmentsPerLesson.setText(mContext.getString(R.string.percentage_card_assignments_per_lesson, percentageData.userAssignmentsPerLessonEstimation));
                percentageHolder.name.setText(percentageData.name);
                percentageHolder.targetLessonCount.setText(mContext.getString(R.string.percentage_card_target_lesson_conut, percentageData.userLessonCountEstimation));
                percentageHolder.targetPercentage.setText(mContext.getString(R.string.percentage_card_target_percentage, percentageData.baselineTargetPercentage));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mCounterData.size() + mPercentageData.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0)
            return VIEW_INFO;
        else if(position <= mCounterData.size())
            return VIEW_COUNTER;
        else
            return VIEW_PERCENTAGE;
    }

    private void requery() {
        mCounterData = null;
        mPercentageData = null;
        mCounterData = mCounterDb.getItemsForSubject(mSubjectId);
        mPercentageData = mPercentageDb.getItemsForSubject(mSubjectId);
    }

    private int convertCounterIdToPosition(int id){
        int pos;
        for(pos = 0; pos < mCounterData.size(); pos++)
            if(mCounterData.get(pos).id == id)
                break;
        return pos + 1;
    }

    private int convertPercentageIdToPosition(int id){
        int pos;
        for(pos = 0; pos < mPercentageData.size(); pos++)
            if(mPercentageData.get(pos).id == id)
                break;
        return pos + mCounterData.size() + 1;
    }

    public void removeCounter(int admissionCounterId) {
        int recyclerViewPosition = convertCounterIdToPosition(admissionCounterId);

        notifyItemRemoved(recyclerViewPosition);
        mCounterDb.deleteItem(admissionCounterId);
        requery();
    }

    public void removePercentage(int percentageId) {
        int recyclerViewPosition = convertPercentageIdToPosition(percentageId);

        notifyItemRemoved(recyclerViewPosition);
        mPercentageDb.deleteItem(percentageId);
        requery();
    }

    public void notifyOfChangeAtId(int id, boolean isPercentage){
        requery();
        notifyItemChanged(isPercentage ? convertPercentageIdToPosition(id) : convertCounterIdToPosition(id));
    }

    public void notifyIdAdded(int id, boolean isPercentage) {
        requery();
        notifyItemInserted(isPercentage ? convertPercentageIdToPosition(id) : convertCounterIdToPosition(id));
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.subject_manager_fragment_main_card_add_percentage_counter){
            mSubjectCreationFragment.openAdmissionPercentageCreationActivity(SubjectCreationFragment.ID_ADD_ITEM, true);
        }
        else{
            Dialogs.showCounterDialog(mFragmentManager, mSubjectId, SubjectCreationFragment.ID_ADD_ITEM, true);
        }
    }

    public AdmissionCounter getCounterPojoAtPosition(int position) {
        return mCounterData.get(position - 1);
    }

    public AdmissionPercentageMetaPojo getPercentagePojoAtPosition(int position) {
        return mPercentageData.get(position - 1 - mCounterData.size());
    }

    public String getCurrentName(){
        return mNameInput.getText().toString();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public int type;

        public ViewHolder(View itemView, int type){
            super(itemView);
            this.type = type;
        }
    }

    public class InfoViewHolder extends ViewHolder {
        public EditText name;
        public Button percentageAddButton, counterAddButton;

        public InfoViewHolder(View itemView) {
            super(itemView, VIEW_INFO);
        }
    }

    public class PercentageViewHolder extends ViewHolder {
        public TextView name, targetPercentage, assignmentsPerLesson, targetLessonCount;

        public PercentageViewHolder(View itemView) {
            super(itemView, VIEW_PERCENTAGE);
        }
    }

    public class CounterViewHolder extends ViewHolder {
        public TextView name, targetPoints;

        public CounterViewHolder(View itemView) {
            super(itemView, VIEW_COUNTER);
        }
    }
}
