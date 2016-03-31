package de.oerntec.votenote.database.pojo;

import de.oerntec.votenote.database.NameAndIdPojo;

/**
 * POJO for representing an admission counter
 */
public class AdmissionCounter implements Cloneable, NameAndIdPojo {
    public final int id;
    public final int subjectId;

    public final int targetValue;
    public final String name;
    public int currentValue;

    public AdmissionCounter(int id, int subjectId, String name, int currentValue, int targetValue) {
        this.id = id;
        this.subjectId = subjectId;
        this.name = name;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
    }

    public String getCsvRepresentation(){
        return currentValue+","+targetValue;
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

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AdmissionCounter that = (AdmissionCounter) o;

        if (id != that.id) return false;
        if (subjectId != that.subjectId) return false;
        if (currentValue != that.currentValue) return false;
        if (targetValue != that.targetValue) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public String getDisplayName() {
        return name + " - " + targetValue;
    }

    @Override
    public int getId() {
        return id;
    }
}