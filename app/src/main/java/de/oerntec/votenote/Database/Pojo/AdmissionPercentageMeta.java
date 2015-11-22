package de.oerntec.votenote.Database.Pojo;

import de.oerntec.votenote.Database.NameAndIdPojo;

/**
 * POJO class for representing the metadata available about an admission percentage counter
 */
public class AdmissionPercentageMeta implements NameAndIdPojo {
    public int id, subjectId, estimatedAssignmentsPerLesson, estimatedLessonCount, targetPercentage;
    public String name;

    public AdmissionPercentageMeta(int id, int subjectId, int estimatedAssignmentsPerLesson, int estimatedLessonCount, int targetPercentage, String name) {
        this.id = id;
        this.subjectId = subjectId;
        this.estimatedAssignmentsPerLesson = estimatedAssignmentsPerLesson;
        this.estimatedLessonCount = estimatedLessonCount;
        this.targetPercentage = targetPercentage;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionPercentageMeta that = (AdmissionPercentageMeta) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (estimatedAssignmentsPerLesson != that.estimatedAssignmentsPerLesson) return false;
        if (estimatedLessonCount != that.estimatedLessonCount) return false;
        if (targetPercentage != that.targetPercentage) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public String getDisplayName() {
        return name + " - " + targetPercentage + "%";
    }

    @Override
    public int getId() {
        return id;
    }
}
