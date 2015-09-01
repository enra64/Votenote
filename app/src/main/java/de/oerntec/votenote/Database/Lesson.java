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

public class Lesson {
    /**
     * instance number of the lesson
     */
    public int lessonId;
    public int myVotes;
    public int maxVotes;
    /**
     * database id
     */
    public int id;
    public int subjectId;

    public Lesson(int lessonId, int myVotes, int maxVotes, int id, int subjectId) {
        this.lessonId = lessonId;
        this.myVotes = myVotes;
        this.maxVotes = maxVotes;
        this.id = id;
        this.subjectId = subjectId;
    }
}