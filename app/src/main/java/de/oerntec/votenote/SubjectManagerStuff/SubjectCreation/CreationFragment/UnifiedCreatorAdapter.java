package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.R;
import de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.Dialogs;

public class UnifiedCreatorAdapter extends RecyclerView.Adapter<UnifiedCreatorAdapter.ViewHolder> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    static final int VIEW_INFO = 0;
    static final int VIEW_PERCENTAGE = 1;
    static final int VIEW_COUNTER = 2;

    private List<AdmissionCounter> mCounterData;
    private List<AdmissionPercentageMeta> mPercentageData;

    private DBAdmissionCounters mCounterDb;
    private DBAdmissionPercentageMeta mPercentageDb;

    private EditText mNameInput;

    private String mSubjectName = null;
    private int mSubjectId;

    private Context mContext;
    private FragmentManager mFragmentManager;

    public UnifiedCreatorAdapter(Context context, FragmentManager fragmentManager, String subjectName, int subjectId){
        mContext = context;
        mCounterDb = DBAdmissionCounters.getInstance();
        mPercentageDb = DBAdmissionPercentageMeta.getInstance();
        mSubjectName = subjectName;
        mSubjectId = subjectId;
        mFragmentManager = fragmentManager;
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
                infoHolder.notificationSwitch = (Switch) root.findViewById(R.id.subject_manager_fragment_main_card_notification_switch);
                infoHolder.percentageAddButton = (Button) root.findViewById(R.id.subject_manager_fragment_main_card_add_percentage_counter);
                infoHolder.counterAddButton = (Button) root.findViewById(R.id.subject_manager_fragment_main_card_add_counter);
                infoHolder.name = (EditText) root.findViewById(R.id.subject_manager_fragment_main_card_name);
                mNameInput = (EditText) root.findViewById(R.id.subject_manager_fragment_main_card_name);
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
                InfoViewHolder infoHolder = (InfoViewHolder) holder;
                if(mSubjectName != null)
                    infoHolder.name.setText(mSubjectName);
                infoHolder.itemView.setTag(-1);
                infoHolder.counterAddButton.setOnClickListener(this);
                infoHolder.percentageAddButton.setOnClickListener(this);
                infoHolder.notificationSwitch.setOnCheckedChangeListener(this);
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
                AdmissionPercentageMeta percentageData = getPercentagePojoAtPosition(position);
                percentageHolder.itemView.setTag(percentageData.id);
                percentageHolder.assignmentsPerLesson.setText(mContext.getString(R.string.percentage_card_assignments_per_lesson, percentageData.estimatedAssignmentsPerLesson));
                percentageHolder.name.setText(percentageData.name);
                percentageHolder.targetLessonCount.setText(mContext.getString(R.string.percentage_card_target_lesson_conut, percentageData.estimatedLessonCount));
                percentageHolder.targetPercentage.setText(mContext.getString(R.string.percentage_card_target_percentage, percentageData.targetPercentage));
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.subject_manager_fragment_main_card_add_percentage_counter){
            Dialogs.showPercentageDialog(mFragmentManager, mSubjectId, SubjectCreationActivityFragment.ID_ADD_ITEM, true);
        }
        else{
            Dialogs.showCounterDialog(mFragmentManager, mSubjectId, SubjectCreationActivityFragment.ID_ADD_ITEM, true);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //TODO: notifcation logic
    }

    public AdmissionCounter getCounterPojoAtPosition(int position) {
        return mCounterData.get(position - 1);
    }

    public AdmissionPercentageMeta getPercentagePojoAtPosition(int position) {
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
        public Switch notificationSwitch;
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
