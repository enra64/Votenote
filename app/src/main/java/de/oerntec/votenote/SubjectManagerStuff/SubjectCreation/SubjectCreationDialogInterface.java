package de.oerntec.votenote.SubjectManagerStuff.SubjectCreation;

import de.oerntec.votenote.Database.Pojo.AdmissionCounter;
import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMeta;

/**
 * Interface to be able to give results from dedicated dialog classes
 */
public interface SubjectCreationDialogInterface {
    void admissionCounterFinished(int id, boolean isNew);

    void deleteAdmissionCounter(int itemId);

    void admissionPercentageFinished(int id, boolean isNew);

    void deleteAdmissionPercentage(int itemId);

    void dialogClosed();
}
