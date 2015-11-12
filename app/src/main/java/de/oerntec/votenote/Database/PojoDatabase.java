package de.oerntec.votenote.Database;

import java.util.List;

public interface PojoDatabase<T extends NameAndIdPojo> {
    //add, get, delete, getForSubject
    void changeItem(T newItem);

    void deleteItem(int id);

    void createSavepoint(String id);

    void rollbackToSavepoint(String id);

    int addItem(T newCounter);

    int getCount();

    List<T> getItemsForSubject(int subjectId);

    T getItem(int id);
}