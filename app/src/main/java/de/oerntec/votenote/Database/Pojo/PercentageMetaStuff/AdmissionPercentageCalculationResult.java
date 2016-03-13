package de.oerntec.votenote.Database.Pojo.PercentageMetaStuff;

/**
 * This class holds all information regarding the estimation concerning a single admission percentage
 * counter.
 */
public class AdmissionPercentageCalculationResult {
    public float   numberOfPastLessons,
            numberOfFutureLessons,
            numberOfPastAvailableAssignments,
            numberOfFinishedAssignments;

    public EstimationModeDependentResults   bestEstimation,
                                            worstEstimation,
                                            meanEstimation,
                                            userEstimation;

    public EstimationModeDependentResults getEstimationDependentResults(AdmissionPercentageMeta.EstimationMode mode){
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
