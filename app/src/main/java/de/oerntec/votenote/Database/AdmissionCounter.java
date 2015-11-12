package de.oerntec.votenote.Database;

/**
 * POJO for representing an admission counter
 */
public class AdmissionCounter implements Cloneable, NameAndIdPojo {
    public int id, subjectId, currentValue, targetValue;
    public String counterName;

    public AdmissionCounter(int id, int subjectId, String counterName, int currentValue, int targetValue) {
        this.id = id;
        this.subjectId = subjectId;
        this.counterName = counterName;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
    }

    @Override
    public AdmissionCounter clone() {
        try {
            return (AdmissionCounter) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        throw new AssertionError("couldnt clone");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionCounter that = (AdmissionCounter) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (currentValue != that.currentValue) return false;
        if (targetValue != that.targetValue) return false;
        return !(counterName != null ? !counterName.equals(that.counterName) : that.counterName != null);

    }

    @Override
    public String getDisplayName() {
        return counterName + " - " + targetValue;
    }

    @Override
    public int getId() {
        return id;
    }
}