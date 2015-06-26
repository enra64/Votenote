package de.oerntec.votenote.Database;

public class Lesson {
    public int lessonId, myVotes, maxVotes, id, subjectId;

    public Lesson(int lessonId, int myVotes, int maxVotes, int id, int subjectId) {
        this.lessonId = lessonId;
        this.myVotes = myVotes;
        this.maxVotes = maxVotes;
        this.id = id;
        this.subjectId = subjectId;
    }
}