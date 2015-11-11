package de.oerntec.votenote.Database;

public class AdmissionPercentageData {
    int id, subjectId, lessonId, finishedAssignments, availableAssignments;

    public AdmissionPercentageData(int id, int subjectId, int lessonId, int finishedAssignments, int availableAssignments) {
        this.id = id;
        this.subjectId = subjectId;
        this.lessonId = lessonId;
        this.finishedAssignments = finishedAssignments;
        this.availableAssignments = availableAssignments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionPercentageData that = (AdmissionPercentageData) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (lessonId != that.lessonId) return false;
        if (finishedAssignments != that.finishedAssignments) return false;
        return availableAssignments == that.availableAssignments;

    }
}
