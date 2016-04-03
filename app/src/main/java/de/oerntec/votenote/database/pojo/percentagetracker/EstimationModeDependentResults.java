package de.oerntec.votenote.database.pojo.percentagetracker;

/**
 * This class holds all information for an admission percentage counter estimation that depends on
 * which mode is used.
 */
public class EstimationModeDependentResults {
    public float numberOfEstimatedOverallAvailableAssignments,
            numberOfAssignmentsEstimatedPerLesson,
            numberOfNeededAssignments,
            numberOfRemainingNeededAssignments,
            numberOfAssignmentsNeededPerLesson,
            numberOfEstimatedFutureAvailableAssignments;
    public boolean bonusReachable;
}