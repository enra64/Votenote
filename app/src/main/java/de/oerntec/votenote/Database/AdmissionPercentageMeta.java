package de.oerntec.votenote.Database;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMeta {
    int id, subjectId, estimatedAssignmentsPerLesson, estimatedLessonCount, targetPercentage;
    String name;

    public AdmissionPercentageMeta(int id, int subjectId, int estimatedAssignmentsPerLesson, int estimatedLessonCount, int targetPercentage, String name) {
        this.id = id;
        this.subjectId = subjectId;
        this.estimatedAssignmentsPerLesson = estimatedAssignmentsPerLesson;
        this.estimatedLessonCount = estimatedLessonCount;
        this.targetPercentage = targetPercentage;
        this.name = name;
    }
}
