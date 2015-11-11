package de.oerntec.votenote.SubjectManagerStuff;

import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Database.AdmissionPercentageMeta;

/**
 * Interface to be able to give results from dedicated dialog classes
 */
public interface SubjectCreationDialogInterface {
    void admissionCounterFinished(AdmissionCounter result);
    void admissionPercentageFinished(AdmissionPercentageMeta result);
}
