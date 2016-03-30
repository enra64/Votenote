package de.oerntec.votenote.database.pojo.percentagetracker;

import java.util.List;

import de.oerntec.votenote.database.pojo.Lesson;

import static de.oerntec.votenote.database.pojo.percentagetracker.PercentageTrackerPojo.EstimationMode;

/**
 * This class pulls the calculation functions from the old admission percentage meta object into a
 * static helper to declutter the pojo and make the calculation functions easier to read and check.
 */
public class PercentageTrackerCalculator {
    public static PercentageTrackerCalculationResult calculateAll(PercentageTrackerPojo metaPojo) {
        if (!metaPojo.mDataLoaded)
            throw new AssertionError("pojo has not loaded data");

        PercentageTrackerCalculationResult result = new PercentageTrackerCalculationResult();

        result.numberOfPastLessons = metaPojo.lessonCount();
        result.numberOfFutureLessons = metaPojo.userLessonCountEstimation - result.numberOfPastLessons;

        result.numberOfPastAvailableAssignments = getNumberOfPastAvailableAssignments(metaPojo.mDataList);
        result.numberOfFinishedAssignments = getNumberOfPastFinishedAssignments(metaPojo.mDataList);

        result.userEstimation = calculateEstimationModeDependentResults(EstimationMode.user, result, metaPojo);
        result.bestEstimation = calculateEstimationModeDependentResults(EstimationMode.best, result, metaPojo);
        result.meanEstimation = calculateEstimationModeDependentResults(EstimationMode.mean, result, metaPojo);
        result.worstEstimation = calculateEstimationModeDependentResults(EstimationMode.worst, result, metaPojo);

        return result;
    }

    private static EstimationModeDependentResults calculateEstimationModeDependentResults(EstimationMode mode,
                                                                                          PercentageTrackerCalculationResult independentResults,
                                                                                          PercentageTrackerPojo item) {
        EstimationModeDependentResults results = new EstimationModeDependentResults();
        results.numberOfAssignmentsEstimatedPerLesson = getEstimatedAssignmentsPerLesson(item, mode);

        float estimatedNumberOfFutureAvailableAssignments =
                getEstimatedAssignmentsPerLesson(item, mode) * independentResults.numberOfFutureLessons;

        results.numberOfEstimatedOverallAssignments = estimatedNumberOfFutureAvailableAssignments +
                independentResults.numberOfPastAvailableAssignments;

        //default to baseline target percentage
        float localTargetPercentage = item.baselineTargetPercentage;
        results.bonusReachable = false;

        //decide whether to use the bonus or the baseline required percentage
        if (item.bonusTargetPercentageEnabled) {
            float neededAssignmentsPerLessonForBonus = calcNeededAssignmentsPerLesson(
                    item,
                    item.bonusTargetPercentage,
                    independentResults.numberOfFutureLessons);

            // bonus reachable!
            if (neededAssignmentsPerLessonForBonus < getEstimatedAssignmentsPerLesson(item, mode)){
                results.bonusReachable = true;
                localTargetPercentage = item.bonusTargetPercentage;
            }
        }

        results.numberOfNeededAssignments = (results.numberOfEstimatedOverallAssignments * localTargetPercentage) / 100f;
        results.numberOfRemainingNeededAssignments = results.numberOfNeededAssignments - independentResults.numberOfFinishedAssignments;
        results.numberOfAssignmentsNeededPerLesson = results.numberOfRemainingNeededAssignments / independentResults.numberOfFutureLessons;

        return results;
    }

    private static int getNumberOfPastFinishedAssignments(List<Lesson> mDataList) {
        int finishedAssignments = 0;
        for (Lesson d : mDataList)
            finishedAssignments += d.finishedAssignments;
        return finishedAssignments;
    }

    private static int getNumberOfPastAvailableAssignments(List<Lesson> mDataList) {
        int availableAssignments = 0;
        for (Lesson d : mDataList)
            availableAssignments += d.availableAssignments;
        return availableAssignments;
    }

    private static int getMinimumPastAvailableAssignments(List<Lesson> mDataList) {
        int minAvailableAssignments = Integer.MAX_VALUE;
        for (Lesson d : mDataList)
            minAvailableAssignments = d.availableAssignments < minAvailableAssignments ? d.availableAssignments : minAvailableAssignments;
        return minAvailableAssignments;
    }

    private static int getMaximumPastAvailableAssignments(List<Lesson> mDataList) {
        int maxAvailableAssignments = 0;
        for (Lesson d : mDataList)
            maxAvailableAssignments = d.availableAssignments > maxAvailableAssignments ? d.availableAssignments : maxAvailableAssignments;
        return maxAvailableAssignments;
    }

    /**
     * This just calculates how many assignments are needed per lesson, but does not write it anywhere,
     * so we can check whether the bonus target percentage may still be hit
     */
    public static float calcNeededAssignmentsPerLesson(PercentageTrackerPojo item, float target, float numberOfFutureLessons) {
        float numberOfNeededAssignments = (item.userAssignmentsPerLessonEstimation * target) / 100f;
        float remainingNeededAssignments = numberOfNeededAssignments - getNumberOfPastFinishedAssignments(item.mDataList);
        return remainingNeededAssignments / numberOfFutureLessons;
    }

    /**
     * This estimates a lesson count by returning the number of assignments per lesson estimated
     * accordingly to the item estimation mode
     */
    public static float getEstimatedAssignmentsPerLesson(PercentageTrackerPojo item) {
        return getEstimatedAssignmentsPerLesson(item, item.estimationMode);
    }

    /**
     * This estimates a lesson count by returning the number of assignments per lesson estimated
     * accordingly to the given estimation mode
     */
    private static float getEstimatedAssignmentsPerLesson(PercentageTrackerPojo item, EstimationMode estimationMode) {
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