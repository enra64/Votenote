package de.oerntec.votenote.database.pojo;

public class Lesson {
    public final int admissionPercentageMetaId;
    public final int finishedAssignments;
    public final int availableAssignments;
    public int id;
    public int lessonId;

    public Lesson(int id, int admissionPercentageMetaId, int lessonId, int finishedAssignments, int availableAssignments) {
        this.id = id;
        this.admissionPercentageMetaId = admissionPercentageMetaId;
        this.lessonId = lessonId;
        this.finishedAssignments = finishedAssignments;
        this.availableAssignments = availableAssignments;
    }

    public Lesson(int admissionPercentageMetaId, int lessonId, int finishedAssignments, int availableAssignments) {
        this.id = -1;
        this.admissionPercentageMetaId = admissionPercentageMetaId;
        this.lessonId = lessonId;
        this.finishedAssignments = finishedAssignments;
        this.availableAssignments = availableAssignments;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lesson that = (Lesson) o;

        if (id != that.id) return false;
        if (admissionPercentageMetaId != that.admissionPercentageMetaId) return false;
        if (lessonId != that.lessonId) return false;
        if (finishedAssignments != that.finishedAssignments) return false;
        return availableAssignments == that.availableAssignments;
    }

    public String getCsvRepresentation(){
        return lessonId+","+finishedAssignments+","+availableAssignments;
    }
}
