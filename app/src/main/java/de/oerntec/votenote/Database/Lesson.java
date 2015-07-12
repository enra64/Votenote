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