package de.oerntec.votenote.Database.Pojo.PercentageMetaStuff;

import java.util.List;

import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMeta implements NameAndIdPojo {
    /**
     * Database ID. Given in constructor.
     */
    public int id;

    /**
     * Subject ID. Not equal to Database id. Given in constructor.
     */
    public int subjectId;

    /**
     * How many lessosns does the user think will this percentage counter have?
     */
    public int userLessonCountEstimation;

    /**
     * This is the always existent baseline target percentage. If a bonus percentage is given,
     * the user should aim for that.
     */
    public int baselineTargetPercentage;

    /**
     * How many assignments does the user think are available in each lesson.
     */
    public int userAssignmentsPerLessonEstimation;

    /**
     * If bonusTargetPercentageEnabled is true, reaching this percentage will give the user a bonus
     */
    public int bonusTargetPercentage;

    /**
     * Is there a bonus percentage?
     */
    public boolean bonusTargetPercentageEnabled;

    /**
     * Admission Counter Percentage name
     */
    public String name;

    /**
     * the estimation mode the user has set for this admission percentage counter
     */
    public EstimationMode estimationMode = EstimationMode.undefined;

    /**
     * If the data has been loaded, this list contains all lessons in this apm, simplifying data
     * calculation
     */
    public List<AdmissionPercentageData> mDataList;

    /**
     * Has the lesson list been populated?
     */
    boolean mDataLoaded = false;

    public AdmissionPercentageMeta(int id, int subjectId, int estimatedAssignmentsPerLesson,
                                   int userLessonCountEstimation, int baselineTargetPercentage,
                                   String name, String mode,
                                   int bonusTargetPercentage, boolean bonusTargetPercentageEnabled) {
        this.id = id;
        this.subjectId = subjectId;
        this.userLessonCountEstimation = userLessonCountEstimation;
        this.userAssignmentsPerLessonEstimation = estimatedAssignmentsPerLesson;
        this.baselineTargetPercentage = baselineTargetPercentage;
        this.name = name;
        this.bonusTargetPercentageEnabled = bonusTargetPercentageEnabled;
        this.bonusTargetPercentage = bonusTargetPercentage;

        this.estimationMode = EstimationMode.valueOf(mode);
    }

    public void loadData(DBAdmissionPercentageData dataDb, boolean latestLessonFirst){
        mDataList = dataDb.getItemsForMetaId(id, latestLessonFirst);
        mDataLoaded = true;
    }

    public boolean hasLessons(){
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        return mDataList.size() > 0;
    }

    public int lessonCount(){
        if (!mDataLoaded) throw new AssertionError("pojo has not loaded data");
        return mDataList.size();
    }

    public String getEstimationModeAsString() {
        return estimationMode.name();
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
        if (userLessonCountEstimation != that.userLessonCountEstimation) return false;
        if (baselineTargetPercentage != that.baselineTargetPercentage) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public String getDisplayName() {
        return name + " - " + baselineTargetPercentage + "%";
    }

    @Override
    public int getId() {
        return id;
    }

    public String getCsvRepresentation() {
        return name + "," +
                baselineTargetPercentage + "%," +
                bonusTargetPercentage + "%," +
                (bonusTargetPercentageEnabled ? "true" : "false") + "," +
                estimationMode.name() + "," +
                userAssignmentsPerLessonEstimation + "," +
                userLessonCountEstimation + "";
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
