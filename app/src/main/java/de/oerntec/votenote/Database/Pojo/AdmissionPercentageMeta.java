package de.oerntec.votenote.Database.Pojo;

import java.util.List;

import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMeta implements NameAndIdPojo {
    public int id, subjectId, estimatedLessonCount, baselineTargetPercentage, userAssignmentsPerLessonEstimation, bonusTargetPercentage;
    public boolean bonusTargetPercentageEnabled;

    public String name;
    public List<AdmissionPercentageData> mDataList;
    public EstimationMode estimationMode = EstimationMode.undefined;
    private boolean mDataLoaded = false;
    private boolean mDataCalculated = false;
    private float numberOfLessonsLeft, scheduledNumberOfAssignments, neededAssignmentsPerUebung, numberOfNeededAssignments,
            remainingNeededAssignments, numberOfFinishedAssignments, numberOfElapsedLessons;

    public AdmissionPercentageMeta(int id, int subjectId, int estimatedAssignmentsPerLesson,
                                   int estimatedLessonCount, int baselineTargetPercentage,
                                   String name, String mode,
                                   int bonusTargetPercentage, boolean bonusTargetPercentageEnabled) {
        this.id = id;
        this.subjectId = subjectId;
        this.estimatedLessonCount = estimatedLessonCount;
        this.userAssignmentsPerLessonEstimation = estimatedAssignmentsPerLesson;
        this.baselineTargetPercentage = baselineTargetPercentage;
        this.name = name;
        this.bonusTargetPercentageEnabled = bonusTargetPercentageEnabled;
        this.bonusTargetPercentage = bonusTargetPercentage;

        estimationMode = EstimationMode.valueOf(mode);
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
        numberOfElapsedLessons = mDataList.size();
        numberOfLessonsLeft = estimatedLessonCount - numberOfElapsedLessons;

        scheduledNumberOfAssignments = getEstimatedAssignmentsPerLesson() * numberOfLessonsLeft +
                getNumberOfAvailableAssignmentsEnteredSoFar();

        numberOfFinishedAssignments = getFinishedAssignmentsCount();

        float localTargetPercentage;

        //decide whether to use the bonus or the baseline required percentage
        if (bonusTargetPercentageEnabled) {
            float neededAssignmentsForBonus = calcNeededAssignmentsPerLesson(bonusTargetPercentage);

            // if we need to do more exercises per lesson than we estimate there are per lesson,
            // use the baseline target percentage as the target.
            if (neededAssignmentsForBonus > getEstimatedAssignmentsPerLesson())
                localTargetPercentage = baselineTargetPercentage;
                // if not, we can still achieve the bonus
                // points, so we use the bonus target percentage as the target.
            else
                localTargetPercentage = bonusTargetPercentage;
        }
        //bonus percentage is disabled, so use the baseline target percentage
        else
            localTargetPercentage = baselineTargetPercentage;


        numberOfNeededAssignments = (scheduledNumberOfAssignments * localTargetPercentage) / 100f;
        remainingNeededAssignments = numberOfNeededAssignments - numberOfFinishedAssignments;
        neededAssignmentsPerUebung = remainingNeededAssignments / numberOfLessonsLeft;

        mDataCalculated = true;
    }

    /**
     * This just calculates how many assignments are needed per lesson, but does not write it anywhere,
     * so we can check whether the bonus target percentage may still be hit
     */
    private float calcNeededAssignmentsPerLesson(float target) {
        float numberOfNeededAssignments = (scheduledNumberOfAssignments * (float) target) / 100f;
        float remainingNeededAssignments = numberOfNeededAssignments - numberOfFinishedAssignments;
        return remainingNeededAssignments / numberOfLessonsLeft;
    }

    /**
     * This estimates a lesson count by returning one the best/worst/mean/given number of remaining assignments
     */
    public float getEstimatedAssignmentsPerLesson() {
        switch (estimationMode) {
            case user:
                return userAssignmentsPerLessonEstimation;
            case mean:
                if (mDataList.size() == 0)
                    return userAssignmentsPerLessonEstimation;
                return (float) getNumberOfAvailableAssignmentsEnteredSoFar() / (float) mDataList.size();
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
        return estimationMode.name();
    }

    public int getFinishedAssignmentsCount() {
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        int finishedAssignments = 0;
        for(AdmissionPercentageData d : mDataList)
            finishedAssignments += d.finishedAssignments;
        return finishedAssignments;
    }

    public int getNumberOfAvailableAssignmentsEnteredSoFar() {
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
        if (baselineTargetPercentage != that.baselineTargetPercentage) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    public String getCsvRepresentation(){
        return name + "," + baselineTargetPercentage + "%," + getEstimatedAssignmentsPerLesson() + "," + estimatedLessonCount;
    }

    @Override
    public String getDisplayName() {
        return name + " - " + baselineTargetPercentage + "%";
    }

    @Override
    public int getId() {
        return id;
    }


    //BEWARE: mEstimationModeSeekbar.setMax(EstimationMode.values().length - 1); in setValuesForViews
    // in AdmissionPercentageFragment relies on the undefined state to exist and be last!
    public enum EstimationMode {
        user,
        mean,
        best,
        worst,
        undefined
    }
}
