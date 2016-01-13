package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation.CreationFragment;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
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
import de.oerntec.votenote.Database.Pojo.Subject;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;
import de.oerntec.votenote.R;

public class UnifiedCreatorAdapter extends RecyclerView.Adapter<UnifiedCreatorAdapter.ViewHolder> implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final int VIEW_INFO = 0;
    private static final int VIEW_PERCENTAGE = 1;
    private static final int VIEW_COUNTER = 2;

    private List<AdmissionCounter> mCounterData;
    private List<AdmissionPercentageMeta> mPercentageData;

    private DBAdmissionCounters mCounterDb;
    private DBAdmissionPercentageMeta mPercentageDb;

    private String mSubjectName = null;

    private Context mContext;

    public UnifiedCreatorAdapter(Context context, String subjectName){
        mContext = context;
        mCounterDb = DBAdmissionCounters.getInstance();
        mPercentageDb = DBAdmissionPercentageMeta.getInstance();
        mSubjectName = subjectName;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType){
            case VIEW_INFO:
            case VIEW_COUNTER:
            case VIEW_PERCENTAGE:
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.type){
            case VIEW_INFO:
                InfoViewHolder viewHolder = (InfoViewHolder) holder;
                if(mSubjectName != null)
                    viewHolder.name.setText(mSubjectName);
                viewHolder.counterAddButton.setOnClickListener(this);
                viewHolder.percentageAddButton.setOnClickListener(this);
                viewHolder.notificationSwitch.setOnCheckedChangeListener(this);
                break;
            case VIEW_COUNTER:
                break;
            case VIEW_PERCENTAGE:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mCounterData.size() + mPercentageData.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0)
            return VIEW_INFO;
        if(position < mCounterData.size())
            return VIEW_COUNTER;
        return VIEW_PERCENTAGE;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.subject_manager_fragment_main_card_add_percentage_counter){

        }
        else{

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public int type;

        public ViewHolder(View itemView) {
            super(itemView);
        }

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
