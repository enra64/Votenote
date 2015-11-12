package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import de.oerntec.votenote.Database.AdmissionCounter;
import de.oerntec.votenote.Database.AdmissionPercentageMeta;

/**
 * Interface to be able to give results from dedicated dialog classes
 */
public interface SubjectCreationDialogInterface {
    void admissionCounterFinished(int id, boolean isNew);

    void deleteAdmissionCounter(AdmissionCounter deleted);

    void admissionPercentageFinished(int id, boolean isNew);

    void deleteAdmissionPercentage(AdmissionPercentageMeta deleted);
}
