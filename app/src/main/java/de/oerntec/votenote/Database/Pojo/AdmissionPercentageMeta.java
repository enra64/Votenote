package de.oerntec.votenote.Database.Pojo;

import android.graphics.Color;

import java.util.List;

import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.R;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMeta implements NameAndIdPojo {
    public int id, subjectId, estimatedAssignmentsPerLesson, estimatedLessonCount, targetPercentage;
    public String name;
    public List<AdmissionPercentageData> mDataList;
    private boolean mDataLoaded = false;

    private boolean mDataCalculated = false;
    private float numberOfLessonsLeft, scheduledNumberOfAssignments, neededAssignmentsPerUebung, numberOfNeededAssignments,
            remainingNeededAssignments, numberOfFinishedAssignments, numberOfElapsedLessons;

    public float getNumberOfNeededAssignments() {
        if(!mDataCalculated) calculateData();
        return numberOfNeededAssignments;
    }

    public float getRemainingNeededAssignments() {
        if(!mDataCalculated) calculateData();
        return remainingNeededAssignments;
    }

    public float getNumberOfFinishedAssignments() {
        if(!mDataCalculated) calculateData();
        return numberOfFinishedAssignments;
    }

    public float getNumberOfElapsedLessons() {
        if(!mDataCalculated) calculateData();
        return numberOfElapsedLessons;
    }

    public float getNeededAssignmentsPerUebung() {
        if(!mDataCalculated) calculateData();
        return neededAssignmentsPerUebung;
    }

    public float getNumberOfLessonsLeft() {
        if(!mDataCalculated) calculateData();
        return numberOfLessonsLeft;
    }

    public float getEstimatedNumberOfAssignments(){
        if(!mDataCalculated) calculateData();
        return scheduledNumberOfAssignments;
    }

    public void loadData(DBAdmissionPercentageData dataDb, boolean latestLessonFirst){
        mDataList = dataDb.getItemsForMetaId(id, latestLessonFirst);
        mDataLoaded = true;
    }

    public boolean getHasLessons(){
        if(!mDataLoaded) throw new AssertionError("no data loaded");
        return mDataList.size() > 0;
    }

    /**
     * calculate the interesting numbers from the raw data, and save them in this meta object
     */
    public void calculateData() {
        if(!mDataLoaded) throw new AssertionError("no data loaded");
        scheduledNumberOfAssignments = estimatedAssignmentsPerLesson * estimatedLessonCount;
        numberOfNeededAssignments = (scheduledNumberOfAssignments * (float) targetPercentage) / 100f;
        numberOfFinishedAssignments = getFinishedAssignments();
        remainingNeededAssignments = numberOfNeededAssignments - numberOfFinishedAssignments;
        numberOfElapsedLessons = mDataList.size();
        numberOfLessonsLeft = estimatedLessonCount - numberOfElapsedLessons;
        neededAssignmentsPerUebung = remainingNeededAssignments / numberOfLessonsLeft;
        mDataCalculated = true;
    }

    public int getFinishedAssignments(){
        if(!mDataLoaded) throw new AssertionError("no data loaded");
        int finishedAssignments = 0;
        for(AdmissionPercentageData d : mDataList)
            finishedAssignments += d.finishedAssignments;
        return finishedAssignments;
    }

    /**
     * calculate the current average vote
     */
    public float getAverageFinished() {
        if(!mDataLoaded) throw new AssertionError("no data loaded");
        int availableAssignments = 0, finishedAssignments = 0;
        for(AdmissionPercentageData d : mDataList){
            availableAssignments += d.availableAssignments;
            finishedAssignments += d.finishedAssignments;
        }
        return ((float) finishedAssignments / (float) availableAssignments) * 100.f;
    }

    public AdmissionPercentageMeta(int id, int subjectId, int estimatedAssignmentsPerLesson, int estimatedLessonCount, int targetPercentage, String name) {
        this.id = id;
        this.subjectId = subjectId;
        this.estimatedAssignmentsPerLesson = estimatedAssignmentsPerLesson;
        this.estimatedLessonCount = estimatedLessonCount;
        this.targetPercentage = targetPercentage;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionPercentageMeta that = (AdmissionPercentageMeta) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (estimatedAssignmentsPerLesson != that.estimatedAssignmentsPerLesson) return false;
        if (estimatedLessonCount != that.estimatedLessonCount) return false;
        if (targetPercentage != that.targetPercentage) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    public String getCsvRepresentation(){
        return name+","+targetPercentage+"%,"+estimatedAssignmentsPerLesson+","+estimatedLessonCount;
    }

    @Override
    public String getDisplayName() {
        return name + " - " + targetPercentage + "%";
    }

    @Override
    public int getId() {
        return id;
    }
}
