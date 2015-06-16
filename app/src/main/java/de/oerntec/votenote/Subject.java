package de.oerntec.votenote;

public class Subject {
    public String id,
            subjectName,
            subjectMinimumVotePercentage,
            subjectCurrentPresentationPoints,
            subjectScheduledLessonCount,
            subjectScheduledAssignmentsPerLesson,
            subjectWantedPresentationPoints;

    public Subject(String id, String subjectName, String subjectMinimumVotePercentage, String subjectCurrentPresentationPoints,
                   String subjectScheduledLessonCount, String subjectScheduledAssignmentsPerLesson, String subjectWantedPresentationPoints) {
        this.id = id;
        this.subjectName = subjectName;
        this.subjectMinimumVotePercentage = subjectMinimumVotePercentage;
        this.subjectCurrentPresentationPoints = subjectCurrentPresentationPoints;
        this.subjectScheduledLessonCount = subjectScheduledLessonCount;
        this.subjectScheduledAssignmentsPerLesson = subjectScheduledAssignmentsPerLesson;
        this.subjectWantedPresentationPoints = subjectWantedPresentationPoints;
    }
}
