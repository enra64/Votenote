package de.oerntec.votenote.subject_management.subject_creation;

/**
 * Interface to be able to give results from dedicated dialog classes
 */
interface SubjectCreationDialogInterface {
    void admissionCounterFinished(int id, boolean isNew);

    void deleteAdmissionCounter(int itemId);

    void admissionPercentageFinished(int id, boolean isNew);

    void deleteAdmissionPercentage(int itemId);

    void dialogClosed();
}
