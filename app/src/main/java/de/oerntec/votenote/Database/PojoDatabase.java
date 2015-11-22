package de.oerntec.votenote.Database;

import java.util.List;

public interface PojoDatabase<T extends NameAndIdPojo> {
    //add, get, delete, getForSubject
    void changeItem(T newItem);
    void deleteItem(int id);
    void createSavepoint(String id);
    void rollbackToSavepoint(String id);
    int getCount();

    int addItem(T newCounter);
    List<T> getItemsForSubject(int subjectId);
    T getItem(int id);
}