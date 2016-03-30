package de.oerntec.votenote.database.pojo.percentagetracker;

/**
 * This class holds all information regarding the estimation concerning a single admission percentage
 * counter.
 */
public class PercentageTrackerCalculationResult {
    public float   numberOfPastLessons,
            numberOfFutureLessons,
            numberOfPastAvailableAssignments,
            numberOfFinishedAssignments;

    public EstimationModeDependentResults   bestEstimation,
                                            worstEstimation,
                                            meanEstimation,
                                            userEstimation;

    public EstimationModeDependentResults getEstimationDependentResults(PercentageTrackerPojo.EstimationMode mode) {
        switch (mode){
            case user:
                return userEstimation;
            case mean:
                return meanEstimation;
            case best:
                return bestEstimation;
            case worst:
                return worstEstimation;
            default:
            case undefined:
                throw new AssertionError("unknown estimation mode requested");
        }
    }
}
