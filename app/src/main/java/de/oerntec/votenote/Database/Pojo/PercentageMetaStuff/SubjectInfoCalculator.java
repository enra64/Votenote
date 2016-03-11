package de.oerntec.votenote.Database.Pojo.PercentageMetaStuff;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageData;

import static de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.AdmissionPercentageMetaNew.*;
import static de.oerntec.votenote.Database.Pojo.PercentageMetaStuff.SubjectInfoCalculationResult.*;

/**
 * This class pulls the calculation functions from the old admission percentage meta object into a
 * static helper to declutter the pojo and make the calculation functions easier to read and check.
 */
public class SubjectInfoCalculator {
    public static SubjectInfoCalculationResult calculateAll(AdmissionPercentageMetaNew item){
        if (!item.mDataLoaded)
            throw new AssertionError("pojo has not loaded data");

        SubjectInfoCalculationResult result = new SubjectInfoCalculationResult();

        result.numberOfPastLessons = item.lessonCount();
        result.numberOfFutureLessons = item.userLessonCountEstimation - result.numberOfPastLessons;

        result.numberOfPastAvailableAssignments = getNumberOfPastAvailableAssignments(item.mDataList);
        result.numberOfFinishedAssignments = getNumberOfPastFinishedAssignments(item.mDataList);

        result.userEstimation = calculateEstimationModeDependentResults(EstimationMode.user, result, item);
        result.bestEstimation = calculateEstimationModeDependentResults(EstimationMode.best, result, item);
        result.meanEstimation = calculateEstimationModeDependentResults(EstimationMode.mean, result, item);
        result.worstEstimation = calculateEstimationModeDependentResults(EstimationMode.worst, result, item);

        return result;
    }

    private static EstimationModeDependentResults calculateEstimationModeDependentResults(EstimationMode mode,
                                                                                          SubjectInfoCalculationResult independentResults,
                                                                                          AdmissionPercentageMetaNew item){
        EstimationModeDependentResults results = new EstimationModeDependentResults();
        results.estimatedAssignmentsPerLesson = getEstimatedAssignmentsPerLesson(item, mode);

        results.scheduledNumberOfAssignments = getEstimatedAssignmentsPerLesson(item) * independentResults.numberOfFutureLessons +
                independentResults.numberOfPastAvailableAssignments;

        //default to baseline target percentage
        float localTargetPercentage = item.baselineTargetPercentage;
        results.bonusReachable = false;

        //decide whether to use the bonus or the baseline required percentage
        if (item.bonusTargetPercentageEnabled) {
            float neededAssignmentsPerLessonForBonus = calcNeededAssignmentsPerLesson(item,
                    item.bonusTargetPercentage,
                    independentResults.numberOfFutureLessons);

            // bonus reachable!
            if (neededAssignmentsPerLessonForBonus < getEstimatedAssignmentsPerLesson(item, mode)){
                results.bonusReachable = true;
                localTargetPercentage = item.bonusTargetPercentage;
            }
        }

        results.numberOfNeededAssignments = (results.scheduledNumberOfAssignments * localTargetPercentage) / 100f;
        results.numberOfRemainingNeededAssignments = results.numberOfNeededAssignments - independentResults.numberOfFinishedAssignments;
        results.numberOfAssignmentsNeededPerLesson = results.numberOfRemainingNeededAssignments / independentResults.numberOfFutureLessons;

        return results;
    }

    private static int getNumberOfPastFinishedAssignments(List<AdmissionPercentageData> mDataList) {
        int finishedAssignments = 0;
        for(AdmissionPercentageData d : mDataList)
            finishedAssignments += d.finishedAssignments;
        return finishedAssignments;
    }

    private static int getNumberOfPastAvailableAssignments(List<AdmissionPercentageData> mDataList) {
        int availableAssignments = 0;
        for (AdmissionPercentageData d : mDataList)
            availableAssignments += d.availableAssignments;
        return availableAssignments;
    }

    private static int getMinimumPastAvailableAssignments(List<AdmissionPercentageData> mDataList) {
        int minAssignments = Integer.MAX_VALUE;
        for (AdmissionPercentageData d : mDataList)
            minAssignments = d.availableAssignments < minAssignments ? d.availableAssignments : minAssignments;
        return minAssignments;
    }

    private static int getMaximumPastAvailableAssignments(List<AdmissionPercentageData> mDataList) {
        int maxAssignments = 0;
        for (AdmissionPercentageData d : mDataList)
            maxAssignments = d.availableAssignments > maxAssignments ? d.availableAssignments : maxAssignments;
        return maxAssignments;
    }

    /**
     * This just calculates how many assignments are needed per lesson, but does not write it anywhere,
     * so we can check whether the bonus target percentage may still be hit
     */
    public static float calcNeededAssignmentsPerLesson(AdmissionPercentageMetaNew item, float target, float numberOfFutureLessons) {
        float numberOfNeededAssignments = (item.userAssignmentsPerLessonEstimation * target) / 100f;
        float remainingNeededAssignments = numberOfNeededAssignments - getNumberOfPastFinishedAssignments(item.mDataList);
        return remainingNeededAssignments / numberOfFutureLessons;
    }

    /**
     * This estimates a lesson count by returning the number of assignments per lesson estimated
     * accordingly to the item estimation mode
     */
    public static float getEstimatedAssignmentsPerLesson(AdmissionPercentageMetaNew item) {
        return getEstimatedAssignmentsPerLesson(item, item.estimationMode);
    }

    /**
     * This estimates a lesson count by returning the number of assignments per lesson estimated
     * accordingly to the given estimation mode
     */
    private static float getEstimatedAssignmentsPerLesson(AdmissionPercentageMetaNew item, EstimationMode estimationMode) {
        switch (estimationMode) {
            case user:
                return item.userAssignmentsPerLessonEstimation;
            case mean:
                if (!item.hasLessons())
                    return item.userAssignmentsPerLessonEstimation;
                return (float) getNumberOfPastAvailableAssignments(item.mDataList) / (float) item.lessonCount();
            case best:
                return getMinimumPastAvailableAssignments(item.mDataList);
            case worst:
                return getMaximumPastAvailableAssignments(item.mDataList);
            default:
            case undefined:
                throw new AssertionError("undefined estimation mode!");
        }
    }
}