package de.oerntec.votenote.Database.Pojo.PercentageMetaStuff;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;

/**
 * This class holds all information regarding the estimation concerning a single admission percentage
 * counter.
 */
public class SubjectInfoCalculationResult {
    public float   numberOfPastLessons,
            numberOfFutureLessons,
            numberOfPastAvailableAssignments,
            numberOfFinishedAssignments;

    public EstimationModeDependentResults   bestEstimation,
                                            worstEstimation,
                                            meanEstimation,
                                            userEstimation;
}
