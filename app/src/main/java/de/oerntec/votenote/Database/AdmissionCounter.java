package de.oerntec.votenote.Database;

/**
 * POJO for representing an admission counter
 */
public class AdmissionCounter {
    public int id, subjectId, currentValue, targetValue;
    public String counterName;

    public AdmissionCounter(int id, int subjectId, String counterName, int currentValue, int targetValue) {
        this.id = id;
        this.subjectId = subjectId;
        this.counterName = counterName;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
    }
}