package de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff;

import java.util.List;

import de.oerntec.votenote.Database.NameAndIdPojo;
import de.oerntec.votenote.Database.Pojo.Lesson;
import de.oerntec.votenote.Database.TableHelpers.DBLessons;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMetaPojo implements NameAndIdPojo {
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
     * Are recurring notifications dis- or enabled?
     */
    public boolean notificationEnabled;

    /**
     * This String contains the recurrance data according to the dow/time picker library used
     */
    public String notificationRecurrenceString;

    /**
     * If the data has been loaded, this list contains all lessons in this apm, simplifying data
     * calculation
     */
    public List<Lesson> mDataList;

    /**
     * Has the lesson list been populated?
     */
    boolean mDataLoaded = false;

    public AdmissionPercentageMetaPojo(int id,
                                       int subjectId,
                                       int userAssignmentsPerLessonEstimation,
                                       int userLessonCountEstimation,
                                       int baselineTargetPercentage,
                                       String name,
                                       String mode,
                                       int bonusTargetPercentage,
                                       boolean bonusTargetPercentageEnabled,
                                       String notificationRecurrenceString,
                                       boolean notificationEnabled) {
        this.id = id;
        this.subjectId = subjectId;
        this.userLessonCountEstimation = userLessonCountEstimation;
        this.userAssignmentsPerLessonEstimation = userAssignmentsPerLessonEstimation;
        this.baselineTargetPercentage = baselineTargetPercentage;
        this.name = name;

        this.bonusTargetPercentageEnabled = bonusTargetPercentageEnabled;
        this.bonusTargetPercentage = bonusTargetPercentage;

        this.notificationRecurrenceString = notificationRecurrenceString;
        this.notificationEnabled = notificationEnabled;

        this.estimationMode = EstimationMode.valueOf(mode);
    }

    public void loadData(DBLessons dataDb, boolean latestLessonFirst) {
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
        for (Lesson d : mDataList) {
            availableAssignments += d.availableAssignments;
            finishedAssignments += d.finishedAssignments;
        }

        availableAssignments += addToAvailable;
        finishedAssignments += addToFinished;

        //safeguard against weird numbers possibly produced by adding the values
        // i have no idea why that should happen
        finishedAssignments = finishedAssignments < 0 ? 0 : finishedAssignments;
        availableAssignments = availableAssignments < 0 ? 0 : availableAssignments;

        float avg = ((float) finishedAssignments / (float) availableAssignments) * 100.f;
        //safeguard against division by zero etc.
        return Float.isInfinite(avg) || Float.isNaN(avg) ? -1 : avg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionPercentageMetaPojo that = (AdmissionPercentageMetaPojo) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (userLessonCountEstimation != that.userLessonCountEstimation) return false;
        if (baselineTargetPercentage != that.baselineTargetPercentage) return false;
        if (userAssignmentsPerLessonEstimation != that.userAssignmentsPerLessonEstimation)
            return false;
        if (bonusTargetPercentage != that.bonusTargetPercentage) return false;
        if (bonusTargetPercentageEnabled != that.bonusTargetPercentageEnabled) return false;
        if (notificationEnabled != that.notificationEnabled) return false;
        if (mDataLoaded != that.mDataLoaded) return false;
        if (!name.equals(that.name)) return false;
        if (estimationMode != that.estimationMode) return false;
        if (!notificationRecurrenceString.equals(that.notificationRecurrenceString)) return false;
        return mDataList != null ? mDataList.equals(that.mDataList) : that.mDataList == null;
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

    // BEWARE: mEstimationModeSeekbar.setMax(EstimationMode.values().length - 2); in setValuesForViews
    // in AdmissionPercentageFragment relies on the undefined state to exist and be last!
    // also in overview fragment, just dont change this
    public enum EstimationMode {
        user,
        mean,
        best,
        worst,
        undefined
    }
}
