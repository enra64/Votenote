package de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff;

/**
 * This class holds all information for an admission percentage counter estimation that depends on
 * which mode is used.
 */
public class EstimationModeDependentResults{
        public float    numberOfEstimatedOverallAssignments,
                        numberOfAssignmentsEstimatedPerLesson,
                        numberOfNeededAssignments,
                        numberOfRemainingNeededAssignments,
                        numberOfAssignmentsNeededPerLesson;
        public boolean bonusReachable;
    }