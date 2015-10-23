package de.oerntec.votenote.Database;

/**
 * POJO for representing an admission counter
 */
public class AdmissionCounter {
    public int id, subjectId, counterName, currentValue, targetValue;

    public AdmissionCounter(int id, int subjectId, int counterName, int currentValue, int targetValue) {
        this.id = id;
        this.subjectId = subjectId;
        this.counterName = counterName;
        this.currentValue = currentValue;
        this.targetValue = targetValue;
    }
}