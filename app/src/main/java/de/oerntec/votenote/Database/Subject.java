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
package de.oerntec.votenote.Database;

public class Subject {
    public String id,
            subjectName,
            subjectMinimumVotePercentage,
            subjectCurrentPresentationPoints,
            subjectScheduledLessonCount,
            subjectScheduledAssignmentsPerLesson,
            subjectWantedPresentationPoints;

    public Subject(String id, String subjectName, String subjectMinimumVotePercentage, String subjectCurrentPresentationPoints,
                   String subjectScheduledLessonCount, String subjectScheduledAssignmentsPerLesson, String subjectWantedPresentationPoints) {
        this.id = id;
        this.subjectName = subjectName;
        this.subjectMinimumVotePercentage = subjectMinimumVotePercentage;
        this.subjectCurrentPresentationPoints = subjectCurrentPresentationPoints;
        this.subjectScheduledLessonCount = subjectScheduledLessonCount;
        this.subjectScheduledAssignmentsPerLesson = subjectScheduledAssignmentsPerLesson;
        this.subjectWantedPresentationPoints = subjectWantedPresentationPoints;
    }
}
