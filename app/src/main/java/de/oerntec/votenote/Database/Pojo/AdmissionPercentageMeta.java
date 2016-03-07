package de.oerntec.votenote.Database.Pojo;

import java.util.List;

import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMeta implements NameAndIdPojo {
    public int id, subjectId, estimatedLessonCount, targetPercentage, userAssignmentsPerLessonEstimation;
    public String name;
    public List<AdmissionPercentageData> mDataList;
    public EstimationMode estimationMode = EstimationMode.undefined;
    private boolean mDataLoaded = false;
    private boolean mDataCalculated = false;
    private float numberOfLessonsLeft, scheduledNumberOfAssignments, neededAssignmentsPerUebung, numberOfNeededAssignments,
            remainingNeededAssignments, numberOfFinishedAssignments, numberOfElapsedLessons;

    public AdmissionPercentageMeta(int id, int subjectId, int estimatedAssignmentsPerLesson, int estimatedLessonCount, int targetPercentage, String name, String mode) {
        this.id = id;
        this.subjectId = subjectId;
        this.estimatedLessonCount = estimatedLessonCount;
        this.userAssignmentsPerLessonEstimation = estimatedAssignmentsPerLesson;
        this.targetPercentage = targetPercentage;
        this.name = name;

        switch (mode) {
            case "user":
                estimationMode = EstimationMode.user;
            case "mean":
                estimationMode = EstimationMode.mean;
            case "worst":
                estimationMode = EstimationMode.worst;
            case "best":
                estimationMode = EstimationMode.best;
        }
    }

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
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        return mDataList.size() > 0;
    }

    /**
     * calculate the interesting numbers from the raw data, and save them in this meta object
     */
    public void calculateData() {
        if (!mDataLoaded)
            throw new AssertionError("pojo has not loaded data");
        scheduledNumberOfAssignments = getEstimatedAssignmentsPerLesson() * estimatedLessonCount;
        numberOfNeededAssignments = (scheduledNumberOfAssignments * (float) targetPercentage) / 100f;
        numberOfFinishedAssignments = getFinishedAssignments();
        remainingNeededAssignments = numberOfNeededAssignments - numberOfFinishedAssignments;
        numberOfElapsedLessons = mDataList.size();
        numberOfLessonsLeft = estimatedLessonCount - numberOfElapsedLessons;
        neededAssignmentsPerUebung = remainingNeededAssignments / numberOfLessonsLeft;
        mDataCalculated = true;
    }

    /**
     * This estimates a lesson count by returning one the best/worst/mean/given number of remaining assignments
     */
    public int getEstimatedAssignmentsPerLesson() {
        switch (estimationMode) {
            case user:
                return userAssignmentsPerLessonEstimation;
            case mean:
                return getAvailableAssignments() / mDataList.size();
            case best:
                return getMinAvailableAssignments();
            case worst:
                return getMaxAvailableAssignments();
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode!");
        }
    }

    public String getEstimationModeAsString() {
        switch (estimationMode) {
            case user:
                return "user";
            case mean:
                return "mean";
            case best:
                return "best";
            case worst:
                return "worst";
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode!");
        }
    }

    public int getFinishedAssignments(){
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        int finishedAssignments = 0;
        for(AdmissionPercentageData d : mDataList)
            finishedAssignments += d.finishedAssignments;
        return finishedAssignments;
    }

    public int getAvailableAssignments() {
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        int availableAssignments = 0;
        for (AdmissionPercentageData d : mDataList)
            availableAssignments += d.availableAssignments;
        return availableAssignments;
    }

    public int getMinAvailableAssignments() {
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        int minAssignments = Integer.MAX_VALUE;
        for (AdmissionPercentageData d : mDataList)
            minAssignments = d.availableAssignments < minAssignments ? d.availableAssignments : minAssignments;
        return minAssignments;
    }

    public int getMaxAvailableAssignments() {
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        int maxAssignments = 0;
        for (AdmissionPercentageData d : mDataList)
            maxAssignments = d.availableAssignments > maxAssignments ? d.availableAssignments : maxAssignments;
        return maxAssignments;
    }

    /**
     * calculate the current average vote
     */
    public float getAverageFinished() {
        return getAverageFinished(0, 0);
    }

    public float getAverageFinished(int addToAvailable, int addToFinished) {
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        int availableAssignments = 0, finishedAssignments = 0;
        for(AdmissionPercentageData d : mDataList){
            availableAssignments += d.availableAssignments;
            finishedAssignments += d.finishedAssignments;
        }
        finishedAssignments += addToFinished;
        availableAssignments += addToAvailable;

        //safeguard against weird numbers possibly produced by adding the values
        finishedAssignments = finishedAssignments < 0 ? 0 : finishedAssignments;
        availableAssignments = availableAssignments < 0 ? 0 : availableAssignments;
        float avg = ((float) finishedAssignments / (float) availableAssignments) * 100.f;
        //safeguard against weird numbers possibly produced by adding the values
        return Float.isInfinite(avg) || Float.isNaN(avg) ? -1 : avg;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionPercentageMeta that = (AdmissionPercentageMeta) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (estimationMode != that.estimationMode) return false;
        if (estimatedLessonCount != that.estimatedLessonCount) return false;
        if (targetPercentage != that.targetPercentage) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    public String getCsvRepresentation(){
        return name + "," + targetPercentage + "%," + getEstimatedAssignmentsPerLesson() + "," + estimatedLessonCount;
    }

    @Override
    public String getDisplayName() {
        return name + " - " + targetPercentage + "%";
    }

    @Override
    public int getId() {
        return id;
    }

    public enum EstimationMode {
        user,
        mean,
        best,
        worst,
        undefined
    }
}
