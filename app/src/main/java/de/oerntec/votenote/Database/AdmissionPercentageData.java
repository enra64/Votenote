package de.oerntec.votenote.Database;

/**
 * Created by Arne on 23-Oct-15.
 */
public class AdmissionPercentageData {
    int id, subjectId, lessonId, finishedAssignments, availableAssignments;

    public AdmissionPercentageData(int id, int subjectId, int lessonId, int finishedAssignments, int availableAssignments) {
        this.id = id;
        this.subjectId = subjectId;
        this.lessonId = lessonId;
        this.finishedAssignments = finishedAssignments;
        this.availableAssignments = availableAssignments;
    }
}
