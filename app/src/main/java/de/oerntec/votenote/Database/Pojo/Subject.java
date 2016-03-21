/*
* VoteNote, an android app for organising the assignments you mark as done for uni.
* Copyright (C) 2015 Arne Herdick
*
* This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
* */
package de.oerntec.votenote.Database.Pojo;

import java.util.List;

import de.oerntec.votenote.Database.Pojo.AdmissionPercentageMetaStuff.AdmissionPercentageMetaPojo;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionCounters;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageData;
import de.oerntec.votenote.Database.TableHelpers.DBAdmissionPercentageMeta;

public class Subject {
    public String name;
    public int id;
    public List<AdmissionCounter> admissionCounterList = null;
    public List<AdmissionPercentageMetaPojo> admissionPercentageMetaPojoList = null;


    public Subject(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public Subject(String name, int id, List<AdmissionCounter> admissionCounterList, List<AdmissionPercentageMetaPojo> admissionPercentageMetaPojoList) {
        this.name = name;
        this.id = id;
        this.admissionCounterList = admissionCounterList;
        this.admissionPercentageMetaPojoList = admissionPercentageMetaPojoList;
    }

    /**
     * fill the lists that are usually empty containing all other pojos belonging to this subject
     */
    public void loadAllData(DBAdmissionCounters counterDb, DBAdmissionPercentageData dataDb, DBAdmissionPercentageMeta metaDb, boolean latestLessonFirst){
        admissionCounterList = counterDb.getItemsForSubject(id);
        admissionPercentageMetaPojoList = metaDb.getItemsForSubject(id);
        for (AdmissionPercentageMetaPojo meta : admissionPercentageMetaPojoList) {
            meta.loadData(dataDb, latestLessonFirst);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subject subject = (Subject) o;

        if (id != subject.id) return false;
        if (name != null ? !name.equals(subject.name) : subject.name != null) return false;
        if (admissionCounterList != null ? !admissionCounterList.equals(subject.admissionCounterList) : subject.admissionCounterList != null)
            return false;
        return !(admissionPercentageMetaPojoList != null ? !admissionPercentageMetaPojoList.equals(subject.admissionPercentageMetaPojoList) : subject.admissionPercentageMetaPojoList != null);
    }
}
