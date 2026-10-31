package com.agustin.pillscheck.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "intake_records")
public class IntakeRecordEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long timestamp;
    private String dateString; // YYYY-MM-DD
    private String timeString; // HH:mm
    private int folcresRemaining;
    private int minoxidilRemaining;
    private int vitaminDRemaining;
    private String note;

    public IntakeRecordEntity() {
    }

    @Ignore
    public IntakeRecordEntity(long timestamp, String dateString, String timeString,
                              int folcresRemaining, int minoxidilRemaining,
                              int vitaminDRemaining, String note) {
        this.timestamp = timestamp;
        this.dateString = dateString;
        this.timeString = timeString;
        this.folcresRemaining = folcresRemaining;
        this.minoxidilRemaining = minoxidilRemaining;
        this.vitaminDRemaining = vitaminDRemaining;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public int getFolcresRemaining() {
        return folcresRemaining;
    }

    public void setFolcresRemaining(int folcresRemaining) {
        this.folcresRemaining = folcresRemaining;
    }

    public int getMinoxidilRemaining() {
        return minoxidilRemaining;
    }

    public void setMinoxidilRemaining(int minoxidilRemaining) {
        this.minoxidilRemaining = minoxidilRemaining;
    }

    public int getVitaminDRemaining() {
        return vitaminDRemaining;
    }

    public void setVitaminDRemaining(int vitaminDRemaining) {
        this.vitaminDRemaining = vitaminDRemaining;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
