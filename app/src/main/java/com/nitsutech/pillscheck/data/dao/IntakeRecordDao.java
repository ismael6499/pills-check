package com.nitsutech.pillscheck.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nitsutech.pillscheck.data.entity.IntakeRecordEntity;

import java.util.List;

@Dao
public interface IntakeRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(IntakeRecordEntity record);

    @Delete
    void delete(IntakeRecordEntity record);

    @Query("SELECT * FROM intake_records WHERE dateString = :dateString ORDER BY timestamp DESC LIMIT 1")
    IntakeRecordEntity getRecordForDate(String dateString);

    @Query("SELECT * FROM intake_records ORDER BY timestamp DESC")
    List<IntakeRecordEntity> getAllRecords();

    @Query("SELECT * FROM intake_records WHERE dateString < :dateString ORDER BY timestamp DESC LIMIT 1")
    IntakeRecordEntity getLatestRecordBeforeDate(String dateString);
}
